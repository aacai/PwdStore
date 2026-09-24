package zhiqiu.app.data

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformTime {
    actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
