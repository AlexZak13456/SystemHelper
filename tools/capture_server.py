#!/usr/bin/env python3
"""
capture_server.py —— SystemHelper 上报接口的高保真模拟服务端（仅监听本机）

相比最小可用版本，本版增加了接近真实运营商 DM 接口的行为：

  1. WAF / UA 校验          非 Dalvik/okhttp UA → 403
  2. Content-Type 校验      非 application/encrypted-json → 415
  3. 参数完整性校验          缺 ver/model/manuf/sign → 400
  4. sign 结构校验          base64 解码后必须 128 字节（1024-bit RSA）→ 400
  5. 请求体校验             空 / 非 16 字节对齐 → 400
  6. 业务一致性校验          IMEI Luhn、号码与运营商冲突 → 409
  7. 去重                   相同 sign（RSA PKCS#1 v1.5 确定性）→ 416
  8. 频率限制               同 IP 10 秒内超过 5 次 → 429
  9. 结构化响应体           模拟真实接口的 JSON，虽客户端不解析

用法：
    python3 -u capture_server.py 8080
    python3 -u capture_server.py 8080 --lenient     # 放宽 UA 与业务校验
    python3 -u capture_server.py 8080 --no-limit    # 关闭频率限制

停止：Ctrl+C
"""

import base64
import glob
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.parse
import uuid
from collections import defaultdict, deque
from http.server import BaseHTTPRequestHandler, HTTPServer

# ----------------------------------------------------------------------------
# 可调参数
# ----------------------------------------------------------------------------
CFG = {
    "port": 8080,
    "strict_ua": True,        # 校验 User-Agent
    "strict_business": True,  # 校验 IMEI / 号码一致性
    "rate_limit": True,       # 频率限制
    "rate_window": 10.0,      # 秒
    "rate_max": 5,            # 窗口内最大请求数
    "expected_sign_len": 128, # 1024-bit RSA 密文长度
}

SEEN_SIGNS = {}                      # 已见过的 sign，用于模拟 416
HITS = defaultdict(deque)            # 频率限制计数
STATS = {"total": 0, "accepted": 0, "duplicate": 0, "rejected": 0}

# 候选明文库：{md5_hex: (文件名, 明文字节)}
# 真实服务端用私钥解 sign 得到 MD5；本模拟器没有私钥，改用"已知明文反推 MD5"
# —— 这恰好复现了该设计最致命的弱点：密钥可由明文自行推导。
CANDIDATES = {}


def load_candidates():
    """加载同目录下所有 .json 作为候选明文"""
    base = os.path.dirname(os.path.abspath(__file__))
    for path in glob.glob(os.path.join(base, "*.json")):
        try:
            data = open(path, "rb").read()
        except Exception:
            continue
        CANDIDATES[hashlib.md5(data).hexdigest()] = (os.path.basename(path), data)
    return CANDIDATES


def aes_ecb_decrypt(cipher: bytes, key_hex: str):
    """用 openssl 做 AES-128-ECB 解密；失败返回 None"""
    try:
        p = subprocess.run(
            ["openssl", "enc", "-d", "-aes-128-ecb", "-K", key_hex, "-nosalt"],
            input=cipher, capture_output=True, timeout=5,
        )
        if p.returncode == 0 and p.stdout:
            return p.stdout
    except Exception:
        pass
    return None


def resolve_plaintext(cipher: bytes):
    """
    尝试用每个候选明文的 MD5 去解密请求体。
    命中则返回 (文件名, 明文, key_hex)。
    """
    for key_hex, (name, plain) in CANDIDATES.items():
        if aes_ecb_decrypt(cipher, key_hex) == plain:
            return name, plain, key_hex
    return None, None, None


def luhn_ok(digits: str) -> bool:
    """IMEI 校验位（Luhn / mod 10）"""
    if not digits.isdigit():
        return False
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


def unicom_msisdn(number: str) -> bool:
    """粗略判断是否为联通号段"""
    if not number or len(number) != 11 or not number.isdigit():
        return False
    return number[:3] in {
        "130", "131", "132", "145", "146", "155", "156", "166", "167",
        "175", "176", "185", "186", "196",
    }


def unicom_mccmnc(mccmnc: str) -> bool:
    return (mccmnc or "").strip() in {"46001", "46006", "46009"}


