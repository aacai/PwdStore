package zhiqiu.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import zhiqiu.app.i18n.I18n

/** 二维码内容过长时难以可靠扫码，约 2.5KB UTF-8 上限。 */
private const val QR_MAX_BYTES = 2500

@Composable
fun QrCodeDialog(
    title: String,
    payload: String?,
    loading: Boolean = false,
    error: String? = null,
    onCopy: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val s = I18n.strings
    val tooLong = !payload.isNullOrEmpty() && payload.encodeToByteArray().size > QR_MAX_BYTES
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(s.qrUploading)
                    }
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    tooLong -> Text(s.qrTooLong, color = MaterialTheme.colorScheme.error)
                    !payload.isNullOrEmpty() -> {
                        Image(
                            painter = rememberQrCodePainter(payload),
                            contentDescription = title,
                            modifier = Modifier
                                .size(220.dp)
                                .background(Color.White)
                                .padding(12.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        SelectionContainer {
                            Text(
                                payload,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!loading && error == null && !tooLong && !payload.isNullOrEmpty() && onCopy != null) {
                TextButton(onClick = onCopy) { Text(s.cdCopy) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.close) }
        },
    )
}
