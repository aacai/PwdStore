package zhiqiu.app.data

/**
 * 切到后台后延迟锁定；选文件 / 生物识别期间可临时压制，避免误锁。
 */
object AppLockController {
    /** 后台停留超过该毫秒数后锁定（默认 60 秒，兼顾自动填充短窗）。 */
    const val BACKGROUND_LOCK_DELAY_MS = 60_000L

    private var suppressUntilMs: Long = 0L

    fun suppressFor(durationMs: Long = 120_000L) {
        val until = PlatformTime.nowMillis() + durationMs
        if (until > suppressUntilMs) suppressUntilMs = until
    }

    fun shouldLockNow(): Boolean = PlatformTime.nowMillis() >= suppressUntilMs
}
