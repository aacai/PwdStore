package zhiqiu.app.data

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

actual object SecureDeviceKey {
    private const val FILE = ".device_key"

    actual fun getOrCreate(): ByteArray {
        val file = File(AppBootstrap.resolveDir(), FILE)
        if (file.exists()) {
            return Crypto.decodeBase64(file.readText().trim())
        }
        val raw = Crypto.randomBytes(32)
        file.parentFile?.mkdirs()
        file.writeText(Crypto.encodeBase64(raw))
        runCatching {
            Files.setPosixFilePermissions(
                file.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        }
        return raw
    }
}
