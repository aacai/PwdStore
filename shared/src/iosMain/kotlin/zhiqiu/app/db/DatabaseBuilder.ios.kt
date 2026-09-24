package zhiqiu.app.db

import androidx.room.Room
import androidx.room.RoomDatabase
import zhiqiu.app.data.AppBootstrap

actual fun createDatabaseBuilder(): RoomDatabase.Builder<PwdDatabase> {
    val dir = AppBootstrap.resolveDir()
    val dbFilePath = if (dir.endsWith("/")) "${dir}pwdstore.db" else "$dir/pwdstore.db"
    return Room.databaseBuilder<PwdDatabase>(
        name = dbFilePath,
    )
}
