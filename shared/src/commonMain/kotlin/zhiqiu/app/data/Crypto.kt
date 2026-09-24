package zhiqiu.app.data

object Crypto {
    const val ITERATIONS = 120_000
    const val KEY_BYTES = 32
    const val SALT_BYTES = 16
    const val IV_BYTES = 16
    const val HMAC_BYTES = 32

    fun randomBytes(size: Int): ByteArray = PlatformCrypto.randomBytes(size)

    fun deriveKey(password: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray =
        PlatformCrypto.pbkdf2(password.encodeToByteArray(), salt, iterations, KEY_BYTES)

    fun verifierOf(key: ByteArray): String = encodeBase64(PlatformCrypto.sha256(key))

    fun encrypt(plain: String, key: ByteArray): String {
        val iv = randomBytes(IV_BYTES)
        val cipher = PlatformCrypto.aesCbcEncrypt(plain.encodeToByteArray(), key, iv)
        val mac = PlatformCrypto.hmacSha256(key, iv + cipher)
        return encodeBase64(iv + cipher + mac)
    }

    fun decrypt(payload: String, key: ByteArray): String {
        val raw = decodeBase64(payload)
        require(raw.size > IV_BYTES + HMAC_BYTES) { "密文过短" }
        val iv = raw.copyOfRange(0, IV_BYTES)
        val mac = raw.copyOfRange(raw.size - HMAC_BYTES, raw.size)
        val cipher = raw.copyOfRange(IV_BYTES, raw.size - HMAC_BYTES)
        val expect = PlatformCrypto.hmacSha256(key, iv + cipher)
        require(constantTimeEquals(mac, expect)) { "完整性校验失败" }
        return PlatformCrypto.aesCbcDecrypt(cipher, key, iv).decodeToString()
    }

    fun md5Hex(data: ByteArray): String = PlatformCrypto.md5(data).joinToString("") { b ->
        ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
    }

    fun encodeBase64(data: ByteArray): String = PlatformCrypto.encodeBase64(data)
    fun decodeBase64(text: String): ByteArray = PlatformCrypto.decodeBase64(text)

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt())
        return r == 0
    }
}

expect object PlatformCrypto {
    fun randomBytes(size: Int): ByteArray
    fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray
    fun aesCbcEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun aesCbcDecrypt(cipher: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
    fun sha256(data: ByteArray): ByteArray
    fun md5(data: ByteArray): ByteArray
    fun encodeBase64(data: ByteArray): String
    fun decodeBase64(text: String): ByteArray
}
