package zhiqiu.app.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object SecureDeviceKey {
    private const val KS_ALIAS = "pwdstore_device_wrap"
    private const val PREF = "pwdstore_secure"
    private const val KEY_WRAPPED = "wrapped_device_key"
    private const val KEY_IV = "wrapped_device_iv"

    actual fun getOrCreate(): ByteArray {
        val prefs = AndroidBridge.appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_WRAPPED, null)
        val iv = prefs.getString(KEY_IV, null)
        if (existing != null && iv != null) {
            return unwrap(Base64.decode(existing, Base64.DEFAULT), Base64.decode(iv, Base64.DEFAULT))
        }
        val raw = Crypto.randomBytes(32)
        val (wrapped, wrapIv) = wrap(raw)
        prefs.edit()
            .putString(KEY_WRAPPED, Base64.encodeToString(wrapped, Base64.DEFAULT))
            .putString(KEY_IV, Base64.encodeToString(wrapIv, Base64.DEFAULT))
            .apply()
        return raw
    }

    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(KS_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KS_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun wrap(raw: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        return cipher.doFinal(raw) to cipher.iv
    }

    private fun unwrap(wrapped: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(wrapped)
    }
}
