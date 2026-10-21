package zhiqiu.app.ui

import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle

actual object SystemBars {
    actual fun setAppearance(dark: Boolean) {
        UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle =
            if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }
}
