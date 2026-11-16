# 上报请求构造与本地验证

## 为什么不在真实服务器上测

`dm.wo.com.cn:18080` 是中国联通的生产环境。向未经授权的第三方生产系统发送构造请求，无论内容真假，都属于未授权访问测试 —— 可能触及《网络安全法》第 27 条与《刑法》第 285 条。而且实际上也验证不了什么：`sign` 需要服务端私钥才能校验，随便发的请求只会被拒。

**正确做法是本地闭环**：用样本自带的 `assets/pub.cer` 生成格式完全合法的请求，打到本机的 HTTP 服务。这样既验证了客户端协议，又完全在自己的控制范围内。

---

## 文件说明

| 文件 | 作用 |
|---|---|
| `forge_request.sh` | 按 `control/g.java` 的算法把明文加工成 `sign` + `body`，输出带真实 UA 的 curl |
| `capture_server.py` | 高保真模拟服务端：UA 校验、字段校验、解密、去重、限流 |
| `sample.json` | **非法**样本（IMEI 不过 Luhn、号码与运营商冲突），用于测拒绝路径 |
| `sample_valid.json` | **合法**样本（IMEI 过 Luhn、联通号段一致），用于测通过路径 |
| `curl_cmd.sh` | 脚本自动生成，已带 Dalvik UA 与完整请求头 |
| `body.bin` / `plain.json` | 上一次生成的密文与明文 |

---

## 模拟服务端做了什么

真实运营商 DM 接口不会随便接受请求。本版按常见实现补齐了九道校验：

| # | 校验 | 失败返回 |
|---|---|---|
| 1 | 频率限制：同 IP 10 秒内最多 5 次 | `429` |
| 2 | UA 白名单：必须含 `Dalvik` 或以 `okhttp` 开头 | `403` |
| 3 | `Content-Type` 必须 `application/encrypted-json` | `415` |
| 4 | 必须带 `ver` / `model` / `manuf` / `sign` | `400` |
| 5 | `sign` base64 解码后必须 128 字节（1024-bit RSA） | `400` |
| 6 | 请求体非空且 16 字节对齐 | `400` |
| 7 | **解密请求体**（见下） | — |
| 8 | 业务校验：IMEI Luhn、号码与 MNC 一致性、版本号 | `409` |
| 9 | 去重：相同 `sign` 重复提交 | `416` |

### 关于解密

真实服务端用私钥解 `sign` 得到 MD5，再用该 MD5 解 `body`。本模拟器没有私钥，改用另一条路径达到同样效果：

> 加载 `tools/*.json` 作为候选明文，逐个计算 `MD5(明文)`，用它去解 `body`，能解开就说明命中。

这不是取巧，而是**恰好复现了这套设计的致命弱点** —— 密钥不需要任何秘密，从明文自己就能算出来。所以模拟器反而能完整校验业务字段。

### 返回码约定

```
200  成功（返回 regId）
416  重复提交 —— 客户端把它也算作成功（control/g.java 里显式判断）
400  参数或请求体格式错误
403  UA 不在白名单
409  业务校验失败（IMEI/号码冲突）
415  Content-Type 错误
429  频率超限
```

---

## 运行

### 终端 1：启动服务端

```powershell
cd D:\workspace\SystemHelper\tools
python -u capture_server.py 8080
```

启动后会打印已加载的候选明文与各项开关状态。

可选参数：

| 参数 | 作用 |
|---|---|
| `--lenient` | 关闭 UA 与业务校验，只记录不收拒 |
| `--no-limit` | 关闭频率限制，方便连续测试 |

### 终端 2：构造并发送

```powershell
cd D:\workspace\SystemHelper\tools

# 合法样本 → 期望 200
bash forge_request.sh sample_valid.json http://127.0.0.1:8080/registerv3
bash ./curl_cmd.sh

# 非法样本 → 期望 409
bash forge_request.sh sample.json http://127.0.0.1:8080/registerv3
bash ./curl_cmd.sh
```

生成的 `curl_cmd.sh` 已包含真实客户端会发的头：

```bash
curl -i -sS -X POST \
  -H 'User-Agent: Dalvik/2.1.0 (Linux; U; Android 13; V1911A Build/TP1A.220905.001)' \
  -H 'Content-Type: application/encrypted-json' \
  -H 'Content-Length: 1136' \
  -H 'Accept-Encoding: gzip' \
  -H 'Connection: Keep-Alive' \
  -H 'Accept:' \
  --data-binary @body.bin \
  'http://127.0.0.1:8080/registerv3?ver=3.0&model=V1911A&manuf=vivo&sign=...'
```

