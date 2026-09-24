package zhiqiu.app.data

actual object BiometricAuth {
    actual fun isAvailable(): Boolean = false

    actual fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        onError("当前平台不支持生物识别")
    }
}
