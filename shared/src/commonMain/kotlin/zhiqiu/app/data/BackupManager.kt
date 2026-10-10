package zhiqiu.app.data

import kotlinx.serialization.Serializable
import zhiqiu.app.i18n.I18n

object BackupManager {
    fun exportTxt(entries: List<PasswordEntry>): String {
        val s = I18n.strings
        val sb = StringBuilder()
        sb.appendLine(s.exportTitle)
        sb.appendLine("${s.exportTime}: ${formatDateTime(PlatformTime.nowMillis())}")
        sb.appendLine("${s.entryCount}: ${entries.size}")
        sb.appendLine()
        entries.forEach { e ->
            sb.appendLine("${s.name}: ${e.title}")
            sb.appendLine("${s.exportAccount}: ${e.username}")
            sb.appendLine("${s.password}: ${s.maskedExport}")
            sb.appendLine("${s.category}: ${s.categoryLabel(e.categoryEnum())}")
            sb.appendLine("${s.created}: ${formatDateTime(e.createdAt)}")
            sb.appendLine("---")
        }
        return sb.toString()
    }

    fun exportTxtFileName(now: Long = PlatformTime.nowMillis()): String =
        "pwdstore_export_$now.txt"

    fun backupFileName(now: Long = PlatformTime.nowMillis()): String =
        "pwdstore_backup_$now.json"

    fun createEncryptedBackup(entries: List<PasswordEntry>, masterPassword: String): ByteArray {
        require(masterPassword.isNotEmpty()) { I18n.strings.setMasterFirst }
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
        val s = I18n.strings
        require(password.isNotEmpty()) { s.errBackupPasswordEmpty }
        val pkg = try {
            JsonX.json.decodeFromString(BackupPackage.serializer(), bytes.decodeToString())
        } catch (_: Exception) {
            error(s.errNotBackup)
        }
        val encBytes = pkg.encryptedData.encodeToByteArray()
        if (Crypto.md5Hex(encBytes) != pkg.metadata.checksum) {
            error(s.errBackupCorrupt)
        }
        val salt = try {
            Crypto.decodeBase64(pkg.salt)
        } catch (_: Exception) {
            error(s.errBackupCorrupt)
        }
        val key = Crypto.deriveKey(password, salt)
        val plain = try {
            Crypto.decrypt(pkg.encryptedData, key)
        } catch (_: Exception) {
            error(s.errBackupPassword)
        }
        return try {
            JsonX.json.decodeFromString(BackupPayload.serializer(), plain)
        } catch (_: Exception) {
            error(s.errBackupCorrupt)
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
