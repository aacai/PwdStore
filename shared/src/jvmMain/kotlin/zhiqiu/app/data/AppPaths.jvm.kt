package zhiqiu.app.data

import java.io.File

actual object AppPaths {
    actual fun documentsDir(): String {
        val home = File(System.getProperty("user.home"))
        val docs = sequenceOf("Documents", "文档")
            .map { File(home, it) }
            .firstOrNull { it.exists() || it.mkdirs() }
            ?: File(home, "Documents").also { it.mkdirs() }
        val dir = File(docs, "PwdStore")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
}
