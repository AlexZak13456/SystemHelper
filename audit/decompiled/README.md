# 反编译核心代码 · 说明

本目录包含 SystemHelper 核心代码的两份 Java 导出，**同一批逻辑、两种命名**，用于对照阅读与独立复核。

```
decompiled/
├── raw/            未反混淆 —— JADX 原始输出，类名/字段名为混淆后的真实名称
└── deobfuscated/   反混淆   —— 人工语义化重命名，逻辑等价
```

---

## 1. 两份的关系

| | raw/ | deobfuscated/ |
|---|---|---|
| 命名 | 混淆原名（`b.c.a.b.c`、`f599a`） | 语义名（`CryptoHelper`、`logEnabled`） |
| 保真度 | **100%**，逐字符对应 JADX 输出 | 逻辑等价，命名重写，可能合并/精简注释 |
| 用途 | 作为**证据**，任何结论可回溯到这里 | 作为**读物**，30 分钟内理解全链路 |
| 是否可直接编译 | 否（缺依赖类与资源） | 否（同为阅读用片段） |
| 生成方式 | JADX 6.x（MCP）导出 | 人工逐个重命名，**非工具自动还原** |

> **重要**：样本没有附带 ProGuard/R8 的 `mapping.txt`，因此不存在"自动反混淆"的可能性。
> `deobfuscated/` 下的名字是**人工根据语义推断**的，不是原始源码名。
> 若需绝对保真，一律以 `raw/` 为准。

---

## 2. 逐文件对照表

| # | raw（混淆类） | 语义 | deobfuscated/ | 行数 | 核心程度 |
|---|---|---|---|---|---|
| 1 | `b/c/a/b/f.java` | 全局常量与状态 | `AppConfig.java` | 40 | ★★★★★ |
| 2 | `b/c/a/b/c.java` | 加密原语（MD5/AES） | `CryptoHelper.java` | 72 | ★★★★★ |
| 3 | `b/c/a/b/k.java` | RSA 封装 | （并入 CryptoHelper） | 30 | ★★★★ |
| 4 | `b/c/a/b/g.java` | HTTP POST 上报 | `HttpUploader.java` | 60 | ★★★★★ |
| 5 | `b/c/a/b/d.java` | SQLite 待上报队列 | `PendingQueueDb.java` | 120 | ★★★★ |
| 6 | `b/c/a/b/l.java` | SharedPreferences | `PrefStore.java` | 120 | ★★★★ |
| 7 | `b/c/a/b/j.java` | 日志封装 | `LogHelper.java` | 40 | ★★★ |
| 8 | `b/c/a/b/a.java` | Alarm 调度器 | —（见 raw） | 90 | ★★★ |
| 9 | `b/c/a/b/b.java` | AppInfo 单项模型 | —（见 raw） | 80 | ★★★ |
| 10 | `control/g.java` | **上报线程** | `UploadThread.java` | 132 | ★★★★★ |
| 11 | `control/c.java` | **数据模型 39 字段** | `DeviceInfo.java` | 600 | ★★★★★ |
| 12 | `control/e.java` | 应用画像采集 | —（见 raw） | 400 | ★★★★ |
| 13 | `control/OnBootReceiver.java` | 开机触发 | —（见 raw） | 90 | ★★★★ |
| 14 | `control/ScheduleRegisterReceiver.java` | 24h 周期 | —（见 raw） | 45 | ★★★ |
| 15 | `control/AppInfoRegisterReceiver.java` | 7d 周期 | —（见 raw） | 45 | ★★★ |
| 16 | `control/DMApplication.java` | 应用入口 | —（见 raw） | 18 | ★★ |
| 17 | `service/RegisterHelperService.java` | **导出 AIDL 服务** | `RegisterHelperService.java` | 80 | ★★★★★ |
| 18 | `service/RegisterIntentService.java` | 厂商 AIDL 绑定 | —（见 raw） | 95 | ★★★ |
| 19 | `b/c/a/a/c.java` | 设备信息读取基类 | —（见 raw） | 600 | ★★★★★ |
| 20 | `b/c/a/a/f1.java` | 机型路由工厂（节选） | —（见 raw） | 400 | ★★★★ |

raw 共 **20 个文件 / 3,253 行**，deobfuscated 共 **9 个文件 / 1,322 行**
（9 个文件覆盖 10 个 raw 类 —— 其中 `b.c.a.b.k` 已并入 `CryptoHelper`）。

---

## 3. 节选说明（两处，均已在文件内标注）

| 文件 | 节选内容 | 原因 |
|---|---|---|
| `raw/b/c/a/a/f1.java` | 机型映射表保留 40 条（原 200+）；`b(Context)` 分支保留 25 条 | 800 行纯数据表，信息价值低，已在报告中归纳 |
| `raw/.../RegisterIntentService.java` | `onHandleIntent` 以注释转写 smali 语义 | JADX 控制流还原失败，原始输出会抛 `UnsupportedOperationException` |

除上述两处，其余文件均为**完整输出**，未删减任何语句。

---

## 4. 五分钟读懂全链路

按此顺序读 `deobfuscated/`，即可串起整条隐私链路：

```
AppConfig        → URL 是什么、周期多长、开关默认值
   ↓
OnBootReceiver   → 开机后延迟 4 分钟触发（见 raw）
   ↓
DeviceInfoReaderBase → getImei / getSubscriberId / getCellIdentity（见 raw）
DeviceInfo       → 39 个字段如何组装成 JSON
   ↓
PrefStore        → serverAddress 从这里读（可被改写）
   ↓
UploadThread     → MD5 → RSA(sign) → AES(body) → POST
   ↓
CryptoHelper     → 加密为什么形同虚设
HttpUploader     → 第三个参数为什么没用
```

---

## 5. 三个"一眼可验证"的关键点

**① 上报地址可被改写** —— 打开 `UploadThread.java` 第 6 步：

```java
String url = PrefStore.getServerAddress(this.context) + "?ver=3.0&model=..."
```

再打开 `RegisterHelperService.java` 的 `a(String serverAddress)`：

```java
PrefStore.setServerAddress(RegisterHelperService.this.context, serverAddress);
```

两者相接，且该方法所在的 Service 在 Manifest 中是 `exported="true"` 且无 `android:permission`。

**② 加密密钥由明文派生** —— `UploadThread.java` 第 4、7 步：

```java
byte[] jsonMd5 = CryptoHelper.md5(plainJson);            // 密钥
...
byte[] encryptedBody = CryptoHelper.aesEncrypt(plainJson, jsonMd5);   // 用同一个 MD5 加密
```

**③ 会话密钥机制被丢弃** —— `HttpUploader.java`：

```java
public boolean upload(String url, byte[] body, byte[] rsaWrappedSessionKey) {
    ...
    out.write(body);          // rsaWrappedSessionKey 全程未出现
```

---

## 6. 局限性（诚实声明）

1. **不是可编译工程**。两份文件都缺少依赖类（`b.c.a.b.o` 反射工具、`control.a/b` 监听器、AIDL stub 等）、资源与 Manifest，无法直接 `javac`。
2. **deobfuscated/ 不是原始源码**。命名是人工推断，若厂商日后公开 mapping，应以 mapping 为准。
3. **反编译存在固有失真**。JADX 对控制流平坦化的方法会失败（本样本有 2 处），已按 smali 语义转写并标注。
4. **行号不稳定**。raw 文件头部的注释不含行号；引用行号时请以 JADX 现场打开的结果为准（见 `核心代码索引.md`）。

---

*本目录内容仅用于安全审计与合规评估，所有代码均来自本地样本的静态反编译，未对任何服务器发起请求。*
