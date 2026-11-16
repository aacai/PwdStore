package zhiqiu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import zhiqiu.applewidgets.kmp.basic.Button as AppleButton
import zhiqiu.applewidgets.kmp.basic.ButtonStyle
import zhiqiu.applewidgets.kmp.basic.Separator
import zhiqiu.applewidgets.kmp.basic.Snackbar
import zhiqiu.applewidgets.kmp.basic.Surface
import zhiqiu.applewidgets.kmp.basic.SurfaceDefaults
import zhiqiu.applewidgets.kmp.oink.form.OinkTextField
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.applewidgets.kmp.theme.LocalContentColor

/** 轻量提示状态：替代 MD3 SnackbarHostState，无协程 API。 */
class AppSnackbarHostState {
    var currentMessage by mutableStateOf<String?>(null)
        private set

    fun showSnackbar(message: String) {
        currentMessage = message
    }

    fun dismiss() {
        currentMessage = null
    }
}

@Composable
fun rememberAppSnackbarHostState(): AppSnackbarHostState = remember { AppSnackbarHostState() }

@Composable
fun AppSnackbarHost(state: AppSnackbarHostState, modifier: Modifier = Modifier) {
    val message = state.currentMessage ?: return
    LaunchedEffect(message) {
        delay(2400L)
        state.dismiss()
    }
    Snackbar(message = message, modifier = modifier.padding(16.dp))
}

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: androidx.compose.foundation.layout.WindowInsets =
        androidx.compose.foundation.layout.WindowInsets.navigationBars,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(Modifier.fillMaxSize().then(modifier)) {
        Column(Modifier.fillMaxSize()) {
            topBar()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(contentWindowInsets),
            ) { content(PaddingValues(0.dp)) }
        }
        Box(Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { floatingActionButton() }
    }
}

data class AppTopBarColors(val containerColor: Color)

object AppTopBarDefaults {
    @Composable
    fun topAppBarColors(containerColor: Color = OinkTheme.colors.bg): AppTopBarColors =
        AppTopBarColors(containerColor)
}

@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: AppTopBarColors = AppTopBarDefaults.topAppBarColors(),
) {
    Column(modifier.fillMaxWidth().background(colors.containerColor).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { navigationIcon() }
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { title() }
            Row(content = actions)
        }
        Separator(startIndent = 0.dp)
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable () -> Unit,
) {
    AppleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = ButtonStyle.Plain,
        shape = shape,
        insideMargin = contentPadding ?: zhiqiu.applewidgets.kmp.basic.ButtonDefaults.InsideMargin,
        content = { content() },
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    AppleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = ButtonStyle.Filled,
        shape = shape,
        insideMargin = contentPadding ?: zhiqiu.applewidgets.kmp.basic.ButtonDefaults.InsideMargin,
        content = content,
    )
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    AppleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = ButtonStyle.Tinted,
        shape = shape,
        insideMargin = contentPadding ?: zhiqiu.applewidgets.kmp.basic.ButtonDefaults.InsideMargin,
        content = content,
    )
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.padding(24.dp),
            shape = shape,
            colors = SurfaceDefaults.surfaceColors(containerColor = OinkTheme.colors.card),
        ) {
            Column(Modifier.padding(24.dp)) {
                title()
                Spacer(Modifier.height(12.dp))
                text()
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(Modifier.size(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Column(modifier) {
        if (label != null) {
            CompositionLocalProvider(LocalContentColor provides OinkTheme.colors.text2) {
                Box(Modifier.padding(bottom = 4.dp)) { label() }
            }
        }
        OinkTextField(
            value = value,
            onValueChange = if (readOnly) { _ -> } else onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailing = trailingIcon,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
        if (supportingText != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) OinkTheme.colors.danger else OinkTheme.colors.text2,
            ) {
                Box(Modifier.padding(top = 4.dp)) { supportingText() }
            }
        }
    }
}
