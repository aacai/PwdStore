package zhiqiu.app.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import zhiqiu.app.data.IconCatalog
import zhiqiu.app.data.PasswordCategory
import zhiqiu.app.data.ThemeMode

class Strings(private val zh: Boolean) {
    val appName = "PwdStore"
    val ok = if (zh) "确定" else "OK"
    val cancel = if (zh) "取消" else "Cancel"
    val close = if (zh) "关闭" else "Close"
    val retry = if (zh) "重试" else "Retry"
    val save = if (zh) "保存" else "Save"
    val select = if (zh) "选择" else "Select"
    val generate = if (zh) "生成" else "Generate"
    val processing = if (zh) "处理中…" else "Working…"
    val cdBack = if (zh) "返回" else "Back"
    val cdCopy = if (zh) "复制" else "Copy"
    val cdEdit = if (zh) "编辑" else "Edit"
    val cdDelete = if (zh) "删除" else "Delete"
    val cdSettings = if (zh) "设置" else "Settings"
    val cdAdd = if (zh) "添加" else "Add"
    val cdTheme = if (zh) "切换主题" else "Toggle theme"
    val cdShowPassword = if (zh) "显示密码" else "Show password"
    val cdHidePassword = if (zh) "隐藏密码" else "Hide password"
    val cdChangeIcon = if (zh) "更改图标" else "Change icon"
    val cdRegenerate = if (zh) "重新生成" else "Regenerate"

    val setupTitle = if (zh) "设置主密码" else "Set a master password"
    val setupHint = if (zh) "主密码用于加密您的所有密码，请妥善保管" else "It encrypts every password you store. Keep it safe."
    val masterPassword = if (zh) "主密码" else "Master password"
    val confirmPassword = if (zh) "确认密码" else "Confirm password"
    val startUsing = if (zh) "开始使用" else "Get started"
    val unlock = if (zh) "解锁" else "Unlock"
    val welcomeBack = if (zh) "欢迎回来" else "Welcome back"
    val unlockHint = if (zh) "输入主密码继续" else "Enter your master password to continue"
    val biometricUnlock = if (zh) "指纹 / 面容解锁" else "Unlock with fingerprint or face"
    val forgotPassword = if (zh) "忘记密码？" else "Forgot password?"
    val resetTitle = if (zh) "重置主密码？" else "Reset master password?"
    val resetText = if (zh) "重置会删除所有密码数据，不可恢复。若只想改主密码，请登录后到设置页修改。" else "Resetting permanently deletes every stored password. To only change it, use Settings after unlocking."
    val resetConfirmTitle = if (zh) "再次确认" else "Confirm again"
    val resetConfirmText = if (zh) "此操作无法撤销，确定删除全部数据？" else "This cannot be undone. Delete all data?"

    val vault = if (zh) "密码库" else "Vault"
    val searchHint = if (zh) "搜索名称/用户名" else "Search name or username"
    val filterAll = if (zh) "全部" else "All"
    val loadFailed = if (zh) "加载失败" else "Failed to load"
    val emptyTitle = if (zh) "还没有添加任何密码" else "No passwords yet"
    val emptyHint = if (zh) "点右下角开始添加" else "Use the + button to add one"
    val noMatch = if (zh) "没有找到匹配的密码" else "No matching passwords"
    val clearFilters = if (zh) "清除筛选" else "Clear filters"

    val detail = if (zh) "详情" else "Details"
    val entryMissing = if (zh) "密码不存在" else "Entry not found"
    val username = if (zh) "用户名" else "Username"
    val password = if (zh) "密码" else "Password"
    val category = if (zh) "分类" else "Category"
    val created = if (zh) "创建时间" else "Created"
    val notes = if (zh) "备注" else "Notes"
    val masked = "••••••••"
    val maskedExport = "********"
    val needMasterToView = if (zh) "需要主密码才能查看" else "Master password required to view"
    val deleteTitle = if (zh) "删除条目？" else "Delete entry?"
    val irreversible = if (zh) "此操作无法撤销" else "This cannot be undone"

    val addEntry = if (zh) "添加密码" else "Add password"
    val editEntry = if (zh) "编辑密码" else "Edit password"
    val name = if (zh) "名称" else "Name"
    val usernameOrEmail = if (zh) "用户名/邮箱" else "Username or email"
    val selectIcon = if (zh) "选择图标" else "Select icon"
    val selectCategory = if (zh) "选择分类" else "Select category"
    val generatorSettings = if (zh) "生成设置" else "Generator settings"
    val genUppercase = if (zh) "大写" else "Uppercase"
    val genLowercase = if (zh) "小写" else "Lowercase"
    val genDigits = if (zh) "数字" else "Digits"
    val genSymbols = if (zh) "特殊字符" else "Symbols"

