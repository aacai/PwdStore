package zhiqiu.app.data

import androidx.collection.mutableScatterMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import zhiqiu.app.db.DbProvider
import zhiqiu.app.db.MetaEntity
import zhiqiu.app.db.PasswordEntryEntity
import zhiqiu.app.db.PwdDao
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PasswordRepository(
    private val session: Session = AppSession.session,
) {
    private val cache = mutableScatterMapOf<String, PasswordEntry>()
    private var loaded = false
    private fun dir() = AppBootstrap.resolveDir()
    private fun dao(): PwdDao = DbProvider.dao()

    private fun <T> db(block: suspend PwdDao.() -> T): T =
        runBlocking(Dispatchers.IO) { dao().block() }

    fun hasMasterPassword(): Boolean {
        return readMasterMeta() != null
    }

    fun setupMasterPassword(password: String) {
        require(password.length >= 4) { "主密码至少 4 位" }
        val salt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val key = Crypto.deriveKey(password, salt)
        val meta = MasterMeta(
            salt = Crypto.encodeBase64(salt),
            verifier = Crypto.verifierOf(key),
        )
        db {
            clearAll()
            upsertMeta(MetaEntity(MetaKeys.MASTER, JsonX.json.encodeToString(MasterMeta.serializer(), meta)))
        }
        session.unlock(password, key)
        cache.clear()
        loaded = true
    }

    fun verifyMasterPassword(password: String): Boolean {
        val meta = readMasterMeta() ?: return false
        val salt = Crypto.decodeBase64(meta.salt)
        val key = Crypto.deriveKey(password, salt, meta.iterations)
        if (Crypto.verifierOf(key) != meta.verifier) return false
        session.unlock(password, key)
        loaded = false
        return true
    }

    fun changeMasterPassword(current: String, newPassword: String) {
        require(newPassword.length >= 4) { "主密码至少 4 位" }
        require(session.masterPassword == current) { "当前密码不正确" }
        val entries = getAllDecrypted()
        val salt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val newKey = Crypto.deriveKey(newPassword, salt)
        val meta = MasterMeta(
            salt = Crypto.encodeBase64(salt),
            verifier = Crypto.verifierOf(newKey),
        )
        session.updatePassword(newPassword, newKey)
        val encrypted = entries.map { encryptEntry(it, newKey) }
        val auth = readAuthConfig()
        db {
            upsertMeta(MetaEntity(MetaKeys.MASTER, JsonX.json.encodeToString(MasterMeta.serializer(), meta)))
            deleteAllEntries()
            upsertEntries(encrypted.map { it.toEntity() })
            if (auth.enabled || auth.biometricEnabled) {
                upsertMeta(MetaEntity(MetaKeys.ENC_MASTER, encryptMasterForDevice(newPassword)))
                if (auth.enabled) {
                    upsertMeta(MetaEntity(MetaKeys.AUTH_TS, PlatformTime.nowMillis().toString()))
                }
            }
        }
        cache.clear()
        encrypted.forEach { cache[it.id] = it }
        loaded = true
    }

    fun resetAll() {
        db { clearAll() }
        DbProvider.close()
        session.lock()
        cache.clear()
        loaded = false
    }

    fun getAllDecrypted(): List<PasswordEntry> {
        ensureLoaded()
        val key = session.requireKey()
        val result = ArrayList<PasswordEntry>(cache.size)
        cache.forEach { _, entry -> result.add(decryptEntry(entry, key)) }
        return result.sortedByDescending { it.createdAt }
    }

    fun getById(id: String): PasswordEntry? {
        ensureLoaded()
        val raw = cache[id] ?: return null
        return try {
            decryptEntry(raw, session.requireKey())
        } catch (_: Exception) {
            raw.copy(password = "需要主密码才能查看")
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun add(entry: PasswordEntry): PasswordEntry {
        ensureLoaded()
        val key = session.requireKey()
        val withId = if (entry.id.isBlank()) entry.copy(id = Uuid.random().toString()) else entry
        val encrypted = encryptEntry(withId, key)
        cache[encrypted.id] = encrypted
        db { upsertEntry(encrypted.toEntity()) }
        return withId
    }

    fun update(entry: PasswordEntry, previousPassword: String?) {
        ensureLoaded()
        val key = session.requireKey()
        var history = entry.passwordHistory
        if (previousPassword != null && previousPassword != entry.password) {
            history = (listOf(PasswordHistoryItem(previousPassword, PlatformTime.nowMillis())) + history)
                .take(10)
        }
        val toSave = entry.copy(passwordHistory = history)
        val encrypted = encryptEntry(toSave, key)
        cache[entry.id] = encrypted
        db { upsertEntry(encrypted.toEntity()) }
    }

    fun delete(id: String) {
        ensureLoaded()
        cache.remove(id)
        db { deleteEntry(id) }
    }

    fun replaceAll(entries: List<PasswordEntry>) {
        val key = session.requireKey()
        cache.clear()
        val encrypted = entries.map { encryptEntry(it, key).also { e -> cache[e.id] = e } }
        db {
            deleteAllEntries()
            upsertEntries(encrypted.map { it.toEntity() })
        }
        loaded = true
    }

    fun importMerge(incoming: List<PasswordEntry>): Int {
        ensureLoaded()
        val existing = getAllDecrypted()
        val dup = existing.map { Triple(it.title, it.password, it.notes) }.toSet()
        var added = 0
        incoming.forEach { item ->
            if (Triple(item.title, item.password, item.notes) !in dup) {
                add(
                    item.copy(
                        id = "",
                        createdAt = item.createdAt.takeIf { it > 0 } ?: PlatformTime.nowMillis(),
                        passwordHistory = item.passwordHistory.take(10),
                    ),
                )
                added++
            }
        }
        return added
    }

    fun readAuthConfig(): AuthConfig {
        val raw = db { getMeta(MetaKeys.AUTH) } ?: return AuthConfig()
        return runCatching { JsonX.json.decodeFromString(AuthConfig.serializer(), raw) }.getOrDefault(AuthConfig())
    }

    fun writeAuthConfig(config: AuthConfig) {
        val needEnc = config.enabled || config.biometricEnabled
        db {
            upsertMeta(MetaEntity(MetaKeys.AUTH, JsonX.json.encodeToString(AuthConfig.serializer(), config)))
            if (!needEnc) {
                deleteMeta(MetaKeys.AUTH_TS)
                deleteMeta(MetaKeys.ENC_MASTER)
            } else {
                val enc = session.masterPassword?.let { encryptMasterForDevice(it) }
                    ?: getMeta(MetaKeys.ENC_MASTER)
                if (enc != null) upsertMeta(MetaEntity(MetaKeys.ENC_MASTER, enc))
                if (config.enabled) {
                    upsertMeta(MetaEntity(MetaKeys.AUTH_TS, PlatformTime.nowMillis().toString()))
                }
            }
        }
    }

    fun touchAuthTimestamp() {
        db { upsertMeta(MetaEntity(MetaKeys.AUTH_TS, PlatformTime.nowMillis().toString())) }
    }

    fun clearPasswordFree() {
        db {
            deleteMeta(MetaKeys.AUTH_TS)
            deleteMeta(MetaKeys.ENC_MASTER)
        }
    }

    fun lockNow() {
        val keepBio = readAuthConfig().biometricEnabled
        db {
            deleteMeta(MetaKeys.AUTH_TS)
            if (!keepBio) deleteMeta(MetaKeys.ENC_MASTER)
        }
        session.lock()
        cache.clear()
        loaded = false
    }

    fun tryPasswordFreeUnlock(): Boolean {
        val config = readAuthConfig()
        if (!config.enabled) return false
        val ts = db { getMeta(MetaKeys.AUTH_TS) }?.toLongOrNull() ?: return false
        val windowMs = (config.duration * 3600_000).toLong()
        if (PlatformTime.nowMillis() - ts > windowMs) {
            if (!config.biometricEnabled) db { deleteMeta(MetaKeys.ENC_MASTER) }
            return false
        }
        return unlockWithEncryptedMaster()
    }

    fun tryBiometricUnlock(): Boolean {
        if (!readAuthConfig().biometricEnabled) return false
        return unlockWithEncryptedMaster()
    }

    private fun unlockWithEncryptedMaster(): Boolean {
        val enc = db { getMeta(MetaKeys.ENC_MASTER) } ?: return false
        val deviceKey = loadOrCreateDeviceKey()
        return try {
            val password = Crypto.decrypt(enc, deviceKey)
            verifyMasterPassword(password)
        } catch (_: Exception) {
            false
        }
    }

    fun onManualUnlockSuccess() {
        val config = readAuthConfig()
        db {
            upsertMeta(MetaEntity(MetaKeys.AUTH_TS, PlatformTime.nowMillis().toString()))
            if (config.enabled || config.biometricEnabled) {
                session.masterPassword?.let {
                    upsertMeta(MetaEntity(MetaKeys.ENC_MASTER, encryptMasterForDevice(it)))
                }
            }
        }
    }

    fun readTheme(): ThemeMode {
        val raw = db { getMeta(MetaKeys.THEME) } ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.find { it.name == raw } ?: ThemeMode.SYSTEM
    }

    fun writeTheme(mode: ThemeMode) {
        db { upsertMeta(MetaEntity(MetaKeys.THEME, mode.name)) }
    }

    fun readGeneratorPrefs(): GeneratorPrefs {
        val raw = db { getMeta(MetaKeys.GENERATOR) } ?: return GeneratorPrefs()
        return runCatching { JsonX.json.decodeFromString(GeneratorPrefs.serializer(), raw) }
            .getOrDefault(GeneratorPrefs())
    }

    fun writeGeneratorPrefs(prefs: GeneratorPrefs) {
        db {
            upsertMeta(
                MetaEntity(MetaKeys.GENERATOR, JsonX.json.encodeToString(GeneratorPrefs.serializer(), prefs)),
            )
        }
    }

    fun readShowPasswordPref(): Boolean = readGeneratorPrefs().showPassword

    fun writeShowPasswordPref(show: Boolean) {
        writeGeneratorPrefs(readGeneratorPrefs().copy(showPassword = show))
    }

    fun documentsPath(): String = dir()

    private fun encryptMasterForDevice(password: String): String =
        Crypto.encrypt(password, SecureDeviceKey.getOrCreate())

    private fun loadOrCreateDeviceKey(): ByteArray = SecureDeviceKey.getOrCreate()

    private fun readMasterMeta(): MasterMeta? {
        val raw = db { getMeta(MetaKeys.MASTER) } ?: return null
        return runCatching { JsonX.json.decodeFromString(MasterMeta.serializer(), raw) }.getOrNull()
    }

    private fun ensureLoaded() {
        if (loaded) return
        session.requireKey()
        cache.clear()
        db { getAllEntries() }.forEach { cache[it.id] = it.toModel() }
        loaded = true
    }


    private fun PasswordEntry.toEntity(): PasswordEntryEntity = PasswordEntryEntity(
        id = id,
        title = title,
        username = username,
        password = password,
        category = category,
        createdAt = createdAt,
        notes = notes,
        iconKey = iconKey,
        passwordHistory = JsonX.json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(PasswordHistoryItem.serializer()),
            passwordHistory,
        ),
    )

    private fun PasswordEntryEntity.toModel(): PasswordEntry {
        val history = runCatching {
            JsonX.json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(PasswordHistoryItem.serializer()),
                passwordHistory,
            )
        }.getOrDefault(emptyList())
        return PasswordEntry(
            id = id,
            title = title,
            username = username,
            password = password,
            category = category,
            createdAt = createdAt,
            notes = notes,
            iconKey = iconKey,
            passwordHistory = history,
        )
    }

    private fun encryptEntry(entry: PasswordEntry, key: ByteArray): PasswordEntry {
        val history = entry.passwordHistory.map {
            it.copy(password = Crypto.encrypt(it.password, key))
        }
        return entry.copy(
            username = Crypto.encrypt(entry.username, key),
            password = Crypto.encrypt(entry.password, key),
            notes = if (entry.notes.isEmpty()) "" else Crypto.encrypt(entry.notes, key),
            passwordHistory = history,
        )
    }

    private fun decryptEntry(entry: PasswordEntry, key: ByteArray): PasswordEntry {
        val history = entry.passwordHistory.map {
            it.copy(password = Crypto.decrypt(it.password, key))
        }
        return entry.copy(
            username = Crypto.decrypt(entry.username, key),
            password = Crypto.decrypt(entry.password, key),
            notes = if (entry.notes.isEmpty()) "" else Crypto.decrypt(entry.notes, key),
            passwordHistory = history,
        )
    }
}

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"

    fun generate(prefs: GeneratorPrefs): String {
        val pool = buildString {
            if (prefs.uppercase) append(UPPER)
            if (prefs.lowercase) append(LOWER)
            if (prefs.digits) append(DIGITS)
            if (prefs.symbols) append(SYMBOLS)
        }
        if (pool.isEmpty()) return "请至少选择一种字符类型"
        val length = prefs.length.coerceIn(4, 20)
        val bytes = Crypto.randomBytes(length)
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(pool[(bytes[i].toInt() and 0xff) % pool.length])
        }
        return sb.toString()
    }
}

expect object PlatformTime {
    fun nowMillis(): Long
}
