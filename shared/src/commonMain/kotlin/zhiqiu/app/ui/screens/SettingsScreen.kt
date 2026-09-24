package zhiqiu.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhiqiu.app.data.AppSession
import zhiqiu.app.data.BiometricAuth
import zhiqiu.app.data.BackupManager
import zhiqiu.app.data.BackupPayload
import zhiqiu.app.data.FileIo
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PlatformActions
import zhiqiu.app.data.StoreFiles
import zhiqiu.app.data.ThemeMode
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.PasswordVisibilityToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: PasswordRepository,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onLocked: () -> Unit,
    onListChanged: () -> Unit,
) {
    var auth by remember { mutableStateOf(repo.readAuthConfig()) }
    var themeDialog by remember { mutableStateOf(false) }
    var changePwd by remember { mutableStateOf(false) }
    var exportPreview by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<BackupPayload?>(null) }
    var importAskPwd by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var storageDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { AppIcon(AppIcons.Back, "返回") } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("安全设置")
            ListItem(
                leadingContent = { AppIcon(AppIcons.Password, null) },
                headlineContent = { Text("修改主密码") },
                modifier = Modifier.clickable { changePwd = true },
            )
            ListItem(
                leadingContent = { AppIcon(AppIcons.LockOpen, null) },
                headlineContent = { Text("免密登录") },
                supportingContent = {
                    Text("时长 ${(auth.duration / 24).let { d -> if (d == d.toInt().toDouble()) "${d.toInt()}" else "$d" }} 天")
                },
                trailingContent = {
                    Switch(
                        checked = auth.enabled,
                        onCheckedChange = {
                            auth = auth.copy(enabled = it)
                            repo.writeAuthConfig(auth)
                        },
                    )
                },
            )
            if (auth.enabled) {
                Text("免密时长（天）", Modifier.padding(horizontal = 16.dp))
                Slider(
                    value = (auth.duration / 24).toFloat().coerceIn(0.5f, 7f),
                    onValueChange = {
                        val days = (it * 2).toInt() / 2f
                        auth = auth.copy(duration = days * 24.0)
                        repo.writeAuthConfig(auth)
                    },
                    valueRange = 0.5f..7f,
                    steps = 12,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            ListItem(
                leadingContent = { AppIcon(AppIcons.Fingerprint, null) },
                headlineContent = { Text("生物识别解锁") },
                supportingContent = {
                    Text(
                        when {
                            !BiometricAuth.isAvailable() -> "当前设备不支持或未录入指纹/面容"
                            auth.biometricEnabled -> "已开启，解锁页可使用指纹/面容"
                            else -> "开启后可用指纹或面容快速解锁"
                        }
                    )
                },
                trailingContent = {
                    Switch(
                        checked = auth.biometricEnabled,
                        enabled = BiometricAuth.isAvailable(),
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                BiometricAuth.authenticate(
                                    title = "确认开启生物识别",
                                    subtitle = "验证后即可用指纹/面容解锁",
                                    onSuccess = {
                                        auth = auth.copy(biometricEnabled = true)
                                        repo.writeAuthConfig(auth)
                                    },
                                    onError = { msg -> scope.launch { snack.showSnackbar(msg) } },
                                )
                            } else {
                                auth = auth.copy(biometricEnabled = false)
                                repo.writeAuthConfig(auth)
                            }
                        },
                    )
                },
            )
            ListItem(
                leadingContent = { AppIcon(AppIcons.Logout, null) },
                headlineContent = { Text("立即锁定") },
                modifier = Modifier.clickable {
                    repo.lockNow()
                    onLocked()
                },
            )
            HorizontalDivider()
            SectionTitle("显示设置")
            ListItem(
                leadingContent = { AppIcon(AppIcons.Palette, null) },
                headlineContent = { Text("主题") },
                trailingContent = { Text(themeMode.label) },
                modifier = Modifier.clickable { themeDialog = true },
            )
            HorizontalDivider()
            SectionTitle("数据管理")
            ListItem(
                leadingContent = { AppIcon(AppIcons.Export, null) },
                headlineContent = { Text("导出数据（TXT）") },
                modifier = Modifier.clickable {
                    val list = repo.getAllDecrypted()
                    if (list.isEmpty()) scope.launch { snack.showSnackbar("暂无密码数据可导出") }
                    else exportPreview = BackupManager.exportTxt(list)
                },
            )
            ListItem(
                leadingContent = { AppIcon(AppIcons.Backup, null) },
                headlineContent = { Text("加密备份") },
                modifier = Modifier.clickable {
                    val pwd = AppSession.session.masterPassword
                    if (pwd == null) {
                        scope.launch { snack.showSnackbar("请先设置主密码") }
                        return@clickable
                    }
                    scope.launch {
                        runCatching {
                            val bytes = withContext(Dispatchers.Default) {
                                BackupManager.createEncryptedBackup(repo.getAllDecrypted(), pwd)
                            }
                            val name = BackupManager.backupFileName()
                            val path = FileIo.join(repo.documentsPath(), name)
                            FileIo.writeBytes(path, bytes)
                            PlatformActions.shareOrRevealFile(path, "application/json")
                            snack.showSnackbar("备份已保存: $name")
                        }.onFailure { snack.showSnackbar(it.message ?: "备份失败") }
                    }
                },
            )
            ListItem(
                leadingContent = { AppIcon(AppIcons.Import, null) },
                headlineContent = { Text("导入数据") },
                modifier = Modifier.clickable {
                    PlatformActions.pickFile { bytes ->
                        if (bytes == null) {
                            scope.launch { snack.showSnackbar("未选择文件") }
                        } else {
                            pendingImportBytes = bytes
                            importAskPwd = true
                        }
                    }
                },
            )
            ListItem(
                leadingContent = { AppIcon(AppIcons.Folder, null) },
                headlineContent = { Text("存储详情") },
                modifier = Modifier.clickable { storageDialog = true },
            )
            HorizontalDivider()
            SectionTitle("关于")
            ListItem(
                leadingContent = { AppIcon(AppIcons.Info, null) },
                headlineContent = { Text("关于应用") },
                modifier = Modifier.clickable { aboutDialog = true },
            )
        }
    }

    if (themeDialog) {
        var temp by remember(themeDialog) { mutableStateOf(themeMode) }
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("主题") },
            text = {
                Column {
                    ThemeMode.entries.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { temp = m }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = temp == m, onClick = { temp = m })
                            Text(m.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onThemeChanged(temp)
                    themeDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { themeDialog = false }) { Text("取消") } },
        )
    }

    if (changePwd) ChangeMasterPasswordDialog(repo, snack) { changePwd = false }

    exportPreview?.let { text ->
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            title = { Text("导出预览") },
            text = {
                SelectionContainer {
                    Text(text, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = BackupManager.exportTxtFileName()
                    val path = FileIo.join(repo.documentsPath(), name)
                    FileIo.writeText(path, text)
                    PlatformActions.shareOrRevealFile(path, "text/plain")
                    exportPreview = null
                    scope.launch { snack.showSnackbar("已导出: $name") }
                }) { Text("确认导出") }
            },
            dismissButton = { TextButton(onClick = { exportPreview = null }) { Text("取消") } },
        )
    }

    if (importAskPwd) {
        var pwd by remember { mutableStateOf(AppSession.session.masterPassword.orEmpty()) }
        var show by remember { mutableStateOf(false) }
        var parsing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!parsing) importAskPwd = false },
            title = { Text("输入备份密码") },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("备份时的主密码") },
                    enabled = !parsing,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show) { show = !show } },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !parsing && pwd.isNotEmpty(),
                    onClick = {
                        val bytes = pendingImportBytes ?: return@TextButton
                        parsing = true
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.Default) {
                                    BackupManager.parseBackup(bytes, pwd)
                                }
                            }.onSuccess {
                                importPreview = it
                                importAskPwd = false
                                pendingImportBytes = null
                            }.onFailure {
                                snack.showSnackbar(it.message ?: "解析失败")
                            }
                            parsing = false
                        }
                    },
                ) { Text(if (parsing) "解析中…" else "解析") }
            },
            dismissButton = {
                TextButton(
                    enabled = !parsing,
                    onClick = {
                        importAskPwd = false
                        pendingImportBytes = null
                    },
                ) { Text("取消") }
            },
        )
    }

    importPreview?.let { payload ->
        var importing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!importing) importPreview = null },
            title = { Text("导入预览") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("版本: ${payload.version}")
                    Text("时间: ${BackupManager.formatDateTime(payload.timestamp)}")
                    Text("条数: ${payload.entryCount}")
                    Spacer(Modifier.height(8.dp))
                    payload.entries.take(5).forEach { e ->
                        Text("• ${e.title} / ${e.username}")
                    }
                    if (payload.entries.size > 5) {
                        Text("… 还有 ${payload.entries.size - 5} 条")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !importing,
                    onClick = {
                        importing = true
                        scope.launch {
                            val n = withContext(Dispatchers.Default) {
                                repo.importMerge(payload.entries)
                            }
                            importPreview = null
                            onListChanged()
                            snack.showSnackbar("已导入 $n 条")
                        }
                    },
                ) { Text(if (importing) "导入中…" else "导入数据") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        enabled = !importing,
                        onClick = {
                            val full = payload.entries.joinToString("\n") {
                                "${it.title}\t${it.username}\t${it.password}"
                            }
                            PlatformActions.copyToClipboard(full)
                            scope.launch { snack.showSnackbar("已复制完整数据") }
                        },
                    ) { Text("复制完整数据") }
                    TextButton(
                        enabled = !importing,
                        onClick = { importPreview = null },
                    ) { Text("关闭") }
                }
            },
        )
    }

    if (storageDialog) {
        AlertDialog(
            onDismissRequest = { storageDialog = false },
            title = { Text("存储详情") },
            text = {
                SelectionContainer {
                    Text(
                        """
                        目录: ${repo.documentsPath()}
                        数据库: ${StoreFiles.DB}
                        导出/备份会写入同目录（带时间戳文件名）
                        """.trimIndent(),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { storageDialog = false }) { Text("关闭") } },
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text("PwdStore") },
            text = {
                Text("版本 1.0\n本地存储 · 高强度加密 · 主密码保护")
            },
            confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text("关闭") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
}

@Composable
private fun ChangeMasterPasswordDialog(
    repo: PasswordRepository,
    snack: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    var cur by remember { mutableStateOf("") }
    var neu by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("修改主密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = cur,
                    onValueChange = { cur = it },
                    label = { Text("当前密码") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show) { show = !show } },
                )
                OutlinedTextField(
                    value = neu,
                    onValueChange = { neu = it },
                    label = { Text("新密码") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("确认新密码") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                )
                if (loading) {
                    Spacer(Modifier.height(8.dp))
                    Text("处理中…")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    when {
                        neu.length < 4 -> scope.launch { snack.showSnackbar("新密码至少 4 位") }
                        neu != confirm -> scope.launch { snack.showSnackbar("两次新密码不一致") }
                        else -> {
                            loading = true
                            scope.launch {
                                val err = withContext(Dispatchers.Default) {
                                    runCatching { repo.changeMasterPassword(cur, neu) }
                                        .exceptionOrNull()?.message
                                }
                                loading = false
                                if (err != null) snack.showSnackbar(err)
                                else {
                                    snack.showSnackbar("主密码已修改")
                                    onDismiss()
                                }
                            }
                        }
                    }
                },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onDismiss) { Text("取消") } },
    )
}
