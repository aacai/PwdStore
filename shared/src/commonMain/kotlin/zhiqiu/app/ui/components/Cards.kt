package zhiqiu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import zhiqiu.applewidgets.kmp.basic.Separator
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.applewidgets.kmp.basic.Surface
import zhiqiu.applewidgets.kmp.basic.SurfaceDefaults
import zhiqiu.applewidgets.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = SurfaceDefaults.surfaceColors(containerColor = OinkTheme.colors.card),
        content = { Column(Modifier.padding(16.dp), content = content) },
    )
}

@Composable
fun EntryListCard(
    title: String,
    username: String,
    hasNotes: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    SoftCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OinkTheme.colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = OinkTheme.textStyles.h4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    username,
                    style = OinkTheme.textStyles.body,
                    color = OinkTheme.colors.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hasNotes) {
                AppIcon(
                    AppIcons.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = OinkTheme.colors.text2,
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = OinkTheme.textStyles.label,
        color = OinkTheme.colors.accent,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun HeroLockBadge(
    icon: ImageVector = AppIcons.Lock,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .size(88.dp)
        .clip(CircleShape)
        .background(OinkTheme.colors.accentSoft)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
            tint = OinkTheme.colors.accent,
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = OinkTheme.textStyles.label,
            color = OinkTheme.colors.accent,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OinkTheme.colors.line, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.surfaceColors(containerColor = OinkTheme.colors.card),
        ) {
            Column(Modifier.padding(vertical = 2.dp), content = content)
        }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(OinkTheme.colors.accentSoft.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon, null, Modifier.size(17.dp), tint = OinkTheme.colors.accent)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = OinkTheme.textStyles.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = OinkTheme.textStyles.caption,
                        color = OinkTheme.colors.text2,
                    )
                }
            }
            trailing?.invoke()
        }
        extra?.invoke()
        if (divider) {
            Separator(
                startIndent = 62.dp,
                color = OinkTheme.colors.line.copy(alpha = 0.5f),
            )
        }
    }
}
