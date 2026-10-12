package zhiqiu.app.data

expect object AppPaths {
    /** 应用私有数据目录：读写数据库、备份、导出文件都落在这里 */
    fun dataDir(): String
}

object AppBootstrap {
    var dataDir: String = ""
        private set

    fun init(dataDir: String) {
        this.dataDir = dataDir
    }

    fun resolveDir(): String =
        dataDir.ifBlank { AppPaths.dataDir() }
}
