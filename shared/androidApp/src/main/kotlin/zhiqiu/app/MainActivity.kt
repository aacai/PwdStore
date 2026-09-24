package zhiqiu.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import zhiqiu.app.data.AndroidBridge
import zhiqiu.app.data.AppBootstrap
import zhiqiu.app.data.PlatformActions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppBootstrap.init(filesDir.absolutePath)
        AndroidBridge.init(this)
        AndroidBridge.activity = this
        setContent { App() }
    }

    override fun onDestroy() {
        if (AndroidBridge.activity === this) AndroidBridge.activity = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PlatformActions.REQ_PICK) {
            PlatformActions.onPickResult(if (resultCode == RESULT_OK) data?.data else null)
        }
    }
}
