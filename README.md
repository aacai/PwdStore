# PwdStore

本地密码管理器。数据只存在本机，无账号、无云同步；默认不联网，仅在你主动生成「文件下载链接二维码」时才会上传到临时镜像。

---

## 功能一览

### 启动与安全

- **主密码**：首次使用需设置主密码（≥ 4 位，设置/修改时显示强度提示）；之后每次启动需解锁
- **免密登录**：可开启，并设置有效时长；期限内自动解锁（若已开启生物识别，仍需先验证指纹 / 面容）
- **生物识别**（Android / iOS / 桌面）：按传感器显示文案；Android/iOS 为指纹或面容；macOS 为 Touch ID（系统也可能允许用登录密码确认）；Windows 为 Windows Hello（指纹/面容/PIN）。设置里开启后，解锁页可走系统验证。Linux 需本机有 `fprintd` 才可用
- **立即锁定**：随时结束免密窗口，回到解锁页
- **后台自动锁定**：切到后台约 60 秒后自动锁定（选文件 / 生物识别期间会临时压制，避免误锁）
- **忘记主密码**：可重置，会清空本地保险库（主密码无法找回）
- **系统自动填充**（Android）：可在系统设置里将 PwdStore 设为自动填充服务；保险库已解锁时，登录框可选用条目填充账号/密码

### 密码条目

- 字段：名称、用户名/邮箱、密码、分类（工作 / 社交 / 其他）、备注、服务图标
- **服务图标**：QQ、微信、GitHub、Google、X、知乎、Facebook、Twitter、Instagram、LinkedIn、Apple、Microsoft 等
- **历史密码**：修改密码时自动保留上一版，最多 10 条；详情页可查看与复制
- **密码生成器**：可配置大小写、数字、符号与长度；偏好跨会话记忆

### 浏览与编辑

- 首页列表：按创建时间倒序；支持关键词搜索与分类筛选；有备注时仅显示图标，不露出备注正文
- 详情页：一键复制用户名 / 密码 / 备注 / 历史密码；用户名 / 密码 / 备注可生成**内容二维码**；密码默认掩码，可切换明文
- 添加 / 编辑：表单校验；编辑时密码变更会写入历史

### 主题

- 浅色 / 深色 / 跟随系统
- 解锁页与首页支持快速切换浅↔深
- 切换主题同步系统栏配色：Android 改状态栏/导航条图标明暗（edge-to-edge 下否则深色界面配深色图标），iOS 改 `overrideUserInterfaceStyle`，桌面端为空实现

### 屏幕适配（edge-to-edge）

- Android 入口 `enableEdgeToEdge()`；targetSdk 37 下窗口不再为键盘缩放，内边距必须自己吃
- 每个页面的 `Scaffold` 统一传 `contentWindowInsets = screenWindowInsets()`（即 `WindowInsets.safeDrawing`，已含状态栏、导航条、刘海与软键盘）。内容层不要再叠 `safeDrawingPadding()`/`imePadding()`，那会和 Scaffold 的内边距重复算

### 语言

- 简体中文 / English / 跟随系统
- 设置页切换后立即生效，无需重启；偏好与主题一样持久化在本地

### 数据管理

- **导出 TXT**：脱敏清单（密码列为 `********`），不含真实密码；预览里可生成**内容二维码**，或上传后生成**下载链接二维码**
- **加密备份**：用主密码加密的完整备份（含历史、图标等）
- **加密备份二维码**：生成备份后上传到临时镜像（优先 [x0.at](https://x0.at/)，失败回退 [uguu.se](https://uguu.se/upload.php)），用下载链接出二维码；链接可能过期
- **导入**：选择备份文件 → 输入备份时的主密码 → 预览 → 合并写入  
  去重规则：标题 + 密码 + 备注 三者全等则跳过；导入后用**当前**主密码重新加密
- **存储详情**：可查看本机数据目录路径
  桌面端用应用私有目录：macOS `~/Library/Application Support/PwdStore`、Windows `%APPDATA%\PwdStore`、Linux `~/.local/share/PwdStore`；不再写 `~/Documents`（macOS 下会弹授权框）。首次启动会把旧的 `~/Documents/PwdStore` 整体复制过去，原目录保留作回滚

### 隐私与加密（默认行为）

- 条目敏感字段入库加密：用户名、密码、备注、历史密码
- 主密码经 PBKDF2 派生密钥；设备侧另有密钥保护免密/指纹用的主密码密文
- 全程本地存储；**内容二维码**不上传；仅「下载链接二维码」会把文件传到临时镜像（与 CI Release 镜像策略一致）

## 版本与发布

- 版本号只有一个来源：`gradle/libs.versions.toml` 的 `[versions] app`。Android `versionName`、桌面安装包版本、应用内「关于」页（构建期生成的 `AppInfo.VERSION`）全部由它派生，不再有 `-PappVersion` 之类的覆盖入口
- 版本号首位不能是 0：桌面打包（jpackage）只接受 1~3 段正整数且不允许以 0 开头，`0.1.0` 这类版本会让 dmg/msi/exe 打包直接失败
- 发布流程：改 toml → 提交 → 打 `vX.Y.Z` 标签并推送。CI 首个 job 会校验标签与 toml 版本一致，不一致直接失败，避免出现「标签一个版本、界面另一个版本」
- 推送 `vX.Y.Z` 标签即触发打包并创建同名 Release
- 桌面安装包产品名为 `PwdStore`（macOS 的应用标识 `bundleID` 仍是 `zhiqiu.app`，保住系统已记住的权限与偏好归属）
- 产物命名：`PwdStore-<版本>-android.apk`、`PwdStore-<版本>-x64.exe` / `.msi`、`PwdStore-<版本>-arm64.dmg`、`pwdstore_<版本>_<amd64|arm64>.deb`（deb 包名按 Debian 规则要求小写）
- 桌面端只用 `packageRelease*` 任务打包（产物在 `desktopApp/build/compose/binaries/main-release`）：只有 release 链路会过 R8 裁剪，macOS dmg 从 126 MB 降到 64 MB，大头是被摘掉的 `material-icons-extended`。`desktopApp/compose-desktop.pro` 是保命规则（Room 反射建的 `*Impl`、`BundledSQLiteDriver` 的 JNI 方法、kotlinx-serialization 的 serializer），改动后必须重跑一次 `packageReleaseDmg` 并验证真机能建库、解锁、备份
- 下载镜像：Release 作业会把每个产物上传到 [x0.at](https://x0.at/)，失败回退 [uguu.se](https://uguu.se/upload.php)，并生成 `latest.json`（版本、大小、sha256、直链）一并上传；镜像链接写进 Release 说明。x0.at 保留 3~100 天、单文件 1 GiB；uguu 只保留 3 小时（128 MiB 上限），仅作应急。镜像失败不阻断 Release
