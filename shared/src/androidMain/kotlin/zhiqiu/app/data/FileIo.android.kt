package zhiqiu.app.data

import java.io.File

actual object FileIo {
    actual fun readText(path: String): String? {
        val f = File(path)
        return if (f.exists()) f.readText() else null
    }

    actual fun writeText(path: String, content: String) {
        val f = File(path)
        f.parentFile?.mkdirs()
        f.writeText(content)
    }

    actual fun readBytes(path: String): ByteArray? {
        val f = File(path)
        return if (f.exists()) f.readBytes() else null
    }

    actual fun writeBytes(path: String, content: ByteArray) {
        val f = File(path)
        f.parentFile?.mkdirs()
        f.writeBytes(content)
    }

    actual fun delete(path: String) {
        File(path).delete()
    }

    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun join(dir: String, name: String): String = File(dir, name).absolutePath

    actual fun listFiles(dir: String): List<String> =
        File(dir).listFiles()?.map { it.absolutePath } ?: emptyList()
}
