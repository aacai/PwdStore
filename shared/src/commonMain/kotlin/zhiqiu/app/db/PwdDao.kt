package zhiqiu.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PwdDao {
    @Query("SELECT * FROM password_entry ORDER BY createdAt DESC")
    suspend fun getAllEntries(): List<PasswordEntryEntity>

    @Query("SELECT * FROM password_entry WHERE id = :id LIMIT 1")
    suspend fun getEntry(id: String): PasswordEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: PasswordEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<PasswordEntryEntity>)

    @Query("DELETE FROM password_entry WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("DELETE FROM password_entry")
    suspend fun deleteAllEntries()

    @Query("SELECT value FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: MetaEntity)

    @Query("DELETE FROM meta WHERE `key` = :key")
    suspend fun deleteMeta(key: String)

    @Query("DELETE FROM meta")
    suspend fun deleteAllMeta()

    @Transaction
    suspend fun clearAll() {
        deleteAllEntries()
        deleteAllMeta()
    }
}
