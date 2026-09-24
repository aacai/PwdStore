package zhiqiu.app.data

actual object SecureDeviceKey {
    private const val FILE = ".device_key"

    actual fun getOrCreate(): ByteArray {
        val path = FileIo.join(AppBootstrap.resolveDir(), FILE)
        val existing = FileIo.readText(path)
        if (existing != null) return Crypto.decodeBase64(existing.trim())
        val raw = Crypto.randomBytes(32)
        FileIo.writeText(path, Crypto.encodeBase64(raw))
        return raw
    }
}
