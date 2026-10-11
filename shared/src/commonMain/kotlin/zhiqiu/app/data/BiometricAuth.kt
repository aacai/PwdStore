package zhiqiu.app.data

enum class BiometryKind {
    NONE,
    FINGERPRINT,
    FACE,
    OTHER,
}

expect object BiometricAuth {
    fun hardwareSupported(): Boolean
    fun isAvailable(): Boolean

    fun sensorKind(): BiometryKind

    fun authenticate(
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {},
    )
}
