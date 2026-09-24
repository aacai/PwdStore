package zhiqiu.app.data

expect object AppPaths {
    fun documentsDir(): String
}

object AppBootstrap {
    var documentsDir: String = ""
        private set

    fun init(documentsDir: String) {
        this.documentsDir = documentsDir
    }

    fun resolveDir(): String =
        documentsDir.ifBlank { AppPaths.documentsDir() }
}
