package zhiqiu.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import zhiqiu.app.data.BackupManager
import zhiqiu.app.data.PasswordCategory
import zhiqiu.app.data.PasswordEntry
import zhiqiu.app.data.PasswordHistoryItem

class BackupRoundtripTest {

    @Test
    fun encryptedBackupRoundtripKeepsSensitiveFields() {
        val entries = listOf(
            PasswordEntry(
                id = "a1",
                title = "GitHub",
                username = "alice@example.com",
                password = "s3cret!",
                category = PasswordCategory.WORK.name,
                createdAt = 1_700_000_000_000L,
                notes = "2FA app",
                passwordHistory = listOf(
                    PasswordHistoryItem(password = "old-pass", changedAt = 1_690_000_000_000L),
                ),
                iconKey = "github",
            ),
        )
        val bytes = BackupManager.createEncryptedBackup(entries, "master-pass")
        val parsed = BackupManager.parseBackup(bytes, "master-pass")
        assertEquals(1, parsed.entryCount)
        assertEquals(entries[0].title, parsed.entries[0].title)
        assertEquals(entries[0].username, parsed.entries[0].username)
        assertEquals(entries[0].password, parsed.entries[0].password)
        assertEquals(entries[0].notes, parsed.entries[0].notes)
        assertEquals(entries[0].iconKey, parsed.entries[0].iconKey)
        assertEquals(entries[0].passwordHistory, parsed.entries[0].passwordHistory)
    }

    @Test
    fun wrongPasswordFails() {
        val entries = listOf(
            PasswordEntry(
                id = "b1",
                title = "X",
                username = "u",
                password = "p",
                createdAt = 1L,
            ),
        )
        val bytes = BackupManager.createEncryptedBackup(entries, "right")
        val err = assertFailsWith<IllegalStateException> {
            BackupManager.parseBackup(bytes, "wrong")
        }
        assertTrue(err.message!!.contains("密码错误") || err.message!!.contains("损坏"))
    }

    @Test
    fun exportTxtMasksPassword() {
        val text = BackupManager.exportTxt(
            listOf(
                PasswordEntry(
                    id = "c1",
                    title = "Mail",
                    username = "me",
                    password = "visible-should-not-appear",
                    createdAt = 1L,
                ),
            ),
        )
        assertTrue(text.contains("********"))
        assertTrue(!text.contains("visible-should-not-appear"))
        assertTrue(BackupManager.exportTxtFileName(42).endsWith("42.txt"))
        assertTrue(BackupManager.backupFileName(42).endsWith("42.json"))
    }
}
