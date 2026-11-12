package zhiqiu.app.data

/**
 * 把本地文件上传到临时镜像，拿到可扫码的下载链接。
 * 优先 x0.at，失败再试 uguu.se（与 CI 镜像策略一致）。
 */
expect object MirrorUpload {
    fun upload(fileName: String, bytes: ByteArray): String
}
