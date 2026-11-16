#!/usr/bin/env bash
#
# forge_request.sh —— 按 SystemHelper 客户端逻辑，构造一个格式完全合法的上报请求
#
# 原理（与 control/g.java 第 36-42 行逐一对应）：
#   md5   = MD5(plainJson)                       → 16 字节
#   sign  = urlencode(base64(RSA_PKCS1(md5)))    → URL 参数
#   body  = AES-128-ECB(plainJson, key = md5)    → 请求体
#
# 用法：
#   ./forge_request.sh sample.json http://127.0.0.1:8080/registerv3
#   ./forge_request.sh sample.json http://127.0.0.1:8080/registerv3 --dry-run
#
# 依赖：openssl、python3（仅用于 urlencode）
#
set -euo pipefail

JSON_FILE="${1:?用法: forge_request.sh <json_file> <target_url> [--dry-run]}"
TARGET_URL="${2:?缺少 target_url}"
DRY_RUN="${3:-}"

APK_DIR="$(cd "$(dirname "$0")/../audit/apk_raw" && pwd)"
CERT="$APK_DIR/assets/pub.cer"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK" 2>/dev/null || true' EXIT

# ---------- 0. 从 DER 证书提取 RSA 公钥 ----------
openssl x509 -in "$CERT" -inform DER -pubkey -noout > "$WORK/pub.pem"

# ---------- 1. 明文 JSON ----------
cp "$JSON_FILE" "$WORK/plain.json"

# ---------- 2. md5 = MD5(plainJson) ----------
# 二进制 16 字节（用于 RSA 加密）
openssl dgst -md5 -binary "$WORK/plain.json" > "$WORK/md5.bin"
# 十六进制 32 字符（正好是 16 字节的 AES-128 密钥）
MD5_HEX="$(openssl dgst -md5 "$WORK/plain.json" | awk '{print $NF}')"

# ---------- 3. sign = base64(RSA_PKCS1(md5)) ----------
openssl pkeyutl -encrypt -pubin -inkey "$WORK/pub.pem" \
        -pkeyopt rsa_padding_mode:pkcs1 \
        -in "$WORK/md5.bin" -out "$WORK/sign.bin"

SIGN_B64="$(openssl base64 -A -in "$WORK/sign.bin")"
SIGN_ENCODED="$(python3 - "$SIGN_B64" <<'PY'
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1], safe=''))
PY
)"

# ---------- 4. body = AES-128-ECB(plainJson, key=md5) ----------
openssl enc -aes-128-ecb -K "$MD5_HEX" -nosalt \
        -in "$WORK/plain.json" -out "$WORK/body.bin"

# ---------- 5. 组装 URL ----------
MODEL="$(python3 -c "import json,sys;d=json.load(open('$JSON_FILE'));print(d.get('Model','Unknown'))")"
MANUF="$(python3 -c "import json,sys;d=json.load(open('$JSON_FILE'));print(d.get('Manuf','Unknown'))")"
MODEL_ENC="$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=''))" "$MODEL")"
MANUF_ENC="$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=''))" "$MANUF")"

FULL_URL="${TARGET_URL}?ver=3.0&model=${MODEL_ENC}&manuf=${MANUF_ENC}&sign=${SIGN_ENCODED}"

# ---------- 6. 组装真实客户端请求头 ----------
# Android HttpURLConnection 默认会带以下头，这里逐一复现：
#   User-Agent   Dalvik/2.1.0 (Linux; U; Android 13; <MODEL> Build/...)
#   Accept-Encoding: gzip        ← Android 默认支持 gzip
#   Connection   Keep-Alive      ← HTTP/1.1 默认
#   Accept       （不发，故用 -H 'Accept:' 移除 curl 自带的 */*）
UA="Dalvik/2.1.0 (Linux; U; Android 13; ${MODEL} Build/TP1A.220905.001)"

# ---------- 7. 输出 ----------
echo "================ 构造结果 ================"
echo "明文 JSON        : $(wc -c < "$WORK/plain.json") 字节"
echo "MD5(hex)         : $MD5_HEX"
echo "sign (base64)    : ${SIGN_B64:0:60}... ($(wc -c < "$WORK/sign.bin") 字节 RSA 密文)"
echo "body (AES-ECB)   : $(wc -c < "$WORK/body.bin") 字节"
echo "User-Agent       : $UA"
echo
echo "请求体已保存到   : body.bin（见下方 curl 的 --data-binary）"
cp "$WORK/body.bin" ./body.bin
cp "$WORK/plain.json" ./plain.json
echo

if [ "$DRY_RUN" = "--dry-run" ]; then
    echo "================ URL ================"
    echo "$FULL_URL"
    echo
    echo "(dry-run：未发送)"
    exit 0
fi

cat > ./curl_cmd.sh <<EOF
curl -i -sS -X POST \\
  -H 'User-Agent: ${UA}' \\
  -H 'Content-Type: application/encrypted-json' \\
  -H 'Content-Length: $(wc -c < "$WORK/body.bin")' \\
  -H 'Accept-Encoding: gzip' \\
  -H 'Connection: Keep-Alive' \\
  -H 'Accept:' \\
  --data-binary @body.bin \\
  '${FULL_URL}'
EOF
chmod +x ./curl_cmd.sh

echo "================ 可直接执行的 curl ================"
cat ./curl_cmd.sh
echo
echo "已写入 ./curl_cmd.sh，请求体在 ./body.bin"
