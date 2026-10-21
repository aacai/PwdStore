package zhiqiu.app.ui

import androidx.core.view.WindowInsetsControllerCompat
import zhiqiu.app.data.AndroidBridge

actual object SystemBars {
    actual fun setAppearance(dark: Boolean) {
        val window = AndroidBridge.activity?.window ?: return
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
