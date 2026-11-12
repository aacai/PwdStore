package zhiqiu.app.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.appendBytes
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.posix.memcpy
import zhiqiu.app.i18n.I18n
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

actual object MirrorUpload {
    actual fun upload(fileName: String, bytes: ByteArray): String = runBlocking {
        runCatching { post("https://x0.at/", fileName, bytes) }.getOrElse {
            runCatching { post("https://uguu.se/upload.php", fileName, bytes) }.getOrElse {
                error(I18n.strings.qrUploadFailed)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun post(endpoint: String, fileName: String, bytes: ByteArray): String =
        suspendCancellableCoroutine { cont ->
            val boundary = "----PwdStore${PlatformTime.nowMillis()}"
            val body = NSMutableData()
            fun appendAscii(s: String) {
                val b = s.encodeToByteArray()
                b.usePinned { pinned ->
                    body.appendBytes(pinned.addressOf(0), b.size.toULong())
                }
            }
            appendAscii("--$boundary\r\n")
            appendAscii("Content-Disposition: form-data; name=\"keep_name\"\r\n\r\n")
            appendAscii("1\r\n")
            appendAscii("--$boundary\r\n")
            appendAscii("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            appendAscii("Content-Type: application/octet-stream\r\n\r\n")
            bytes.usePinned { pinned ->
                body.appendBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            appendAscii("\r\n--$boundary--\r\n")

            val url = NSURL.URLWithString(endpoint)!!
            val request = NSMutableURLRequest.requestWithURL(url).apply {
                setHTTPMethod("POST")
                setValue("multipart/form-data; boundary=$boundary", forHTTPHeaderField = "Content-Type")
                setHTTPBody(body)
            }
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
                if (error != null) {
                    cont.resumeWithException(IllegalStateException(error.localizedDescription))
                    return@dataTaskWithRequest
                }
                val http = response as? NSURLResponse
                val text = data?.toKotlinByteArray()?.decodeToString().orEmpty()
                val link = text.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("http") }
                    ?: text.trim().takeIf { it.startsWith("http") }
                if (link == null) {
                    cont.resumeWithException(IllegalStateException(text.ifBlank { "empty response" }))
                } else {
                    cont.resume(link)
                }
                http // silence unused
            }
            cont.invokeOnCancellation { task.cancel() }
            task.resume()
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length <= 0) return ByteArray(0)
    val out = ByteArray(length)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return out
}
