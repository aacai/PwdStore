package zhiqiu.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.TopAppBarDefaults
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
import zhiqiu.app.AppInfo
import zhiqiu.app.data.AuthConfig
import zhiqiu.app.data.BackupManager
import zhiqiu.app.data.BackupPayload
import zhiqiu.app.data.BiometricAuth
import zhiqiu.app.data.FileIo
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PlatformActions
import zhiqiu.app.data.StoreFiles
import zhiqiu.app.data.ThemeMode
import zhiqiu.app.i18n.AppLanguage
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.PasswordVisibilityToggle
import zhiqiu.app.ui.components.SettingRow
import zhiqiu.app.ui.components.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: PasswordRepository,
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onLocked: () -> Unit,
    onListChanged: () -> Unit,
) {
    val s = I18n.strings
    var auth by remember { mutableStateOf(repo.readAuthConfig()) }
    var themeDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var changePwd by remember { mutableStateOf(false) }
    var exportPreview by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<BackupPayload?>(null) }
    var importAskPwd by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var storageDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hardware = remember { BiometricAuth.hardwareSupported() }
    val bioReady = remember { BiometricAuth.isAvailable() }

    fun save(config: AuthConfig) {
        auth = config
        repo.writeAuthConfig(config)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(s.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) { AppIcon(AppIcons.Back, s.cdBack) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsSection(s.sectionSecurity) {
                SettingRow(
                    icon = AppIcons.Password,
                    title = s.changeMasterPassword,
                    onClick = { changePwd = true },
                )
                SettingRow(
                    icon = AppIcons.LockOpen,
                    title = s.passwordFree,
                    subtitle = if (auth.enabled) s.daysOf(daysText(auth.duration)) else null,
                    trailing = {
                        Switch(
                            checked = auth.enabled,
                            onCheckedChange = { save(auth.copy(enabled = it)) },
                        )
                    },
                    extra = {
                        if (auth.enabled) {
                            Column(Modifier.padding(start = 48.dp, end = 12.dp, bottom = 10.dp)) {
                                Text(
                                    s.passwordFreeDuration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Slider(
                                    value = (auth.duration / 24).toFloat().coerceIn(0.5f, 7f),
                                    onValueChange = { save(auth.copy(duration = (it * 2).toInt() / 2f * 24.0)) },
                                    valueRange = 0.5f..7f,
                                    steps = 12,
                                )
                            }
                        }
                    },
                )
                SettingRow(
                    icon = AppIcons.Fingerprint,
                    title = s.biometricUnlockRow,
                    subtitle = when {
                        !hardware -> s.biometricNoHardware
                        !bioReady -> s.biometricEnrollHint
                        auth.biometricEnabled -> s.biometricOn
                        else -> s.biometricOff
                    },
                    trailing = {
                        Switch(
                            checked = auth.biometricEnabled,
                            enabled = bioReady,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    BiometricAuth.authenticate(
                                        title = s.biometricSetupTitle,
                                        subtitle = s.biometricSetupSubtitle,
                                        negativeButton = s.biometricUsePassword,
                                        onSuccess = { save(auth.copy(biometricEnabled = true)) },
                                        onError = { msg -> scope.launch { snack.showSnackbar(msg) } },
                                    )
                                } else {
                                    save(auth.copy(biometricEnabled = false))
                                }
                            },
                        )
                    },
                )
                SettingRow(
                    icon = AppIcons.Logout,
                    title = s.lockNow,
                    divider = false,
                    onClick = {
                        repo.lockNow()
                        onLocked()
                    },
                )
            }

            SettingsSection(s.sectionDisplay) {
                SettingRow(
                    icon = AppIcons.Palette,
                    title = s.theme,
                    trailing = { ValueText(s.themeLabel(themeMode)) },
                    onClick = { themeDialog = true },
                )
                SettingRow(
                    icon = AppIcons.Language,
                    title = s.language,
                    divider = false,
                    trailing = { ValueText(s.languageLabel(language)) },
                    onClick = { languageDialog = true },
                )
            }

            SettingsSection(s.sectionData) {
                SettingRow(
                    icon = AppIcons.Export,
                    title = s.exportTxt,
                    onClick = {
                        val list = repo.getAllDecrypted()
                        if (list.isEmpty()) {
                            scope.launch { snack.showSnackbar(s.nothingToExport) }
                        } else {
                            exportPreview = BackupManager.exportTxt(list)
                        }
                    },
                )
                SettingRow(
                    icon = AppIcons.Backup,
                    title = s.encryptedBackup,
                    onClick = {
                        val pwd = AppSession.session.masterPassword
                        if (pwd == null) {
                            scope.launch { snack.showSnackbar(s.setMasterFirst) }
                        } else {
                            scope.launch {
                                runCatching {
                                    val bytes = withContext(Dispatchers.Default) {
                                        BackupManager.createEncryptedBackup(repo.getAllDecrypted(), pwd)
                                    }
                                    val name = BackupManager.backupFileName()
                                    FileIo.writeBytes(FileIo.join(repo.dataPath(), name), bytes)
                                    PlatformActions.shareOrRevealFile(
                                        FileIo.join(repo.dataPath(), name),
                                        "application/json",
                                    )
                                    snack.showSnackbar(s.backupSavedTo(name))
                                }.onFailure { snack.showSnackbar(it.message ?: s.backupFailed) }
                            }
                        }
                    },
                )
                SettingRow(
                    icon = AppIcons.Import,
                    title = s.importData,
                    onClick = {
                        PlatformActions.pickFile { bytes ->
                            if (bytes == null) {
                                scope.launch { snack.showSnackbar(s.noFilePicked) }
                            } else {
                                pendingImportBytes = bytes
                                importAskPwd = true
                            }
                        }
                    },
                )
                SettingRow(
                    icon = AppIcons.Folder,
                    title = s.storageDetails,
                    divider = false,
                    onClick = { storageDialog = true },
                )
            }

            SettingsSection(s.sectionAbout) {
                SettingRow(
                    icon = AppIcons.Info,
                    title = s.aboutApp,
                    divider = false,
                    trailing = { ValueText("${s.version} ${AppInfo.VERSION}") },
                    onClick = { aboutDialog = true },
                )
            }
        }
    }

    if (themeDialog) {
        ChoiceDialog(
            title = s.theme,
            options = ThemeMode.entries.toList(),
            selected = themeMode,
            labelOf = { s.themeLabel(it) },
            onSelect = {
                onThemeChanged(it)
                themeDialog = false
            },
            onDismiss = { themeDialog = false },
        )
    }

    if (languageDialog) {
        ChoiceDialog(
            title = s.language,
            options = AppLanguage.entries.toList(),
            selected = language,
            labelOf = { s.languageLabel(it) },
            onSelect = {
                onLanguageChanged(it)
                languageDialog = false
            },
            onDismiss = { languageDialog = false },
        )
    }

    if (changePwd) {
        ChangeMasterPasswordDialog(repo, snack) { changePwd = false }
    }

    exportPreview?.let { text ->
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            title = { Text(s.exportPreview) },
            text = {
                SelectionContainer {
                    Text(text, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = BackupManager.exportTxtFileName()
                    val path = FileIo.join(repo.dataPath(), name)
                    FileIo.writeText(path, text)
                    PlatformActions.shareOrRevealFile(path, "text/plain")
                    exportPreview = null
                    scope.launch { snack.showSnackbar(s.exportedTo(name)) }
                }) { Text(s.confirmExport) }
            },
            dismissButton = {
                TextButton(onClick = { exportPreview = null }) { Text(s.cancel) }
            },
        )
    }

    if (importAskPwd) {
        var pwd by remember { mutableStateOf(AppSession.session.masterPassword.orEmpty()) }
        var show by remember { mutableStateOf(false) }
        var parsing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!parsing) importAskPwd = false },
            title = { Text(s.backupPasswordTitle) },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text(s.backupPasswordHint) },
                    enabled = !parsing,
                    singleLine = true,
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
                                snack.showSnackbar(it.message ?: s.parseFailed)
                            }
                            parsing = false
                        }
                    },
                ) { Text(if (parsing) s.parsing else s.parse) }
            },
            dismissButton = {
                TextButton(
                    enabled = !parsing,
                    onClick = {
                        importAskPwd = false
                        pendingImportBytes = null
                    },
                ) { Text(s.cancel) }
            },
        )
    }

    importPreview?.let { payload ->
        var importing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!importing) importPreview = null },
            title = { Text(s.importPreview) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${s.version}: ${payload.version}")
                    Text("${s.time}: ${BackupManager.formatDateTime(payload.timestamp)}")
                    Text("${s.entryCount}: ${payload.entryCount}")
                    Spacer(Modifier.height(8.dp))
                    payload.entries.take(5).forEach { e ->
                        Text("• ${e.title} / ${e.username}")
                    }
                    if (payload.entries.size > 5) {
                        Text(s.moreEntries(payload.entries.size - 5))
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
                            snack.showSnackbar(s.importedCount(n))
                        }
                    },
                ) { Text(if (importing) s.importing else s.import) }
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
                            scope.launch { snack.showSnackbar(s.copiedFullData) }
                        },
                    ) { Text(s.copyFullData) }
                    TextButton(
                        enabled = !importing,
                        onClick = { importPreview = null },
                    ) { Text(s.close) }
                }
            },
        )
    }

    if (storageDialog) {
        AlertDialog(
            onDismissRequest = { storageDialog = false },
            title = { Text(s.storageDetails) },
            text = {
                SelectionContainer {
                    Column {
                        Text("${s.storageDir}: ${repo.dataPath()}")
                        Text("${s.storageDb}: ${StoreFiles.DB}")
                        Text(s.storageNote)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { storageDialog = false }) { Text(s.close) }
            },
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text(s.appName) },
            text = {
                Column {
                    Text("${s.version} ${AppInfo.VERSION}")
                    Text(s.aboutBody, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutDialog = false }) { Text(s.close) }
            },
        )
    }
}