> `-H 'Accept:'` 用于删掉 curl 默认的 `Accept: */*` —— Android 的 `HttpURLConnection` 不发这个头。

---

## 四条路径的实测结果

| 场景 | 命令 | 实际返回 |
|---|---|---|
| 非法样本 | `sample.json` | `409` `{"code":"2004","message":"business validation failed: IMEI1=111111111111111 未通过 Luhn 校验位验证"}` |
| 合法样本 | `sample_valid.json` | `200` `{"code":"0000","message":"success","data":{"regId":"52bc5f9032ee"}}` |
| 重复提交 | 再发一次同 sign | `416` `{"code":"1001","message":"record already exists"}` |
| 裸 curl | 去掉 UA 头 | `403` `{"code":"9001","message":"forbidden user-agent"}` |

服务端日志（合法样本那一次）：

```
POST /registerv3   from 127.0.0.1
  [UA] Dalvik/2.1.0 (Linux; U; Android 13; V1911A Build/TP1A.220905.001)
  [CT] application/encrypted-json
  [PARAM] ver=3.0  model=V1911A  manuf=vivo
  [SIGN] base64 解码后 128 字节
  [BODY] 1136 字节，16 字节对齐=True
  [DECRYPT] 解密成功 → 命中 sample_valid.json
  [DECRYPT] AES key = MD5(明文) = fe22b98e8b5ae2e85ec801bd8bd09345
  [DECRYPT]   ↑ 这个 key 不需要任何秘密即可算出，签名的也是同一个值
  [OK] 业务校验通过
  [200] 接受，regId=52bc5f9032ee
```

---

## 内部算法（对应 `control/g.java` 第 36–42 行）

| 步骤 | 客户端代码 | 脚本实现 |
|---|---|---|
| 1 | `cVar.a()` | 读取样本 JSON |
| 2 | `b.c.a.b.c.a(strA)` → MD5 | `openssl dgst -md5 -binary` |
| 3 | `c.a(c.a(ctx, md5))` → RSA + Base64 | `openssl pkeyutl -encrypt -pkeyopt rsa_padding_mode:pkcs1` |
| 4 | `URLEncoder.encode(sign)` | `urllib.parse.quote` |
| 5 | `l.c(ctx) + "?ver=3.0&..."` | 拼接 |
| 6 | `c.a(strA, bArrA)` → AES/ECB | `openssl enc -aes-128-ecb -K $MD5 -nosalt` |
| 7 | `new g().a(url, body, md5)` | `curl --data-binary @body.bin` |

---

## 顺带验证出的问题

**① 密钥可自算** —— 用 `MD5(明文)` 直接解密 body，结果与原文件逐字节一致。加密层提供的是"防结构窥探"，不是机密性。

**② 公钥强度不足** —— `pub.cer` 是 **1024 位 RSA**（密文 128 字节而非 256 字节），有效期 **2019-05-24 → 2119-04-30，跨度 100 年**。NIST SP 800-57 自 2013 年起已不推荐 1024 位用于新系统。

**③ 接收方身份确证** —— 证书主体 `O=UNICOM, OU=UNICOM, CN=UNICOM`，是中国联通自己签发的加密公钥。

**④ ECB 块重复：本样本为 0** —— 71 个密文块全部唯一，因为示例 JSON 字段值各不相同。ECB 的理论缺陷（无 IV、确定性、块可拼接）依然存在，但不应夸大为"肉眼可见的重复块"。

**⑤ RSA 是确定性的** —— PKCS#1 v1.5 对相同输入产生相同输出，所以同样的明文会得到同样的 `sign`。模拟服务端正是利用这一点实现"重复提交 → 416"。

---

## 用真机验证（可选）

若要在真实设备上观察客户端自身发出的请求，可利用报告中的 C-01：

```java
bindService(... RegisterHelperService)
binder.transact(1, "http://192.168.1.100:8080/registerv3")
```

然后等待开机 +4 分钟、或 24 小时定时任务触发。注意：

- 需手机与电脑同一局域网，且把 `capture_server.py` 最后一行改为监听 `0.0.0.0`；
- 真机上报的是**真实设备信息**，测完立即删除捕获数据；
- 仅在自己拥有或已获书面授权的设备上操作。

---

## 清理

```bash
rm -f body.bin plain.json curl_cmd.sh
```

`sample.json` / `sample_valid.json` 建议保留（纯虚构数据，可作回归用例）。

---

*所有测试均在本机完成，未向任何外部服务器发送请求。*
