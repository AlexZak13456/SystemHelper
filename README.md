# SystemHelper 1.2-20221110 — Reverse Engineering & Security Audit

**[中文](#中文) · [English](#english) · [Русский](#русский)**

> 🤖 **AI-ANALYZED / 由 AI 分析 / ПРОАНАЛИЗИРОВАНО ИИ**
>
> 中文：本仓库中的报告由 **DeepSeek V4.1 Flash** 配合 **CodeX、JADX** 完成分析与撰写，并经过部分人工复核，未经独立人工安全机构全面审核。
>
> English: The report in this repository was **analyzed and written using DeepSeek V4.1 Flash, CodeX, and JADX**, with partial human review. It has not undergone a full independent security audit by a professional security firm.
>
> Русский: Отчёт в этом репозитории **проанализирован и составлен с использованием DeepSeek V4.1 Flash, CodeX и JADX** и прошёл частичную проверку человеком, но не прошёл полноценный независимый аудит профессиональной организацией по безопасности.

| | |
|---|---|
| Target / 对象 / Объект | `SystemHelper_1.2-20221110.apk` (`com.mobiletools.systemhelper`) |
| Method / 方式 / Метод | Static analysis only / 纯静态逆向 / Только статический анализ |
| Audit period / 审计日期 / Период | 2026-11-14 – 2026-11-16 |
| Analysis performed by / 分析者 / Анализ выполнил | AI — DeepSeek V4.1 Flash + CodeX + JADX |
| Human review / 人工复核 / Проверка человеком | 部分人工复核 / Partial human review / Частичная проверка человеком |

---

## 中文

### ⚠️ AI 分析声明

本仓库中的《SystemHelper 逆向工程与安全审计总报告》、全部底稿及反编译整理产物，主要由 **AI（DeepSeek V4.1 Flash）配合 CodeX、JADX 完成分析与撰写**，并经过部分人工复核。

AI 分析可能存在以下问题：

- 误读反编译结果（报告自述有 5 处 JADX 控制流还原失败，已按 smali 语义补全）；
- 将推断写成事实，或遗漏代码路径；
- 法律条文引用、适用性判断不准确。

**请勿将本报告视为经过完整独立人工审核的权威结论。** 引用前请依据报告附录 A 的证据索引自行复核。

人工复核情况：部分人工复核，尚未经过独立第三方安全机构的完整审计。

### 概述

本仓库存放一份针对 `SystemHelper_1.2-20221110.apk` 的逆向工程与安全审计总报告，以及对应的二进制样本。该 APK 是一款中国联通定制终端的设备管理（DM）组件，随平台级签名预装。

报告同时从**隐私**（采集什么、是否合规）与**安全**（自身有哪些可被利用的缺陷）两个层面展开。

### 样本信息

| 项目 | 内容 |
|---|---|
| 文件名 | `SystemHelper_1.2-20221110.apk` |
| 包名 / 版本 | `com.mobiletools.systemhelper` / `1.2-20221110`（versionCode 3） |
| 大小 | 1,762,193 字节 |
| MD5 | `a78da3f746597509ab61054689a65353` |
| SHA-256 | `6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4` |
| 签名证书 | `CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN`（SHA1withRSA / 2048） |
| SDK | minSdk 24 / targetSdk 33 |

请在使用前自行校验哈希。

### 报告主要结论（由 AI 得出）

- **隐私**：单条上报记录含 39 个字段（双卡 IMEI/IMSI/ICCID、手机号、基站信息、全部已安装应用包名及使用时长/流量等），周期性上传至 `dm.wo.com.cn`。报告认为其告知—同意机制未落地。
- **安全**：导出的 `RegisterHelperService` 未设置 `android:permission`，可被任意应用绑定并改写上报地址；链路为明文 HTTP；加密实现存在误用（密钥由明文 MD5 推导、AES-ECB）。
- **18 项发现**：严重 4 / 高 5 / 中 5 / 低 4。
- **外部传言核查**：「会录屏」——不属实；「上传所有 APK 包名」——属实；「上传 APK 文件」——不属实。

### 证据分级

报告对结论标注证据强度：

| 标记 | 含义 |
|---|---|
| `[A]` | 由代码/资源/Manifest 直接证实，可复现 |
| `[B]` | 由代码逻辑强推断 |
| `[C]` | 需服务端或运行时数据确认，**报告无法验证** |
| `[D]` | 无证据，**不构成指控** |

### 方法与局限

- 纯静态分析（JADX、DEX 结构、资源表、证书解析）。
- **未进行动态调试，未对任何目标服务器发起请求**；服务端如何处理数据完全未知。
- 反编译存在固有失真；无法排除同设备其他预装组件的影响。
- 结论仅适用于本版本 APK 及采用它的机型，**不推及**厂商其他产品或运营商其他业务。
- 报告只判断"是否合规 / 是否构成侵害"，**不推断动机**。

### 目录结构

```text
.
├── README.md
├── SystemHelper_Audit-Report.md    # 逆向工程与安全审计总报告（v1.0）
├── Binaries/                       # 二进制样本
│   └── SystemHelper_1.2-20221110.apk
├── audit/                          # 5 份底稿
│   └── decompiled/
│       ├── raw/                    # JADX 原始输出（未反混淆，保真以此为准）
│       └── deobfuscated/           # 人工语义化命名
└── tools/                          # 本地验证工具（仅面向本机模拟服务端）
```

### 使用与安全注意

- 样本为厂商平台级签名的预装组件，其导出服务存在报告所述缺陷。**请勿安装在日常使用的设备上**，建议仅在隔离的虚拟机/模拟器中分析。
- `tools/` 中的脚本仅用于对**本机模拟服务端**做离线验证。**请勿向真实的生产服务器发送任何请求。**
- 本仓库仅供安全研究、隐私研究与教育用途。

### 免责声明

- 报告内容由 AI 生成并经过部分人工复核，**不构成法律意见**，也不构成对任何公司或个人的指控；其中的合规与法律分析仅为技术性评估。
- 样本版权归原权利人所有，仓库仅出于研究目的留存。如权利人提出合理要求，将予以处理。
- 欢迎通过电子邮件 `AlexZak135@proton.me` 联系，提交勘误与复核结果。

### 许可

- **报告文本：** CC0 1.0 Universal（`CC0-1.0`）。
- **工具代码：** CC0 1.0 Universal（`CC0-1.0`）。
- **APK 样本：** 不适用上述许可，版权及相关权利归原权利人所有。

---

## English

### ⚠️ AI-Analysis Notice

The *SystemHelper Reverse Engineering and Security Audit — Master Report*, all of its drafts, and the organized decompilation outputs in this repository were **primarily analyzed and written using DeepSeek V4.1 Flash, CodeX, and JADX**, with partial human review.

AI-generated analysis may contain:

- Misreadings of decompiled output (the report itself notes 5 places where JADX failed to recover control flow, which were completed from smali semantics);
- Inferences presented as facts, or missed code paths;
- Inaccurate citation or application of legal provisions.

**Do not treat this report as a fully independently human-verified authoritative assessment.** Before citing it, re-verify claims against the evidence index in Appendix A of the report.

Human review status: partial human review has been conducted; no full independent third-party security audit has been completed.

### Overview

This repository holds a master reverse-engineering and security-audit report on `SystemHelper_1.2-20221110.apk`, together with the corresponding binary sample. The APK is a device-management (DM) component for China Unicom-customized handsets, pre-installed with a platform-level signature.

The report examines both **privacy** (what is collected and whether it is compliant) and **security** (what exploitable weaknesses the app itself has).

### Sample Information

| Item | Value |
|---|---|
| File name | `SystemHelper_1.2-20221110.apk` |
| Package / version | `com.mobiletools.systemhelper` / `1.2-20221110` (versionCode 3) |
| Size | 1,762,193 bytes |
| MD5 | `a78da3f746597509ab61054689a65353` |
| SHA-256 | `6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4` |
| Signing certificate | `CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN` (SHA1withRSA / 2048) |
| SDK | minSdk 24 / targetSdk 33 |

Please verify the hashes before use.

### Key Findings (as concluded by the AI)

- **Privacy:** Each report record contains 39 fields (dual-SIM IMEI/IMSI/ICCID, phone number, cell-tower data, the package names of all installed apps with usage time and traffic, and more), uploaded periodically to `dm.wo.com.cn`. The report concludes that the notice-and-consent mechanism is not effectively implemented.
- **Security:** The exported `RegisterHelperService` declares no `android:permission`, so any app can bind to it and overwrite the reporting server address; transport is plaintext HTTP; the cryptography is misused (key derived as MD5 of the plaintext, AES-ECB).
- **18 findings:** 4 critical / 5 high / 5 medium / 4 low.
- **Fact-check of public rumors:** "Records the screen" — not supported; "uploads all installed app package names" — supported; "uploads APK files" — not supported.

### Evidence Grading

Each conclusion in the report is tagged with an evidence level:

| Tag | Meaning |
|---|---|
| `[A]` | Directly confirmed by code, resources or Manifest; reproducible |
| `[B]` | Strongly inferred from code logic |
| `[C]` | Requires server-side or runtime data; **the report cannot verify it** |
| `[D]` | No evidence; **not an allegation** |

### Methodology & Limitations

- Static analysis only (JADX, DEX structure, resource table, certificate parsing).
- **No dynamic debugging was performed and no request was ever sent to any target server;** what the server does with the data is entirely unknown.
- Decompilation is inherently lossy; other pre-installed components on the same device cannot be ruled out.
- Conclusions apply only to this APK version and to devices that ship it, and **are not extended** to other products of the vendor or other services of the carrier.
- The report judges "compliant or not / infringing or not" and **does not infer motive**.

### Repository Layout

```text
.
├── README.md
├── SystemHelper_Audit-Report.md    # Master reverse-engineering and security audit report (v1.0)
├── Binaries/                       # Binary sample
│   └── SystemHelper_1.2-20221110.apk
├── audit/                          # 5 source drafts
│   └── decompiled/
│       ├── raw/                    # Raw JADX output (not deobfuscated; authoritative)
│       └── deobfuscated/            # Manually renamed for readability
└── tools/                          # Local verification tools (local mock server only)
```

### Usage & Safety Notes

- The sample is a vendor, platform-signed pre-installed component and contains the weaknesses described in the report. **Do not install it on a device you use day to day;** analyze it only in an isolated VM or emulator.
- The scripts in `tools/` are intended solely for offline verification against a **local mock server**. **Do not send any request to a real production server.**
- This repository is provided for security research, privacy research and educational purposes only.

### Disclaimer

- The report is AI-generated and has undergone partial human review. It is **not legal advice** and is not an accusation against any company or individual; its compliance and legal analysis is a technical assessment only.
- The sample remains the property of its rights holder and is retained here for research purposes only. Reasonable requests from the rights holder will be honored.
- Corrections and independent verification results are welcome via email at `AlexZak135@proton.me`.

### License

- **Report text:** CC0 1.0 Universal (`CC0-1.0`).
- **Tool code:** CC0 1.0 Universal (`CC0-1.0`).
- **APK sample:** Not covered by the above license; all rights remain with its respective rights holder.

---

## Русский

### ⚠️ Уведомление об анализе, выполненном ИИ

«Сводный отчёт по обратной разработке и аудиту безопасности SystemHelper», все его черновики и упорядоченные результаты декомпиляции были **в основном проанализированы и составлены с использованием DeepSeek V4.1 Flash, CodeX и JADX**, с последующей частичной проверкой человеком.

Анализ, выполненный ИИ, может содержать:

- ошибки в интерпретации результатов декомпиляции (сам отчёт отмечает 5 мест, где JADX не смог восстановить поток управления; они были дополнены по семантике smali);
- предположения, поданные как факты, либо пропущенные пути выполнения кода;
- неточные ссылки на правовые нормы или неверное их применение.

**Не считайте этот отчёт полностью независимо проверенным человеком авторитетным заключением.** Перед цитированием перепроверяйте утверждения по индексу доказательств в Приложении A отчёта.

Статус проверки человеком: проведена частичная проверка; полноценный независимый аудит безопасности третьей стороной не проводился.

### Обзор

В этом репозитории хранится сводный отчёт по обратной разработке и аудиту безопасности `SystemHelper_1.2-20221110.apk`, а также соответствующий двоичный образец. Это APK — компонент управления устройством (DM) для смартфонов в кастомизации China Unicom, предустанавливаемый с подписью системного уровня.

Отчёт рассматривает как **приватность** (что собирается и соответствует ли это требованиям), так и **безопасность** (какие уязвимости есть у самого приложения).

### Информация об образце

| Параметр | Значение |
|---|---|
| Имя файла | `SystemHelper_1.2-20221110.apk` |
| Пакет / версия | `com.mobiletools.systemhelper` / `1.2-20221110` (versionCode 3) |
| Размер | 1 762 193 байт |
| MD5 | `a78da3f746597509ab61054689a65353` |
| SHA-256 | `6658c4be127bdf57ae276a91ae686a9fe26e65f17ee3ba5dec91c61d51fee3b4` |
| Сертификат подписи | `CN=bbkmobile.com.cn, OU=IQOO, O=BBK, C=CN` (SHA1withRSA / 2048) |
| SDK | minSdk 24 / targetSdk 33 |

Перед использованием проверьте хеши.

### Основные выводы (сделаны ИИ)

- **Приватность:** каждая отправляемая запись содержит 39 полей (IMEI/IMSI/ICCID обеих SIM-карт, номер телефона, данные о базовых станциях, имена пакетов всех установленных приложений с временем использования и трафиком и др.) и периодически отправляется на `dm.wo.com.cn`. Отчёт делает вывод, что механизм уведомления и получения согласия фактически не реализован.
- **Безопасность:** экспортируемый `RegisterHelperService` не объявляет `android:permission`, поэтому любое приложение может подключиться к нему и перезаписать адрес сервера отправки; передача идёт по открытому HTTP; криптография применена неверно (ключ получается как MD5 от открытого текста, AES-ECB).
- **18 находок:** 4 критических / 5 высоких / 5 средних / 4 низких.
- **Проверка слухов:** «записывает экран» — не подтверждается; «отправляет имена пакетов всех приложений» — подтверждается; «отправляет APK-файлы» — не подтверждается.

### Градация доказательств

Каждый вывод в отчёте помечен уровнем доказательности:

| Метка | Значение |
|---|---|
| `[A]` | Напрямую подтверждено кодом, ресурсами или Manifest; воспроизводимо |
| `[B]` | Строго выведено из логики кода |
| `[C]` | Требует данных сервера или времени выполнения; **отчёт не может это проверить** |
| `[D]` | Доказательств нет; **не является обвинением** |

### Методология и ограничения

- Только статический анализ (JADX, структура DEX, таблица ресурсов, разбор сертификатов).
- **Динамическая отладка не проводилась, ни один запрос к целевым серверам не отправлялся;** что сервер делает с данными — полностью неизвестно.
- Декомпиляция по своей природе неточна; влияние других предустановленных компонентов на том же устройстве исключить нельзя.
- Выводы относятся только к этой версии APK и устройствам, на которых она установлена, и **не распространяются** на другие продукты производителя или другие услуги оператора.
- Отчёт оценивает «соответствует ли требованиям / нарушает ли права» и **не делает выводов о мотивах**.

### Структура репозитория

```text
.
├── README.md
├── SystemHelper_Audit-Report.md    # Сводный отчёт по обратной разработке и аудиту безопасности (v1.0)
├── Binaries/                       # Двоичный образец
│   └── SystemHelper_1.2-20221110.apk
├── audit/                          # 5 исходных черновиков
│   └── decompiled/
│       ├── raw/                    # Исходный вывод JADX (без деобфускации; эталон)
│       └── deobfuscated/           # Имена, присвоенные вручную для читаемости
└── tools/                          # Инструменты локальной проверки (только локальный имитатор сервера)
```

### Использование и меры предосторожности

- Образец — предустановленный компонент производителя с подписью системного уровня; он содержит уязвимости, описанные в отчёте. **Не устанавливайте его на устройство, которым вы пользуетесь ежедневно;** анализируйте только в изолированной виртуальной машине или эмуляторе.
- Скрипты в `tools/` предназначены исключительно для офлайн-проверки на **локальном имитаторе сервера**. **Не отправляйте никаких запросов на реальные рабочие серверы.**
- Репозиторий предоставляется только для исследований в области безопасности и приватности и в образовательных целях.

### Отказ от ответственности

- Отчёт создан ИИ и прошёл частичную проверку человеком. Он **не является юридической консультацией** и не содержит обвинений в адрес какой-либо компании или лица; правовой анализ в нём — лишь техническая оценка.
- Права на образец принадлежат правообладателю; он хранится здесь только в исследовательских целях. Обоснованные обращения правообладателя будут рассмотрены.
- Исправления и результаты независимой проверки приветствуются по электронной почте: `AlexZak135@proton.me`.

### Лицензия

- **Текст отчёта:** CC0 1.0 Universal (`CC0-1.0`).
- **Код инструментов:** CC0 1.0 Universal (`CC0-1.0`).
- **Файл APK:** на него не распространяется указанная выше лицензия; все права принадлежат соответствующему правообладателю.
