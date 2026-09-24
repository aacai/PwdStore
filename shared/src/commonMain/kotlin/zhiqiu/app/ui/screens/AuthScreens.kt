package zhiqiu.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhiqiu.app.data.BiometricAuth
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.HeroLockBadge
import zhiqiu.app.ui.components.PasswordVisibilityToggle
import zhiqiu.app.ui.components.SoftCard
import zhiqiu.app.ui.components.ThemeToggleIcon

@Composable
fun SetupMasterPasswordScreen(
    repo: PasswordRepository,
    onDone: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun submit() {
        when {
            password.length < 4 -> scope.launch { snack.showSnackbar("主密码至少 4 位") }
            password != confirm -> scope.launch { snack.showSnackbar("两次输入不一致") }
            else -> {
                loading = true
                scope.launch {
                    val err = withContext(Dispatchers.Default) {
                        runCatching { repo.setupMasterPassword(password) }.exceptionOrNull()?.message
                    }
                    loading = false
                    if (err != null) snack.showSnackbar("设置失败: $err")
                    else onDone()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroLockBadge()
            Spacer(Modifier.height(20.dp))
            Text("设置主密码", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "主密码用于加密您的所有密码，请妥善保管",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("主密码") },
                    singleLine = true,
                    leadingIcon = { AppIcon(AppIcons.Lock, null) },
                    visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show1) { show1 = !show1 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("确认密码") },
                    singleLine = true,
                    leadingIcon = { AppIcon(AppIcons.Lock, null) },
                    visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show2) { show2 = !show2 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (loading) "处理中…" else "开始使用")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    repo: PasswordRepository,
    onUnlocked: () -> Unit,
    onReset: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bioEnabled = remember { repo.readAuthConfig().biometricEnabled }
    val bioAvailable = remember { BiometricAuth.isAvailable() }

    fun unlockOk() {
        repo.onManualUnlockSuccess()
        onUnlocked()
    }

    fun submit() {
        if (password.isBlank()) {
            scope.launch { snack.showSnackbar("请输入密码") }
            return
        }
        loading = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { repo.verifyMasterPassword(password) }
            loading = false
            if (ok) unlockOk() else snack.showSnackbar("密码错误")
        }
    }

    fun runBiometric() {
        BiometricAuth.authenticate(
            title = "解锁 PwdStore",
            subtitle = "使用指纹或面容验证身份",
            onSuccess = {
                scope.launch {
                    val ok = withContext(Dispatchers.Default) { repo.tryBiometricUnlock() }
                    if (ok) {
                        unlockOk()
                    } else {
                        snack.showSnackbar("生物识别成功，但本地凭证不可用，请输入主密码")
                    }
                }
            },
            onError = { msg -> scope.launch { snack.showSnackbar(msg) } },
        )
    }

    LaunchedEffect(Unit) {
        if (bioEnabled && bioAvailable) runBiometric()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("解锁") },
                actions = { IconButton(onClick = onToggleTheme) { ThemeToggleIcon() } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroLockBadge()
            Spacer(Modifier.height(16.dp))
            Text("欢迎回来", style = MaterialTheme.typography.headlineSmall)
            Text(
                "输入主密码继续",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("主密码") },
                    singleLine = true,
                    leadingIcon = { AppIcon(AppIcons.Lock, null) },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show) { show = !show } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("解锁") }
                if (bioEnabled && bioAvailable) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { runBiometric() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        AppIcon(AppIcons.Fingerprint, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("指纹 / 面容解锁")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmReset = 1 }) { Text("忘记密码？") }
        }
    }

    if (confirmReset == 1) {
        ConfirmDialog(
            title = "重置主密码？",
            text = "重置会删除所有密码数据，不可恢复。若只想改主密码，请登录后到设置页修改。",
            onDismiss = { confirmReset = 0 },
            onConfirm = { confirmReset = 2 },
        )
    }
    if (confirmReset == 2) {
        ConfirmDialog(
            title = "再次确认",
            text = "此操作无法撤销，确定删除全部数据？",
            onDismiss = { confirmReset = 0 },
            onConfirm = {
                repo.resetAll()
                confirmReset = 0
                onReset()
            },
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = MaterialTheme.shapes.large,
    )
}
