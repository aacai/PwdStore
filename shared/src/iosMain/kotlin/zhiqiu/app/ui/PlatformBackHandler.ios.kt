package zhiqiu.app.ui

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS：手势返回由系统/导航层处理；此处不拦截
}
