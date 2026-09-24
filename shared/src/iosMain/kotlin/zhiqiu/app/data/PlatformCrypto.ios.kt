package zhiqiu.app.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual object PlatformCrypto {
    actual fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        bytes.usePinned {
            val status = SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0))
            check(status == errSecSuccess) { "SecRandomCopyBytes failed" }
        }
        return bytes
    }

    actual fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray {
        val out = ByteArray(keyBytes)
        val status = password.usePinned { pwdPin ->
            salt.usePinned { saltPin ->
                out.usePinned { outPin ->
                    CCKeyDerivationPBKDF(
                        kCCPBKDF2,
                        pwdPin.addressOf(0),
                        password.size.convert(),
                        saltPin.addressOf(0),
                        salt.size.convert(),
                        kCCPRFHmacAlgSHA256,
                        iterations.toUInt(),
                        outPin.addressOf(0),
                        keyBytes.convert(),
                    )
                }
            }
        }
        check(status == 0) { "PBKDF2 failed: $status" }
        return out
    }

    actual fun aesCbcEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        crypt(kCCEncrypt, plain, key, iv)

    actual fun aesCbcDecrypt(cipher: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
        crypt(kCCDecrypt, cipher, key, iv)

    private fun crypt(op: UInt, input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val out = ByteArray(input.size + kCCBlockSizeAES128.toInt())
        val moved = ULongArray(1)
        val status = input.usePinned { inPin ->
            key.usePinned { keyPin ->
                iv.usePinned { ivPin ->
                    out.usePinned { outPin ->
                        moved.usePinned { movedPin ->
                            CCCrypt(
                                op,
                                kCCAlgorithmAES,
                                kCCOptionPKCS7Padding,
                                keyPin.addressOf(0),
                                kCCKeySizeAES256.convert(),
                                ivPin.addressOf(0),
                                inPin.addressOf(0),
                                input.size.convert(),
                                outPin.addressOf(0),
                                out.size.convert(),
                                movedPin.addressOf(0),
                            )
                        }
                    }
                }
            }
        }
        check(status == kCCSuccess) { "AES failed: $status" }
        return out.copyOf(moved[0].toInt())
    }

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(CC_SHA256_DIGEST_LENGTH)
        key.usePinned { keyPin ->
            data.usePinned { dataPin ->
                out.usePinned { outPin ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        keyPin.addressOf(0),
                        key.size.convert(),
                        dataPin.addressOf(0),
                        data.size.convert(),
                        outPin.addressOf(0),
                    )
                }
            }
        }
        return out
    }

    actual fun sha256(data: ByteArray): ByteArray {
        val out = ByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pin ->
            out.usePinned { outPin ->
                CC_SHA256(pin.addressOf(0), data.size.convert(), outPin.addressOf(0))
            }
        }
        return out
    }

    actual fun md5(data: ByteArray): ByteArray {
        val out = ByteArray(CC_MD5_DIGEST_LENGTH)
        data.usePinned { pin ->
            out.usePinned { outPin ->
                CC_MD5(pin.addressOf(0), data.size.convert(), outPin.addressOf(0))
            }
        }
        return out
    }

    actual fun encodeBase64(data: ByteArray): String {
        val nsData = data.usePinned {
            NSData.create(bytes = it.addressOf(0), length = data.size.convert())
        }
        return nsData.base64EncodedStringWithOptions(0u)
    }

    actual fun decodeBase64(text: String): ByteArray {
        val nsData = NSData.create(base64EncodedString = text, options = 0u)
            ?: error("invalid base64")
        val size = nsData.length.toInt()
        val out = ByteArray(size)
        out.usePinned { pin ->
            memcpy(pin.addressOf(0), nsData.bytes, size.convert())
        }
        return out
    }
}
