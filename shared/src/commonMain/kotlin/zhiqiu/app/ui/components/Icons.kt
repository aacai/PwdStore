package zhiqiu.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Apple
import compose.icons.fontawesomeicons.brands.Facebook
import compose.icons.fontawesomeicons.brands.Github
import compose.icons.fontawesomeicons.brands.Google
import compose.icons.fontawesomeicons.brands.Instagram
import compose.icons.fontawesomeicons.brands.Linkedin
import compose.icons.fontawesomeicons.brands.Microsoft
import compose.icons.fontawesomeicons.brands.Qq
import compose.icons.fontawesomeicons.brands.Twitter
import compose.icons.fontawesomeicons.brands.Weixin
import compose.icons.fontawesomeicons.brands.XTwitter
import compose.icons.fontawesomeicons.brands.Zhihu

object AppIcons {
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Add = Icons.Filled.Add
    val Settings = Icons.Filled.Settings
    val Theme = Icons.Filled.DarkMode
    val ThemeAuto = Icons.Filled.BrightnessAuto
    val Visibility = Icons.Filled.Visibility
    val VisibilityOff = Icons.Filled.VisibilityOff
    val Edit = Icons.Filled.Edit
    val Delete = Icons.Filled.Delete
    val Copy = Icons.Filled.ContentCopy
    val Refresh = Icons.Filled.Refresh
    val Search = Icons.Filled.Search
    val Lock = Icons.Filled.Lock
    val LockOpen = Icons.Filled.LockOpen
    val Password = Icons.Filled.Password
    val Key = Icons.Filled.Key
    val Apps = Icons.Filled.Apps
    val Export = Icons.Filled.FileUpload
    val Import = Icons.Filled.FileDownload
    val Backup = Icons.Filled.Backup
    val Folder = Icons.Filled.Folder
    val Info = Icons.Filled.Info
    val Palette = Icons.Filled.Palette
    val Fingerprint = Icons.Filled.Fingerprint
    val Login = Icons.AutoMirrored.Filled.Login
    val Logout = Icons.AutoMirrored.Filled.Logout

    fun forService(key: String): ImageVector = when (key) {
        "qq" -> FontAwesomeIcons.Brands.Qq
        "weixin" -> FontAwesomeIcons.Brands.Weixin
        "github" -> FontAwesomeIcons.Brands.Github
        "google" -> FontAwesomeIcons.Brands.Google
        "x" -> FontAwesomeIcons.Brands.XTwitter
        "twitter" -> FontAwesomeIcons.Brands.Twitter
        "zhihu" -> FontAwesomeIcons.Brands.Zhihu
        "facebook" -> FontAwesomeIcons.Brands.Facebook
        "instagram" -> FontAwesomeIcons.Brands.Instagram
        "linkedin" -> FontAwesomeIcons.Brands.Linkedin
        "apple" -> FontAwesomeIcons.Brands.Apple
        "microsoft" -> FontAwesomeIcons.Brands.Microsoft
        else -> Icons.Filled.Key
    }
}

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun PasswordVisibilityToggle(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        AppIcon(
            imageVector = if (visible) AppIcons.VisibilityOff else AppIcons.Visibility,
            contentDescription = if (visible) "隐藏密码" else "显示密码",
        )
    }
}

@Composable
fun ThemeToggleIcon() {
    AppIcon(AppIcons.Theme, "切换主题")
}

@Composable
fun IconGlyph(key: String, modifier: Modifier = Modifier) {
    AppIcon(
        imageVector = AppIcons.forService(key),
        contentDescription = key,
        modifier = modifier,
    )
}
