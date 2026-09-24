package zhiqiu.app.data

actual object AppPaths {
    actual fun documentsDir(): String {
        val seeded = AppBootstrap.documentsDir
        check(seeded.isNotBlank()) { "AppBootstrap.init 未调用" }
        return seeded
    }
}
