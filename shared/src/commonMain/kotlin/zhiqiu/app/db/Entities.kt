package zhiqiu.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_entry")
data class PasswordEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val username: String,
    val password: String,
    val category: String,
    val createdAt: Long,
    val notes: String,
    val iconKey: String,
    val passwordHistory: String,
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