def validate_plaintext(plain: bytes):
    """对解密出的明文做业务校验，返回问题列表"""
    problems = []
    try:
        obj = json.loads(plain.decode("utf-8"))
    except Exception as e:
        return [f"明文不是合法 JSON：{e}"]

    for slot in ("1", "2"):
        imei = obj.get(f"IMEI{slot}")
        if imei and not luhn_ok(imei):
            problems.append(f"IMEI{slot}={imei} 未通过 Luhn 校验位验证")

        msisdn = obj.get(f"MSISDN{slot}")
        mccmnc = obj.get(f"MNC{slot}")
        if msisdn and mccmnc and unicom_mccmnc(mccmnc) and not unicom_msisdn(msisdn):
            problems.append(f"MSISDN{slot}={msisdn} 属非联通号段，与 MNC{slot}={mccmnc} 冲突")

    spec = obj.get("SpecVersion")
    if spec != "3.0":
        problems.append(f"SpecVersion={spec} 不受支持")

    for key in ("Manuf", "Model", "IMEI1"):
        if not obj.get(key):
            problems.append(f"必填字段 {key} 为空")

    return problems


class Reply:
    """统一的响应封装"""

    def __init__(self, status: int, code: str, message: str, extra: dict | None = None):
        self.status = status
        payload = {"code": code, "message": message}
        if extra:
            payload.update(extra)
        self.body = json.dumps(payload, ensure_ascii=False).encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "DM-Register/3.0"
    sys_version = ""

    def _send(self, reply: Reply):
        self.send_response(reply.status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(reply.body)))
        self.send_header("X-Request-Id", uuid.uuid4().hex[:16])
        self.end_headers()
        self.wfile.write(reply.body)

    def _log(self, tag, msg):
        print(f"  [{tag}] {msg}")

    def log_message(self, fmt, *args):
        """覆盖父类的访问日志（已在上方自行打印详细流程）"""
        pass

    # ---------------- GET（/public/ip）----------------
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/public/ip":
            ip = self.client_address[0].encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.send_header("Content-Length", str(len(ip)))
            self.end_headers()
            self.wfile.write(ip)
            print(f"GET /public/ip  →  {self.client_address[0]}")
            return
        self._send(Reply(404, "9004", "not found"))

    # ---------------- POST（/registerv3）----------------
    def do_POST(self):
        STATS["total"] += 1
        parsed = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsed.query)
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)

        print("=" * 78)
        print(f"POST {parsed.path}   from {self.client_address[0]}")
        print("-" * 78)

        # ---- 1. 频率限制 ----
        if CFG["rate_limit"]:
            q = HITS[self.client_address[0]]
            now = time.time()
            while q and now - q[0] > CFG["rate_window"]:
                q.popleft()
            if len(q) >= CFG["rate_max"]:
                STATS["rejected"] += 1
                self._log("429", f"频率超限：{CFG['rate_window']}s 内已 {len(q)} 次")
                return self._send(Reply(429, "9003", "too many requests"))
            q.append(now)

        # ---- 2. WAF / UA ----
        ua = self.headers.get("User-Agent", "")
        self._log("UA", ua or "(空)")
        if CFG["strict_ua"] and not ("Dalvik" in ua or ua.lower().startswith("okhttp")):
            STATS["rejected"] += 1
            self._log("403", "UA 不在白名单（期望 Dalvik/... 或 okhttp/...）")
            return self._send(Reply(403, "9001", "forbidden user-agent"))

        # ---- 3. Content-Type ----
        ctype = self.headers.get("Content-Type", "")
        self._log("CT", ctype)
        if "application/encrypted-json" not in ctype:
            STATS["rejected"] += 1
            self._log("415", "Content-Type 不正确")
            return self._send(Reply(415, "9002", "unsupported media type"))

        # ---- 4. 参数完整性 ----
        for key in ("ver", "model", "manuf", "sign"):
            if key not in params:
                STATS["rejected"] += 1
                self._log("400", f"缺少参数 {key}")
                return self._send(Reply(400, "2001", f"missing parameter: {key}"))

        ver = params["ver"][0]
        model = params["model"][0]
        manuf = params["manuf"][0]
        sign_raw = urllib.parse.unquote(params["sign"][0])
        self._log("PARAM", f"ver={ver}  model={model}  manuf={manuf}")

        # ---- 5. sign 结构 ----
        try:
            sign_bytes = base64.b64decode(sign_raw, validate=True)
        except Exception as e:
            STATS["rejected"] += 1
            self._log("400", f"sign base64 解码失败：{e}")
            return self._send(Reply(400, "2002", "invalid sign encoding"))

        self._log("SIGN", f"base64 解码后 {len(sign_bytes)} 字节")
        if len(sign_bytes) != CFG["expected_sign_len"]:
            STATS["rejected"] += 1
            self._log("400", f"sign 长度应为 {CFG['expected_sign_len']} 字节")
            return self._send(Reply(400, "2002", "invalid sign length"))

        # ---- 6. 请求体 ----
        self._log("BODY", f"{len(body)} 字节，16 字节对齐={len(body) % 16 == 0}")
        if len(body) == 0:
            STATS["rejected"] += 1
            self._log("400", "请求体为空")
            return self._send(Reply(400, "2003", "empty body"))
        if len(body) % 16 != 0:
            STATS["rejected"] += 1
            self._log("400", "请求体不是 16 字节整数倍")
            return self._send(Reply(400, "2003", "invalid body length"))
        self._log("BODY", f"hex[0:32] = {body[:32].hex()}")

        # ---- 7. 业务校验（模拟）----
        rejected = self._business_check(body)
        if rejected is not None:
            STATS["rejected"] += 1
            return self._send(rejected)

        # ---- 8. 去重（416）----
        if sign_raw in SEEN_SIGNS:
            STATS["duplicate"] += 1
            self._log("416", f"相同 sign 已存在（首次于 {SEEN_SIGNS[sign_raw]}）→ 客户端视为成功")
            return self._send(Reply(416, "1001", "record already exists"))
        SEEN_SIGNS[sign_raw] = time.strftime("%H:%M:%S")

        # ---- 9. 成功 ----
        STATS["accepted"] += 1
        reg_id = uuid.uuid4().hex[:12]
        self._log("200", f"接受，regId={reg_id}")
        print("-" * 78)
        print(f"累计：{STATS}")
        return self._send(Reply(200, "0000", "success", {"data": {"regId": reg_id}}))

    def _business_check(self, body):
        """
        真实服务端：私钥解 sign → 得 MD5 → 用该 MD5 解 body → 得明文 → 校验字段。

        本模拟器没有私钥，改用"候选明文反推"：对 tools/*.json 逐个算 MD5，
        用它去解 body，能解开就说明命中了。
        这恰好复现了该系统最致命的弱点 —— 密钥可由明文自行推导。
        """
        name, plain, key_hex = resolve_plaintext(body)
        if plain is None:
            self._log("WARN", "无法解密（明文不在候选库中），跳过业务校验")
            return None

        self._log("DECRYPT", f"解密成功 → 命中 {name}")
        self._log("DECRYPT", f"AES key = MD5(明文) = {key_hex}")
        self._log("DECRYPT", "  ↑ 这个 key 不需要任何秘密即可算出，签名的也是同一个值")

        if not CFG["strict_business"]:
            return None

        problems = validate_plaintext(plain)
        if problems:
            self._log("409", "业务校验失败：")
            for p in problems:
                self._log("409", f"  · {p}")
            return Reply(409, "2004", "business validation failed: " + problems[0])

        self._log("OK", "业务校验通过")
        return None


