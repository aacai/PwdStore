package zhiqiu.app.db

import androidx.room.Room
import androidx.room.RoomDatabase
import zhiqiu.app.data.AppBootstrap
import java.io.File

actual fun createDatabaseBuilder(): RoomDatabase.Builder<PwdDatabase> {
    val dir = File(AppBootstrap.resolveDir()).also { it.mkdirs() }
    val dbFile = File(dir, "pwdstore.db")
    return Room.databaseBuilder<PwdDatabase>(
        name = dbFile.absolutePath,
    )
}
