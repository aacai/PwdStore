package zhiqiu.app.data

import zhiqiu.app.i18n.I18n

actual object BiometricAuth {
    actual fun hardwareSupported(): Boolean = false

    actual fun isAvailable(): Boolean = false

    actual fun authenticate(
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        onError(I18n.strings.biometricPlatformUnsupported)
    }
}
