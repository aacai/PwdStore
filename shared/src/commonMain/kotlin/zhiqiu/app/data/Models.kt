package zhiqiu.app.data

import kotlinx.serialization.Serializable

enum class PasswordCategory(val label: String) {
    WORK("工作"),
    SOCIAL("社交"),
    OTHER("其他");

    companion object {
        fun fromName(name: String): PasswordCategory =
            entries.find { it.name.equals(name, ignoreCase = true) }
                ?: entries.find { "PasswordCategory.${it.name.lowercase()}" == name }
                ?: OTHER
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");
}

data class IconOption(val key: String, val label: String)

object IconCatalog {
    val all = listOf(
        IconOption("qq", "QQ"),
        IconOption("weixin", "微信"),
        IconOption("github", "GitHub"),
        IconOption("google", "Google"),
        IconOption("x", "X"),
        IconOption("zhihu", "知乎"),
        IconOption("facebook", "Facebook"),
        IconOption("twitter", "Twitter"),
        IconOption("instagram", "Instagram"),
        IconOption("linkedin", "LinkedIn"),
        IconOption("apple", "Apple"),
        IconOption("microsoft", "Microsoft"),
        IconOption("other", "其他"),
    )

    fun labelOf(key: String): String =
        all.find { it.key == key }?.label ?: "其他"
}

@Serializable
data class PasswordHistoryItem(
    val password: String,
    val changedAt: Long,
)

@Serializable
data class PasswordEntry(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val category: String = PasswordCategory.OTHER.name,
    val createdAt: Long,
    val notes: String = "",
    val passwordHistory: List<PasswordHistoryItem> = emptyList(),
    val iconKey: String = "other",
) {
    fun categoryEnum(): PasswordCategory = PasswordCategory.fromName(category)
}

@Serializable
data class AuthConfig(
    val enabled: Boolean = false,
    val duration: Double = 24.0,
    val biometricEnabled: Boolean = false,
)

@Serializable
data class GeneratorPrefs(
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = false,
    val length: Int = 8,
    val showPassword: Boolean = true,
)

@Serializable
data class MasterMeta(
    val version: Int = 1,
    val salt: String,
    val verifier: String,
    val iterations: Int = Crypto.ITERATIONS,
)

@Serializable
data class BackupMetadata(
    val version: String = "1.0.0",
    val createdAt: Long,
    val entryCount: Int,
    val checksum: String,
    val description: String,
)

@Serializable
data class BackupPayload(
    val version: String = "1.0.0",
    val timestamp: Long,
    val entryCount: Int,
    val entries: List<PasswordEntry>,
)
