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
import zhiqiu.app.i18n.I18n

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
        val s = I18n.strings
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                dialogTitle = s.fileChooserTitle
                fileFilter = FileNameExtensionFilter(s.fileFilterBackup, "json")
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
