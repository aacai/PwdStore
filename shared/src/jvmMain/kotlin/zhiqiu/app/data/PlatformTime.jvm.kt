package zhiqiu.app.data

actual object PlatformTime {
    actual fun nowMillis(): Long = System.currentTimeMillis()
}
