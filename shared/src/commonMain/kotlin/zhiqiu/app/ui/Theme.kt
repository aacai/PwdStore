package zhiqiu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.data.ThemeMode
import zhiqiu.applewidgets.kmp.oink.theme.OinkAccent
import zhiqiu.applewidgets.kmp.oink.theme.OinkColors
import zhiqiu.applewidgets.kmp.oink.theme.OinkTextStyles
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme

// oink 令牌 → Material3 语义槽：既有 M3 组件直接吃 oink 的墨色/表面/强调色，不再走 MD3 默认紫
private fun oinkMaterialColors(c: OinkColors, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accentText,
        inversePrimary = c.accentDeep,
        secondary = c.accentDeep,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentTint,
        onSecondaryContainer = c.accentText,
        tertiary = c.info,
        onTertiary = c.onInverse,
        tertiaryContainer = c.infoSoft,
        onTertiaryContainer = c.infoText,
        background = c.bg,
        onBackground = c.text,
        surface = c.card,
        onSurface = c.text,
        surfaceVariant = c.well,
        onSurfaceVariant = c.text2,
        surfaceTint = c.accent,
        surfaceContainerLowest = c.bg,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.raised,
        surfaceContainerHighest = c.well,
        outline = c.line2,
        outlineVariant = c.line,
        error = c.danger,
        onError = c.onInverse,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.dangerText,
        inverseSurface = c.inverse,
        inverseOnSurface = c.onInverse,
        scrim = c.scrim,
    )
}

private fun oinkMaterialTypography(t: OinkTextStyles): Typography = Typography(
    headlineLarge = t.h1,
    headlineMedium = t.h2,
    headlineSmall = t.h3,
    titleLarge = t.h3,
    titleMedium = t.h4,
    titleSmall = t.label,
    bodyLarge = t.bodyLarge,
    bodyMedium = t.body,
    bodySmall = t.caption,
    labelLarge = t.label,
    labelMedium = t.caption,
    labelSmall = t.micro,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PwdTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    OinkTheme(darkTheme = dark, accent = OinkAccent.Pink) {
        MaterialTheme(
            colorScheme = oinkMaterialColors(OinkTheme.colors, dark),
            typography = oinkMaterialTypography(OinkTheme.textStyles),
            shapes = AppShapes,
            content = content,
        )
    }
    SideEffect { SystemBars.setAppearance(dark) }
}

// safeDrawing 已含状态栏、导航条、刘海与软键盘；Scaffold 用它做内容内边距，屏幕内容不要再叠 safeDrawingPadding
@Composable
fun screenWindowInsets(): WindowInsets = WindowInsets.safeDrawing

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(OinkTheme.colors.bg),
        content = { content() },
    )
}