    val settings = if (zh) "设置" else "Settings"
    val sectionSecurity = if (zh) "安全设置" else "Security"
    val sectionDisplay = if (zh) "显示设置" else "Display"
    val sectionData = if (zh) "数据管理" else "Data"
    val sectionAbout = if (zh) "关于" else "About"
    val changeMasterPassword = if (zh) "修改主密码" else "Change master password"
    val passwordFree = if (zh) "免密登录" else "Password-free unlock"
    val passwordFreeDuration = if (zh) "免密时长（天）" else "Password-free duration (days)"
    val biometricUnlockRow = if (zh) "生物识别解锁" else "Biometric unlock"
    val biometricOn = if (zh) "已开启，解锁页可使用指纹/面容" else "On — unlock with fingerprint or face"
    val biometricOff = if (zh) "开启后可用指纹或面容快速解锁" else "Turn on to unlock with fingerprint or face"
    val biometricUnavailable = if (zh) "当前设备不支持或未录入指纹/面容" else "Not supported here, or no fingerprint/face is enrolled"
    val biometricNoHardware = if (zh) "此设备没有指纹/面容硬件" else "This device has no fingerprint or face sensor"
    val biometricEnrollHint = if (zh) "请先在系统设置中录入指纹或面容" else "Enroll a fingerprint or face in system settings first"
    val biometricSetupTitle = if (zh) "确认开启生物识别" else "Confirm biometrics"
    val biometricSetupSubtitle = if (zh) "验证后即可用指纹/面容解锁" else "Verify once to enable biometric unlock"
    val lockNow = if (zh) "立即锁定" else "Lock now"
    val theme = if (zh) "主题" else "Theme"
    val language = if (zh) "语言" else "Language"
    val languageSystem = if (zh) "跟随系统" else "System default"
    val exportTxt = if (zh) "导出数据（TXT）" else "Export data (TXT)"
    val nothingToExport = if (zh) "暂无密码数据可导出" else "Nothing to export yet"
    val exportPreview = if (zh) "导出预览" else "Export preview"
    val confirmExport = if (zh) "确认导出" else "Export"
    val encryptedBackup = if (zh) "加密备份" else "Encrypted backup"
    val setMasterFirst = if (zh) "请先设置主密码" else "Set a master password first"
    val importData = if (zh) "导入数据" else "Import data"
    val import = if (zh) "导入" else "Import"
    val noFilePicked = if (zh) "未选择文件" else "No file selected"
    val fileChooserTitle = if (zh) "选择备份文件" else "Select backup file"
    val fileFilterBackup = if (zh) "备份文件" else "Backup files"
    val shareFile = if (zh) "分享文件" else "Share file"
    val backupPasswordTitle = if (zh) "输入备份密码" else "Enter backup password"
    val backupPasswordHint = if (zh) "备份时的主密码" else "Master password used when the backup was made"
    val parsing = if (zh) "解析中…" else "Parsing…"
    val parse = if (zh) "解析" else "Parse"
    val parseFailed = if (zh) "解析失败" else "Parse failed"
    val importPreview = if (zh) "导入预览" else "Import preview"
    val version = if (zh) "版本" else "Version"
    val time = if (zh) "时间" else "Time"
    val entryCount = if (zh) "条数" else "Entries"
    val importing = if (zh) "导入中…" else "Importing…"
    val copyFullData = if (zh) "复制完整数据" else "Copy all data"
    val copiedFullData = if (zh) "已复制完整数据" else "All data copied"
    val copied = if (zh) "已复制" else "Copied"
    val storageDetails = if (zh) "存储详情" else "Storage details"
    val storageDir = if (zh) "目录" else "Directory"
    val storageDb = if (zh) "数据库" else "Database"
    val storageNote = if (zh) "导出/备份会写入同目录（带时间戳文件名）" else "Exports and backups are written here with timestamped file names"
    val aboutApp = if (zh) "关于应用" else "About"
    val aboutBody = if (zh) "本地存储 · 高强度加密 · 主密码保护" else "Local storage · strong encryption · master password protection"
    val appVersion = if (zh) "版本 1.0" else "Version 1.0"

