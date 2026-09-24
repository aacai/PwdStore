package zhiqiu.app.data

/**
 * 设备级密钥，用于加密「免密/指纹恢复用的主密码」。
 * 存在系统安全存储里，不进 SQLite，避免库文件被拷走后直接解出主密码。
 */
expect object SecureDeviceKey {
    fun getOrCreate(): ByteArray
}
