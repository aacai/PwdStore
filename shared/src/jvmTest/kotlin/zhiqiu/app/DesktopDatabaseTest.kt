package zhiqiu.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import zhiqiu.app.data.AppBootstrap
import zhiqiu.app.data.AppSession
import zhiqiu.app.data.PasswordEntry
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.db.DbProvider

/** 桌面端真实落库链路：Room + BundledSQLiteDriver + 加解密，打包裁剪运行时后靠它判定可用性 */
class DesktopDatabaseTest {

    private fun openInTempDir(): Pair<File, PasswordRepository> {
        val dir = kotlin.io.path.createTempDirectory("pwdstore-db-test").toFile()
        AppBootstrap.init(dir.absolutePath)
        return dir to PasswordRepository()
    }

    private fun closeVault() {
        AppSession.session.lock()
        DbProvider.close()
    }

    @Test
    fun entriesSurviveDatabaseReopen() {
        val (dir, repo) = openInTempDir()
        assertFalse(repo.hasMasterPassword())
        repo.setupMasterPassword("mast3r-pass")
        repo.add(
            PasswordEntry(
                id = "",
                title = "GitHub",
                username = "alice@example.com",
                password = "p4ss-w0rd",
                notes = "2FA app",
                createdAt = 1_700_000_000_000L,
            ),
        )
        assertTrue(File(dir, "pwdstore.db").isFile)
        closeVault()

        AppBootstrap.init(dir.absolutePath)
        val reopened = PasswordRepository()
        assertTrue(reopened.verifyMasterPassword("mast3r-pass"))
        val entries = reopened.getAllDecrypted()
        assertEquals(1, entries.size)
        assertEquals("GitHub", entries[0].title)
        assertEquals("alice@example.com", entries[0].username)
        assertEquals("p4ss-w0rd", entries[0].password)
        assertEquals("2FA app", entries[0].notes)
        closeVault()
    }

    @Test
    fun wrongMasterPasswordLeavesVaultLocked() {
        val (dir, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        closeVault()

        AppBootstrap.init(dir.absolutePath)
        val reopened = PasswordRepository()
        assertFalse(reopened.verifyMasterPassword("wrong-pass"))
        try {
            reopened.getAllDecrypted()
            fail("未解锁不应读到条目")
        } catch (_: IllegalStateException) {
        }
        closeVault()
    }

    @Test
    fun vaultFilesHoldOnlyCiphertextForSecretFields() {
        val (dir, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        repo.add(
            PasswordEntry(
                id = "",
                title = "Bank",
                username = "secret-user",
                password = "p4ss",
                notes = "secret-note",
                createdAt = 1_700_000_000_000L,
            ),
        )
        val raw = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("pwdstore.db") }
            ?.joinToString("\n") { String(it.readBytes(), Charsets.ISO_8859_1) }
            .orEmpty()
        assertFalse(raw.contains("p4ss"), "密码不应明文落盘")
        assertFalse(raw.contains("secret-user"), "账号不应明文落盘")
        assertFalse(raw.contains("secret-note"), "备注不应明文落盘")
        assertTrue(raw.contains("Bank"), "标题按明文保存，用于列表展示")
        closeVault()
    }
}
