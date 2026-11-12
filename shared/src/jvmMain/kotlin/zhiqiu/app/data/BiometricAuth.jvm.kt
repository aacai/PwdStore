package zhiqiu.app.data

import com.ucasoft.koncierge.BiometricResults
import com.ucasoft.koncierge.Koncierge
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import zhiqiu.app.i18n.I18n

actual object BiometricAuth {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val koncierge by lazy { Koncierge() }

    private fun osName(): String = System.getProperty("os.name").orEmpty().lowercase()

    private fun isWindows(): Boolean = osName().startsWith("windows")
    private fun isMac(): Boolean = osName().startsWith("mac") || osName().startsWith("darwin")
    private fun isLinux(): Boolean = osName().startsWith("linux")

    actual fun hardwareSupported(): Boolean = isWindows() || isMac() || isLinux()

    actual fun isAvailable(): Boolean = runCatching { koncierge.isBiometricAvailable() }.getOrDefault(false)

    actual fun sensorKind(): BiometryKind = when {
        !hardwareSupported() -> BiometryKind.NONE
        isMac() -> BiometryKind.FINGERPRINT // Touch ID；系统也可能回落到登录密码
        isWindows() -> BiometryKind.OTHER // Windows Hello（指纹/面容/PIN）
        else -> BiometryKind.OTHER
    }

    actual fun authenticate(
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        if (!hardwareSupported()) {
            onError(I18n.strings.biometricPlatformUnsupported)
            return
        }
        AppLockController.suppressFor()
        val message = subtitle.ifBlank { title }.ifBlank { I18n.strings.biometricSetupSubtitle }
        scope.launch {
            val result = try {
                koncierge.authenticate(message, timeout = 2.minutes)
            } catch (t: Throwable) {
                BiometricResults.AuthenticationError(t.message ?: t::class.simpleName.orEmpty())
            }
            val s = I18n.strings
            SwingUtilities.invokeLater {
                when (result) {
                    BiometricResults.AuthenticationSuccessful -> onSuccess()
                    BiometricResults.AuthenticationCancelled -> onCancel()
                    BiometricResults.NotEnrolled -> onError(s.biometricEnrollHint)
                    BiometricResults.HardwareUnavailable -> onError(s.biometricNoHardware)
                    BiometricResults.FeatureUnavailable -> onError(s.biometricPlatformUnsupported)
                    BiometricResults.Locked -> onError(s.biometricLocked)
                    BiometricResults.AuthenticationFailed -> onError(s.biometricAuthFailed)
                    is BiometricResults.AuthenticationError -> onError(result.cause.ifBlank { s.biometricAuthFailed })
                    BiometricResults.Available -> onError(s.biometricAuthFailed)
                }
            }
        }
    }
}
