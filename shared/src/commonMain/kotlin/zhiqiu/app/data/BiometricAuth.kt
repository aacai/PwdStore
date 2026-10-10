package zhiqiu.app.data

expect object BiometricAuth {
    fun hardwareSupported(): Boolean
    fun isAvailable(): Boolean

    fun authenticate(
        title: String,
        subtitle: String,
        negativeButton: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {},
    )
}
