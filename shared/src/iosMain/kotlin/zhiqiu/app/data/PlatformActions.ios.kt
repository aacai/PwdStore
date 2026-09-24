package zhiqiu.app.data

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSPasteboard
import platform.Foundation.NSPasteboardTypeString
import platform.Foundation.NSString
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

actual object PlatformFormat {
    private val fmt = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd HH:mm"
    }

    actual fun formatDateTime(millis: Long): String {
        val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
        return fmt.stringFromDate(date)
    }
}

actual object PlatformActions {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun shareOrRevealFile(path: String, mimeType: String) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val vc = UIActivityViewController(activityItems = listOf(path), applicationActivities = null)
        root.presentViewController(vc, animated = true, completion = null)
    }

    actual fun pickFile(onResult: (ByteArray?) -> Unit) {
        onResult(null)
    }

    actual fun isDesktop(): Boolean = false
}
