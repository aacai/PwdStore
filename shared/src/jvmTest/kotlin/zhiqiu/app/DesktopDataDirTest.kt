package zhiqiu.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zhiqiu.app.data.desktopDataRoot
import zhiqiu.app.data.migrateLegacyDataDir

class DesktopDataDirTest {

    private val home = File("/home/tester")

    private fun envOf(vararg pairs: Pair<String, String>): (String) -> String? =
        { key -> pairs.firstOrNull { it.first == key }?.second }

    @Test
    fun macOSUsesApplicationSupportAndIgnoresEnv() {
        val root = desktopDataRoot("Mac OS X", home, envOf("APPDATA" to "C:\\wrong"))
        assertEquals(File(home, "Library/Application Support").path, root.path)
    }

    @Test
    fun windowsPrefersAppData() {
        val withEnv = desktopDataRoot("Windows 11", home, envOf("APPDATA" to "/roaming"))
        assertEquals(File("/roaming").path, withEnv.path)

        val fallback = desktopDataRoot("Windows 11", home, envOf("APPDATA" to " "))
        assertEquals(File(home, "AppData/Roaming").path, fallback.path)
    }

    @Test
    fun linuxPrefersXdgDataHome() {
        val withEnv = desktopDataRoot("Linux", home, envOf("XDG_DATA_HOME" to "/xdg"))
        assertEquals(File("/xdg").path, withEnv.path)

        val fallback = desktopDataRoot("Linux", home, envOf())
        assertEquals(File(home, ".local/share").path, fallback.path)
    }

    @Test
    fun legacyDocumentsDataIsCopiedOnceIntoPrivateDir() {
        val tmp = java.nio.file.Files.createTempDirectory("pwdstore").toFile()
        try {
            val legacy = File(tmp, "Documents/PwdStore").apply { mkdirs() }
            File(legacy, "pwdstore.db").writeText("db")
            File(legacy, "master.json").writeText("master")
            File(legacy, "notes.txt").writeText("notes")
            val target = File(tmp, "Library/Application Support/PwdStore")

            assertTrue(migrateLegacyDataDir(target, tmp))
            assertEquals("db", File(target, "pwdstore.db").readText())
            assertEquals("master", File(target, "master.json").readText())
            assertEquals("notes", File(target, "notes.txt").readText())
            assertTrue(File(legacy, "pwdstore.db").isFile)

            assertFalse(migrateLegacyDataDir(target, tmp))
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun migrationSkippedWhenNothingToMove() {
        val tmp = java.nio.file.Files.createTempDirectory("pwdstore").toFile()
        try {
            val target = File(tmp, "Library/Application Support/PwdStore")
            assertFalse(migrateLegacyDataDir(target, tmp))
            assertFalse(target.exists())

            val legacy = File(tmp, "Documents/PwdStore").apply { mkdirs() }
            File(legacy, "stray.txt").writeText("x")
            assertFalse(migrateLegacyDataDir(target, tmp))
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun existingDatabaseWinsOverLegacyCopy() {
        val tmp = java.nio.file.Files.createTempDirectory("pwdstore").toFile()
        try {
            val legacy = File(tmp, "文档/PwdStore").apply { mkdirs() }
            File(legacy, "pwdstore.db").writeText("old")
            val target = File(tmp, ".local/share/PwdStore").apply { mkdirs() }
            File(target, "pwdstore.db").writeText("current")

            assertFalse(migrateLegacyDataDir(target, tmp))
            assertEquals("current", File(target, "pwdstore.db").readText())
        } finally {
            tmp.deleteRecursively()
        }
    }
}
