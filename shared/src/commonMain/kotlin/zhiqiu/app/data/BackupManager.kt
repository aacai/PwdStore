package zhiqiu.app.data

import kotlinx.serialization.Serializable

object BackupManager {
    fun exportTxt(entries: List<PasswordEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("密码导出")
        sb.appendLine("导出时间: ${formatDateTime(PlatformTime.nowMillis())}")
        sb.appendLine("条目数: ${entries.size}")
        sb.appendLine()
        entries.forEach { e ->
            sb.appendLine("名称: ${e.title}")
            sb.appendLine("账号: ${e.username}")
            sb.appendLine("密码: ********")
            sb.appendLine("分类: ${e.categoryEnum().label}")
            sb.appendLine("创建时间: ${formatDateTime(e.createdAt)}")
            sb.appendLine("---")
        }
        return sb.toString()
    }

    fun exportTxtFileName(now: Long = PlatformTime.nowMillis()): String =
        "密码数据_$now.txt"

    fun backupFileName(now: Long = PlatformTime.nowMillis()): String =
        "pwdbackup_$now.json"

    fun createEncryptedBackup(entries: List<PasswordEntry>, masterPassword: String): ByteArray {
        require(masterPassword.isNotEmpty()) { "请先设置主密码" }
        val payload = BackupPayload(
            timestamp = PlatformTime.nowMillis(),
            entryCount = entries.size,
            entries = entries,
        )
        val plain = JsonX.json.encodeToString(BackupPayload.serializer(), payload)
        val salt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val key = Crypto.deriveKey(masterPassword, salt)
        val encrypted = Crypto.encrypt(plain, key)
        val checksum = Crypto.md5Hex(encrypted.encodeToByteArray())
        val packageData = BackupPackage(
            metadata = BackupMetadata(
                createdAt = payload.timestamp,
                entryCount = entries.size,
                checksum = checksum,
                description = "PwdStore backup ${formatDateTime(payload.timestamp)}",
            ),
            salt = Crypto.encodeBase64(salt),
            encryptedData = encrypted,
        )
        return JsonX.json.encodeToString(BackupPackage.serializer(), packageData).encodeToByteArray()
    }

    fun parseBackup(bytes: ByteArray, password: String): BackupPayload {
        require(password.isNotEmpty()) { "请输入备份密码" }
        val pkg = try {
            JsonX.json.decodeFromString(BackupPackage.serializer(), bytes.decodeToString())
        } catch (_: Exception) {
            error("解析失败：不是有效的备份文件")
        }
        val encBytes = pkg.encryptedData.encodeToByteArray()
        if (Crypto.md5Hex(encBytes) != pkg.metadata.checksum) {
            error("解析失败：文件已损坏")
        }
        val salt = try {
            Crypto.decodeBase64(pkg.salt)
        } catch (_: Exception) {
            error("解析失败：文件已损坏")
        }
        val key = Crypto.deriveKey(password, salt)
        val plain = try {
            Crypto.decrypt(pkg.encryptedData, key)
        } catch (_: Exception) {
            error("解析失败：密码错误或文件损坏")
        }
        return try {
            JsonX.json.decodeFromString(BackupPayload.serializer(), plain)
        } catch (_: Exception) {
            error("解析失败：文件已损坏")
        }
    }

    fun formatDateTime(millis: Long): String = PlatformFormat.formatDateTime(millis)
}

@Serializable
data class BackupPackage(
    val metadata: BackupMetadata,
    val salt: String,
    val encryptedData: String,
)

expect object PlatformFormat {
    fun formatDateTime(millis: Long): String
}

expect object PlatformActions {
    fun copyToClipboard(text: String)
    fun shareOrRevealFile(path: String, mimeType: String)
    fun pickFile(onResult: (ByteArray?) -> Unit)
    fun isDesktop(): Boolean
}
