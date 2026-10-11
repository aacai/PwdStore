package zhiqiu.app.data

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import zhiqiu.app.i18n.I18n

actual object BiometricAuth {
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK

    private fun context(): android.content.Context? = runCatching { AndroidBridge.appContext }.getOrNull()

    private fun canAuthenticate(): Int {
        val ctx = context() ?: return BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        return BiometricManager.from(ctx).canAuthenticate(AUTHENTICATORS)
    }

    actual fun hardwareSupported(): Boolean {
        val result = canAuthenticate()
        return result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
            result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    actual fun isAvailable(): Boolean = canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

    actual fun sensorKind(): BiometryKind {
        val ctx = context() ?: return BiometryKind.NONE
        if (!hardwareSupported()) return BiometryKind.NONE
        val pm = ctx.packageManager
        val face = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
        val finger = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        return when {
            finger -> BiometryKind.FINGERPRINT
            face -> BiometryKind.FACE
            else -> BiometryKind.OTHER
        }
    }

    actual fun authenticate(
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        val activity = AndroidBridge.activity as? FragmentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onError(I18n.strings.biometricNotStarted)
            return
        }
        if (!isAvailable()) {
            onError(I18n.strings.biometricUnavailable)
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    when {
                        cancelled -> onCancel()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() = Unit
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButton)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
