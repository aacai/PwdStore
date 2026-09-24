package zhiqiu.app.db

import androidx.room.RoomDatabase

expect fun createDatabaseBuilder(): RoomDatabase.Builder<PwdDatabase>
