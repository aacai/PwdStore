package zhiqiu.app.data

import java.io.File

private const val APP_DIR = "PwdStore"
private const val DB_FILE = "pwdstore.db"

actual object AppPaths {
    actual fun dataDir(): String {
        val home = File(System.getProperty("user.home"))
        val dir = File(
            desktopDataRoot(
                osName = System.getProperty("os.name"),
                home = home,
                env = System::getenv,
            ),
            APP_DIR,
        )
        migrateLegacyDataDir(dir, home)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
}

/** 各系统的私有数据目录；不用 ~/Documents，否则 macOS 每次读写都要用户手动授权 */
internal fun desktopDataRoot(osName: String, home: File, env: (String) -> String?): File {
    fun envDir(name: String): File? = env(name)?.takeIf { it.isNotBlank() }?.let(::File)
    return when {
        osName.lowercase().contains("mac") -> File(home, "Library/Application Support")
        osName.lowercase().contains("win") -> envDir("APPDATA") ?: File(home, "AppData/Roaming")
        else -> envDir("XDG_DATA_HOME") ?: File(home, ".local/share")
    }
}

/** 早期版本把数据写在 ~/Documents/PwdStore，首次启动到新目录时整体搬迁 */
internal fun migrateLegacyDataDir(target: File, home: File): Boolean {
    if (File(target, DB_FILE).isFile) return false
    val legacy = sequenceOf("Documents", "文档")
        .map { File(File(home, it), APP_DIR) }
        .firstOrNull { File(it, DB_FILE).isFile } ?: return false
    target.mkdirs()
    legacy.listFiles()?.forEach { src ->
        if (src.isFile) src.copyTo(File(target, src.name), overwrite = false)
    }
    return true
}
