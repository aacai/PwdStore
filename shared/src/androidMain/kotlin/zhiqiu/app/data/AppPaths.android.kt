package zhiqiu.app.data

actual object AppPaths {
    actual fun dataDir(): String {
        val seeded = AppBootstrap.dataDir
        check(seeded.isNotBlank()) { "AppBootstrap.init 未调用" }
        return seeded
    }
}
