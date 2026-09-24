package zhiqiu.app.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object FileIo {
    actual fun readText(path: String): String? =
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    actual fun writeText(path: String, content: String) {
        (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    }

    actual fun readBytes(path: String): ByteArray? {
        val data = NSData.create(contentsOfFile = path) ?: return null
        val size = data.length.toInt()
        val out = ByteArray(size)
        out.usePinned { pin ->
            memcpy(pin.addressOf(0), data.bytes, size.toULong())
        }
        return out
    }

    actual fun writeBytes(path: String, content: ByteArray) {
        content.usePinned { pin ->
            val data = NSData.create(bytes = pin.addressOf(0), length = content.size.toULong())
            data.writeToFile(path, true)
        }
    }

    actual fun delete(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    actual fun exists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun join(dir: String, name: String): String =
        if (dir.endsWith("/")) "$dir$name" else "$dir/$name"

    actual fun listFiles(dir: String): List<String> {
        val contents = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, null)
            ?: return emptyList()
        return contents.map { join(dir, it as String) }
    }
}
