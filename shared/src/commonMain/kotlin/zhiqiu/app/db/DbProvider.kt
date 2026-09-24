package zhiqiu.app.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import zhiqiu.app.data.AppBootstrap

object DbProvider {
    private var instance: PwdDatabase? = null

    fun get(): PwdDatabase {
        instance?.let { return it }
        // ensure data dir exists before opening db
        AppBootstrap.resolveDir()
        val db = createDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        instance = db
        return db
    }

    fun dao(): PwdDao = get().pwdDao()

    fun close() {
        instance?.close()
        instance = null
    }
}
