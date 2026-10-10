package zhiqiu.app.data

import kotlinx.serialization.json.Json
import zhiqiu.app.i18n.I18n

object JsonX {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
}

object StoreFiles {
    const val DB = "pwdstore.db"
}

object MetaKeys {
    const val MASTER = "master"
    const val AUTH = "auth"
    const val AUTH_TS = "auth_timestamp"
    const val ENC_MASTER = "encrypted_master"
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val GENERATOR = "generator"
}

class Session {
    var masterPassword: String? = null
        private set
    var encryptionKey: ByteArray? = null
        private set

    val isUnlocked: Boolean get() = encryptionKey != null

    fun unlock(password: String, key: ByteArray) {
        masterPassword = password
        encryptionKey = key
    }

    fun updatePassword(password: String, key: ByteArray) {
        masterPassword = password
        encryptionKey = key
    }

    fun lock() {
        masterPassword = null
        encryptionKey = null
    }

    fun requireKey(): ByteArray = encryptionKey ?: error(I18n.strings.notUnlocked)
}

object AppSession {
    val session = Session()
}
