package zhiqiu.app.data

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import zhiqiu.app.i18n.I18n

actual object MirrorUpload {
    actual fun upload(fileName: String, bytes: ByteArray): String {
        val primary = runCatching { postMultipart("https://x0.at/", fileName, bytes) }.getOrNull()
        if (!primary.isNullOrBlank() && primary.startsWith("http")) return primary.trim().lineSequence().first()
        val fallback = runCatching { postMultipart("https://uguu.se/upload.php", fileName, bytes) }.getOrNull()
        if (!fallback.isNullOrBlank() && fallback.startsWith("http")) return fallback.trim().lineSequence().first()
        error(I18n.strings.qrUploadFailed)
    }

    private fun postMultipart(endpoint: String, fileName: String, bytes: ByteArray): String {
        val boundary = "----PwdStore${PlatformTime.nowMillis()}"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        DataOutputStream(conn.outputStream).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"keep_name\"\r\n\r\n")
            out.writeBytes("1\r\n")
            out.writeBytes("--$boundary\r\n")
            out.writeBytes(
                "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n",
            )
            out.writeBytes("Content-Type: application/octet-stream\r\n\r\n")
            out.write(bytes)
            out.writeBytes("\r\n--$boundary--\r\n")
            out.flush()
        }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.use { it.readBytes() }
            ?.toString(StandardCharsets.UTF_8)
            .orEmpty()
        if (code !in 200..299) error("HTTP $code: ${body.take(200)}")
        return body.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("http") }
            ?: body.trim().takeIf { it.startsWith("http") }
            ?: error(body.ifBlank { "empty response" })
    }
}
