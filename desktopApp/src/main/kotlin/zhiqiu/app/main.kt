package zhiqiu.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import zhiqiu.app.data.AppBootstrap
import zhiqiu.app.data.AppPaths

fun main() = application {
    AppBootstrap.init(AppPaths.documentsDir())
    Window(
        onCloseRequest = ::exitApplication,
        title = "PwdStore",
    ) {
        App()
    }
}
