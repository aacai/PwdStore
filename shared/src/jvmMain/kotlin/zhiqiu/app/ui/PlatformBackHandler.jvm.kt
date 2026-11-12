package zhiqiu.app.ui

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop：系统返回键不适用；窗口关闭由系统处理
}