@Composable
private fun ValueText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(labelOf(option))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(I18n.strings.cancel) } },
    )
}

@Composable
private fun ChangeMasterPasswordDialog(
    repo: PasswordRepository,
    snack: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val s = I18n.strings
    var cur by remember { mutableStateOf("") }
    var neu by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(s.changeMasterPassword) },
        text = {
            Column {
                OutlinedTextField(
                    value = cur,
                    onValueChange = { cur = it },
                    label = { Text(s.masterPassword) },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show) { show = !show } },
                )
                OutlinedTextField(
                    value = neu,
                    onValueChange = { neu = it },
                    label = { Text(s.password) },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(s.confirmPassword) },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                )
                if (loading) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.processing)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading,
                onClick = {
                    when {
                        neu.length < 4 -> scope.launch { snack.showSnackbar(s.errPasswordShort) }
                        neu != confirm -> scope.launch { snack.showSnackbar(s.errPasswordMismatch) }
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
                                    snack.showSnackbar(s.masterChanged)
                                    onDismiss()
                                }
                            }
                        }
                    }
                },
            ) { Text(s.ok) }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onDismiss) { Text(s.cancel) } },
    )
}

private fun daysText(durationHours: Double): String {
    val days = durationHours / 24
    return if (days == days.toLong().toDouble()) days.toLong().toString() else days.toString()
}