    val errPasswordShort = if (zh) "主密码至少 4 位" else "Master password must be at least 4 characters"
    val errPasswordMismatch = if (zh) "两次输入不一致" else "The two passwords do not match"
    val errEmptyPassword = if (zh) "请输入密码" else "Enter your password"
    val errWrongPassword = if (zh) "密码错误" else "Incorrect password"
    val errCurrentPassword = if (zh) "当前密码不正确" else "Current password is wrong"
    val errRequiredFields = if (zh) "请填写必填项" else "Required fields are empty"
    val errNoCharType = if (zh) "请至少选择一种字符类型" else "Select at least one character type"
    val backupFailed = if (zh) "备份失败" else "Backup failed"
    val masterChanged = if (zh) "主密码已修改" else "Master password changed"
    val notUnlocked = if (zh) "未解锁" else "Locked"
    val errCipherTooShort = if (zh) "密文过短" else "Ciphertext too short"
    val errIntegrity = if (zh) "完整性校验失败" else "Integrity check failed"
    val errNotBackup = if (zh) "解析失败：不是有效的备份文件" else "Parse failed: not a valid backup file"
    val errBackupCorrupt = if (zh) "解析失败：文件已损坏" else "Parse failed: file is corrupted"
    val errBackupPassword = if (zh) "解析失败：密码错误或文件损坏" else "Parse failed: wrong password or corrupted file"
    val errBackupPasswordEmpty = if (zh) "请输入备份密码" else "Enter the backup password"

    val biometricNotStarted = if (zh) "无法启动生物识别" else "Can't start biometrics"
    val biometricUsePassword = if (zh) "使用密码" else "Use password"
    val biometricNoCredential = if (zh) "生物识别成功，但本地凭证不可用，请输入主密码" else "Biometrics verified, but the local credential is unavailable — enter your master password"
    val biometricPlatformUnsupported = if (zh) "当前平台不支持生物识别" else "This platform does not support biometrics"

    val exportTitle = if (zh) "密码导出" else "Password export"
    val exportTime = if (zh) "导出时间" else "Exported at"
    val exportAccount = if (zh) "账号" else "Account"

    fun daysOf(days: String): String = if (zh) "时长 $days 天" else "$days days"
    fun recordCount(count: Int): String = if (zh) "$count 条记录" else "$count records"
    fun historyCount(count: Int): String = if (zh) "历史密码 ($count)" else "Password history ($count)"
    fun viewAllHistory(count: Int): String = if (zh) "查看全部 $count 个" else "View all $count"
    fun confirmDeleteEntry(title: String): String = if (zh) "确定删除「$title」？" else "Delete \"$title\"?"
    fun passwordLength(length: Int): String = if (zh) "当前长度: $length 位" else "Length: $length"
    fun moreEntries(count: Int): String = if (zh) "… 还有 $count 条" else "… $count more"
    fun importedCount(count: Int): String = if (zh) "已导入 $count 条" else "$count entries imported"
    fun exportedTo(fileName: String): String = if (zh) "已导出: $fileName" else "Exported: $fileName"
    fun backupSavedTo(fileName: String): String = if (zh) "备份已保存: $fileName" else "Backup saved: $fileName"
    fun setupFailedWith(reason: String): String = if (zh) "设置失败: $reason" else "Setup failed: $reason"

    fun categoryLabel(category: PasswordCategory): String = when (category) {
        PasswordCategory.WORK -> if (zh) "工作" else "Work"
        PasswordCategory.SOCIAL -> if (zh) "社交" else "Social"
        PasswordCategory.OTHER -> if (zh) "其他" else "Other"
    }

    fun themeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> if (zh) "跟随系统" else "System"
        ThemeMode.LIGHT -> if (zh) "浅色" else "Light"
        ThemeMode.DARK -> if (zh) "深色" else "Dark"
    }

    fun languageLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.SYSTEM -> languageSystem
        AppLanguage.ZH -> "简体中文"
        AppLanguage.EN -> "English"
    }

    fun iconLabel(key: String): String = when (key) {
        "other" -> if (zh) "其他" else "Other"
        else -> IconCatalog.nameOf(key)
    }
}

enum class AppLanguage {
    SYSTEM,
    ZH,
    EN,
}

private val ZhStrings by lazy { Strings(true) }
private val EnStrings by lazy { Strings(false) }

object I18n {
    var language: AppLanguage by mutableStateOf(AppLanguage.SYSTEM)

    val strings: Strings
        get() = if (usesChinese) ZhStrings else EnStrings

    val usesChinese: Boolean
        get() = when (language) {
            AppLanguage.ZH -> true
            AppLanguage.EN -> false
            AppLanguage.SYSTEM -> Locale.current.language.startsWith("zh")
        }
}
