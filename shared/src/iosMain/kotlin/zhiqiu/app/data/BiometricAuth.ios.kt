package zhiqiu.app.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeNone
import platform.LocalAuthentication.LABiometryTypeTouchID
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import zhiqiu.app.i18n.I18n

@OptIn(ExperimentalForeignApi::class)
actual object BiometricAuth {
    private val policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics
    private var active: LAContext? = null

    actual fun hardwareSupported(): Boolean = LAContext().biometryType != LABiometryTypeNone

    actual fun isAvailable(): Boolean {
        val ctx = LAContext()
        return ctx.canEvaluatePolicy(policy, error = null) &&
            ctx.biometryType in setOf(LABiometryTypeTouchID, LABiometryTypeFaceID)
    }

    actual fun sensorKind(): BiometryKind = when (LAContext().biometryType) {
        LABiometryTypeTouchID -> BiometryKind.FINGERPRINT
        LABiometryTypeFaceID -> BiometryKind.FACE
        LABiometryTypeNone -> BiometryKind.NONE
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
        val s = I18n.strings
        val ctx = LAContext()
        if (!ctx.canEvaluatePolicy(policy, error = null)) {
            onError(s.biometricUnavailable)
            return
        }
        active = ctx
        ctx.evaluatePolicy(
            policy,
            localizedReason = subtitle.ifEmpty { title },
            reply = { success, error ->
                val message = error?.localizedDescription
                val cancelled = error?.code?.toLong() == LAErrorUserCancel ||
                    error?.code?.toLong() == LAErrorSystemCancel
                active = null
                dispatch_async(dispatch_get_main_queue()) {
                    when {
                        success -> onSuccess()
                        cancelled -> onCancel()
                        message != null -> onError(message)
                        else -> onError(s.biometricNotStarted)
                    }
                }
            },
        )
    }
}