def main():
    args = sys.argv[1:]
    if args and args[0].isdigit():
        CFG["port"] = int(args[0])
        args = args[1:]

    if "--lenient" in args:
        CFG["strict_ua"] = False
        CFG["strict_business"] = False
        print("*** 宽松模式：跳过 UA 与业务校验 ***")

    if "--no-limit" in args:
        CFG["rate_limit"] = False
        print("*** 已关闭频率限制 ***")

    cands = load_candidates()
    print(f"*** 已加载 {len(cands)} 个候选明文：{', '.join(v[0] for v in cands.values())} ***")

    print("=" * 78)
    print("SystemHelper 上报接口模拟服务端")
    print("=" * 78)
    print(f"  监听      : http://127.0.0.1:{CFG['port']}/registerv3")
    print(f"  UA 校验   : {'开' if CFG['strict_ua'] else '关'}")
    print(f"  业务校验  : {'开' if CFG['strict_business'] else '关'}")
    if CFG["rate_limit"]:
        print(f"  频率限制  : 每 {CFG['rate_window']}s 最多 {CFG['rate_max']} 次")
    else:
        print("  频率限制  : 关")
    print("  验签      : 跳过（本机无私钥，无法解 sign）")
    print("-" * 78)
    print("返回码：200 成功 / 416 重复(客户端视为成功) / 400 参数错")
    print("        403 UA 拒绝 / 409 业务冲突 / 415 类型错 / 429 频率超限")
    print("=" * 78)
    print("Ctrl+C 停止\n")

    try:
        HTTPServer(("127.0.0.1", CFG["port"]), Handler).serve_forever()
    except KeyboardInterrupt:
        print(f"\n\n已停止。累计：{STATS}")


if __name__ == "__main__":
    main()
