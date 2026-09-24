package zhiqiu.app.data

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual object PlatformFormat {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    actual fun formatDateTime(millis: Long): String = fmt.format(Date(millis))
}

actual object PlatformActions {
    actual fun copyToClipboard(text: String) {
        val sel = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, null)
    }

    actual fun shareOrRevealFile(path: String, mimeType: String) {
        val file = File(path)
        if (Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().open(file.parentFile) }
        }
    }

    actual fun pickFile(onResult: (ByteArray?) -> Unit) {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("备份文件", "json", "pwdbackup", "bin")
            }
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                onResult(chooser.selectedFile.readBytes())
            } else {
                onResult(null)
            }
        }
    }

    actual fun isDesktop(): Boolean = true
}
