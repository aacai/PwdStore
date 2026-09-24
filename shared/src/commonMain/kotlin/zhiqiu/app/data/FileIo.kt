package zhiqiu.app.data

expect object FileIo {
    fun readText(path: String): String?
    fun writeText(path: String, content: String)
    fun readBytes(path: String): ByteArray?
    fun writeBytes(path: String, content: ByteArray)
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun join(dir: String, name: String): String
    fun listFiles(dir: String): List<String>
}
