package zhiqiu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import zhiqiu.app.data.ThemeMode
import zhiqiu.applewidgets.kmp.oink.theme.OinkAccent
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme

@Composable
fun PwdTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    OinkTheme(darkTheme = dark, accent = OinkAccent.Pink, content = content)
    SideEffect { SystemBars.setAppearance(dark) }
}

// 顶栏已自行处理状态栏安全区；这里只给内容区留导航条/手势区
@Composable
fun screenWindowInsets(): WindowInsets = WindowInsets.navigationBars

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(OinkTheme.colors.bg),
        content = { content() },
    )
}
