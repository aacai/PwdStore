package zhiqiu.app.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

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
    private var activePickerDelegate: NSObject? = null

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun shareOrRevealFile(path: String, mimeType: String) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val url = NSURL.fileURLWithPath(path)
        val vc = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
        root.presentViewController(vc, animated = true, completion = null)
    }

    actual fun pickFile(onResult: (ByteArray?) -> Unit) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (root == null) {
            onResult(null)
            return
        }
        AppLockController.suppressFor()
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeItem),
            asCopy = true,
        )
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                activePickerDelegate = null
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val bytes = url?.let { readUrlBytes(it) }
                dispatch_async(dispatch_get_main_queue()) { onResult(bytes) }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                activePickerDelegate = null
                dispatch_async(dispatch_get_main_queue()) { onResult(null) }
            }
        }
        activePickerDelegate = delegate
        picker.delegate = delegate
        root.presentViewController(picker, animated = true, completion = null)
    }

    actual fun isDesktop(): Boolean = false
}

@OptIn(ExperimentalForeignApi::class)
private fun readUrlBytes(url: NSURL): ByteArray? {
    val data = NSData.dataWithContentsOfURL(url) ?: return null
    val length = data.length.toInt()
    if (length <= 0) return ByteArray(0)
    val out = ByteArray(length)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return out
}
