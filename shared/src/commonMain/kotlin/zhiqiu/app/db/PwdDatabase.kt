package zhiqiu.app.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [PasswordEntryEntity::class, MetaEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(PwdDatabaseConstructor::class)
abstract class PwdDatabase : RoomDatabase() {
    abstract fun pwdDao(): PwdDao
}

@Suppress("KotlinNoActualForExpect")
expect object PwdDatabaseConstructor : RoomDatabaseConstructor<PwdDatabase> {
    override fun initialize(): PwdDatabase
}
