package zhiqiu.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import zhiqiu.app.data.AppBootstrap
import zhiqiu.app.data.AppSession
import zhiqiu.app.data.PasswordEntry
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.SecurityQuestions
import zhiqiu.app.db.DbProvider
import zhiqiu.app.i18n.Strings

/** 密保问题找回全链路：设置密保 → 忘记密码 → 回答问题 → 重置主密码 → 数据完好 */
class SecurityQuestionRecoveryTest {

    private fun openInTempDir(): Pair<File, PasswordRepository> {
        val dir = kotlin.io.path.createTempDirectory("pwdstore-security-test").toFile()
        AppBootstrap.init(dir.absolutePath)
        return dir to PasswordRepository()
    }

    private fun closeVault() {
        AppSession.session.lock()
        DbProvider.close()
    }

    @Test
    fun fullRecoveryChainKeepsVaultData() {
        val (dir, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        repo.add(
            PasswordEntry(
                id = "",
                title = "Bank",
                username = "alice@example.com",
                password = "p4ss-w0rd",
                notes = "2FA app",
                createdAt = 1_700_000_000_000L,
            ),
        )
        repo.writeSecurityQuestion("first_pet", "  MiLu  ")
        closeVault()

        // 模拟重启后从「忘记密码」入口进入
        AppBootstrap.init(dir.absolutePath)
        val reopened = PasswordRepository()

        // 答案错误：拿不到主密码，也不能解锁
        assertNull(reopened.recoverMasterPassword("wrong-answer"))
        assertFalse(AppSession.session.isUnlocked)

        // 答案正确（大小写与首尾空格不敏感）：恢复主密码并解锁
        val recovered = reopened.recoverMasterPassword("milu")
        assertEquals("mast3r-pass", recovered)
        assertTrue(AppSession.session.isUnlocked)

        // 重置主密码：旧数据必须仍能用新密码解密
        reopened.changeMasterPassword("mast3r-pass", "new-mast3r-pass")
        val entries = reopened.getAllDecrypted()
        assertEquals(1, entries.size)
        assertEquals("Bank", entries[0].title)
        assertEquals("alice@example.com", entries[0].username)
        assertEquals("p4ss-w0rd", entries[0].password)
        assertEquals("2FA app", entries[0].notes)

        // 新旧主密码的可用性
        assertFalse(reopened.verifyMasterPassword("mast3r-pass"))
        assertTrue(reopened.verifyMasterPassword("new-mast3r-pass"))
        closeVault()
    }

    @Test
    fun masterPasswordChangeInvalidatesSecurityQuestion() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        repo.writeSecurityQuestion("birth_city", "shanghai")
        assertTrue(repo.readSecurityQuestion() != null)

        repo.changeMasterPassword("mast3r-pass", "rotated-pass")

        assertNull(repo.readSecurityQuestion())
        assertNull(repo.verifySecurityAnswer("shanghai"))
        assertNull(repo.recoverMasterPassword("shanghai"))
        closeVault()
    }

    @Test
    fun clearingSecurityQuestionDisablesRecovery() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        repo.writeSecurityQuestion("favorite_food", "hotpot")
        repo.clearSecurityQuestion()

        assertNull(repo.readSecurityQuestion())
        assertNull(repo.recoverMasterPassword("hotpot"))
        closeVault()
    }

    @Test
    fun wrongAnswersTriggerCooldownAndBlockBruteForce() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        repo.writeSecurityQuestion("first_school", "no1-middle-school")
        assertEquals(PasswordRepository.SECURITY_MAX_FAILURES, repo.securityAttemptsLeft())

        repeat(PasswordRepository.SECURITY_MAX_FAILURES) { index ->
            assertNull(repo.verifySecurityAnswer("nope-$index"))
        }

        assertTrue(repo.securityLockRemainingMs() > 0)
        // 冷却期内即使答案正确也不放行，避免暴力枚举
        assertNull(repo.verifySecurityAnswer("no1-middle-school"))
        assertNull(repo.recoverMasterPassword("no1-middle-school"))
        closeVault()
    }

    @Test
    fun securityQuestionRequiresUnlockedVault() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        closeVault()

        assertFailsWith<IllegalArgumentException> {
            repo.writeSecurityQuestion("first_pet", "milu")
        }
    }

    @Test
    fun blankAnswerIsRejected() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        assertFailsWith<IllegalArgumentException> {
            repo.writeSecurityQuestion("first_pet", "   ")
        }
        closeVault()
    }

    @Test
    fun tooShortAnswerIsRejected() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        assertFailsWith<IllegalArgumentException> {
            repo.writeSecurityQuestion("first_pet", "a")
        }
        // 达到下限后即可写入
        repo.writeSecurityQuestion("first_pet", "mi")
        assertEquals("first_pet", repo.readSecurityQuestion()?.questionKey)
        closeVault()
    }

    @Test
    fun unknownQuestionKeyIsRejected() {
        val (_, repo) = openInTempDir()
        repo.setupMasterPassword("mast3r-pass")
        assertFailsWith<IllegalArgumentException> {
            repo.writeSecurityQuestion("not_a_preset", "answer")
        }
        closeVault()
    }

    @Test
    fun everyPresetQuestionIsLocalized() {
        val zh = Strings(true)
        val en = Strings(false)
        assertTrue(SecurityQuestions.presetKeys.size >= 6)
        SecurityQuestions.presetKeys.forEach { key ->
            assertNotEquals(key, zh.securityQuestionText(key), "缺少中文问题：$key")
            assertNotEquals(key, en.securityQuestionText(key), "缺少英文问题：$key")
        }
    }
}
