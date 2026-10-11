package zhiqiu.app

import kotlin.test.Test
import kotlin.test.assertEquals
import zhiqiu.app.data.BiometryKind
import zhiqiu.app.i18n.AppLanguage
import zhiqiu.app.i18n.I18n

class SharedCommonTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun biometricLabelsFollowSensorAndLanguage() {
        try {
            I18n.language = AppLanguage.ZH
            val zh = I18n.strings
            assertEquals("使用指纹解锁", zh.biometricUnlockFor(BiometryKind.FINGERPRINT))
            assertEquals("用面容更快解锁", zh.biometricInviteTitle(BiometryKind.FACE))
            assertEquals(
                "验证生物识别即可进入，无需每次输入主密码",
                zh.biometricInviteHint(BiometryKind.OTHER),
            )
            I18n.language = AppLanguage.EN
            val en = I18n.strings
            assertEquals("Unlock with Face ID", en.biometricUnlockFor(BiometryKind.FACE))
            assertEquals("Unlock faster with Fingerprint", en.biometricInviteTitle(BiometryKind.FINGERPRINT))
        } finally {
            I18n.language = AppLanguage.SYSTEM
        }
    }
}
