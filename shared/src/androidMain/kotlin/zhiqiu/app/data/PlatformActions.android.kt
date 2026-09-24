package zhiqiu.app.data

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AndroidBridge {
    lateinit var appContext: Context
    var activity: Activity? = null
    var pendingPick: ((ByteArray?) -> Unit)? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

actual object PlatformFormat {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    actual fun formatDateTime(millis: Long): String = fmt.format(Date(millis))
}

actual object PlatformActions {
    const val REQ_PICK = 9101

    actual fun copyToClipboard(text: String) {
        val cm = AndroidBridge.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("pwdstore", text))
    }

    actual fun shareOrRevealFile(path: String, mimeType: String) {
        val context = AndroidBridge.appContext
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, "分享文件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    actual fun pickFile(onResult: (ByteArray?) -> Unit) {
        val act = AndroidBridge.activity
        if (act == null) {
            onResult(null)
            return
        }
        AndroidBridge.pendingPick = onResult
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        @Suppress("DEPRECATION")
        act.startActivityForResult(intent, REQ_PICK)
    }

    actual fun isDesktop(): Boolean = false

    fun onPickResult(uri: Uri?) {
        val cb = AndroidBridge.pendingPick
        AndroidBridge.pendingPick = null
        if (cb == null) return
        if (uri == null) {
            cb(null)
            return
        }
        val bytes = runCatching {
            AndroidBridge.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        cb(bytes)
    }
}
