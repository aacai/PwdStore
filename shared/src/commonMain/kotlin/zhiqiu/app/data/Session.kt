package zhiqiu.app.data

import kotlinx.serialization.json.Json

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

    fun requireKey(): ByteArray = encryptionKey ?: error("未解锁")
}

object AppSession {
    val session = Session()
}
