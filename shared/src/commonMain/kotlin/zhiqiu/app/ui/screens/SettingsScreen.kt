package zhiqiu.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.ui.components.AlertDialog
import zhiqiu.applewidgets.kmp.basic.IconButton
import zhiqiu.applewidgets.kmp.theme.AppleWidgetsTheme
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.app.ui.components.OutlinedTextField
import zhiqiu.applewidgets.kmp.basic.RadioButton
import zhiqiu.app.ui.components.AppScaffold as Scaffold
import zhiqiu.applewidgets.kmp.basic.Slider
import zhiqiu.app.ui.components.AppSnackbarHost as SnackbarHost
import zhiqiu.app.ui.components.AppSnackbarHostState as SnackbarHostState
import zhiqiu.applewidgets.kmp.basic.Switch
import zhiqiu.applewidgets.kmp.basic.Text
import zhiqiu.app.ui.components.TextButton
import zhiqiu.app.ui.components.AppTopBar as TopAppBar
import zhiqiu.app.ui.components.AppTopBarDefaults as TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import zhiqiu.app.data.MirrorUpload
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PasswordStrength
import zhiqiu.app.data.PasswordStrengthMeter
import zhiqiu.app.data.PlatformActions
import zhiqiu.app.data.SecurityQuestionConfig
import zhiqiu.app.data.SecurityQuestions
import zhiqiu.app.data.StoreFiles
import zhiqiu.app.data.ThemeMode
import zhiqiu.app.i18n.AppLanguage
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.PasswordVisibilityToggle
import zhiqiu.app.ui.components.QrCodeDialog
import zhiqiu.app.ui.components.SettingRow
import zhiqiu.app.ui.components.SettingsSection
import zhiqiu.app.ui.screenWindowInsets

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
    var secCfg by remember { mutableStateOf<SecurityQuestionConfig?>(repo.readSecurityQuestion()) }
    var secDialog by remember { mutableStateOf(false) }
    var exportPreview by remember { mutableStateOf<String?>(null) }
    var importPreview by remember { mutableStateOf<BackupPayload?>(null) }
    var importAskPwd by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var storageDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var qrText by remember { mutableStateOf<String?>(null) }
    var qrLink by remember { mutableStateOf<String?>(null) }
    var qrLoading by remember { mutableStateOf(false) }
    var qrError by remember { mutableStateOf<String?>(null) }
    var qrOpen by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hardware = remember { BiometricAuth.hardwareSupported() }
    val bioReady = remember { BiometricAuth.isAvailable() }

    fun showTextQr(text: String) {
        qrText = text
        qrLink = null
        qrError = null
        qrLoading = false
        qrOpen = true
    }

    fun showFileLinkQr(fileName: String, bytes: ByteArray) {
        qrText = null
        qrLink = null
        qrError = null
        qrLoading = true
        qrOpen = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    MirrorUpload.upload(fileName, bytes)
                }
            }.onSuccess {
                qrLink = it
                qrLoading = false
            }.onFailure {
                qrError = it.message ?: s.qrUploadFailed
                qrLoading = false
            }
        }
    }

    fun save(config: AuthConfig) {
        auth = config
        repo.writeAuthConfig(config)
    }

    Scaffold(
        contentWindowInsets = screenWindowInsets(),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(s.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) { AppIcon(AppIcons.Back, s.cdBack) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OinkTheme.colors.bg,
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
                    icon = AppIcons.Key,
                    title = s.securityQuestionRow,
                    subtitle = secCfg?.let { s.securityQuestionText(it.questionKey) } ?: s.securityQuestionOff,
                    onClick = { secDialog = true },
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
                                    style = OinkTheme.textStyles.caption,
                                    color = OinkTheme.colors.text2,
                                )
                                Slider(
                                    value = (auth.duration / 24).toFloat().coerceIn(0.5f, 7f),
                                    onValueChange = { save(auth.copy(duration = (it * 2).toInt() / 2f * 24.0)) },
                                    valueRange = 0.5f..7f,
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
                    icon = AppIcons.QrCode,
                    title = s.encryptedBackupQr,
                    subtitle = s.qrLinkHint,
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
                                    showFileLinkQr(name, bytes)
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
        ChangeMasterPasswordDialog(repo, snack) {
            changePwd = false
            secCfg = repo.readSecurityQuestion()
        }
    }
    if (secDialog) {
        SecurityQuestionSetupDialog(
            repo = repo,
            current = secCfg,
            snack = snack,
            onDismiss = {
                secDialog = false
                secCfg = repo.readSecurityQuestion()
            },
        )
    }

    exportPreview?.let { text ->
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            title = { Text(s.exportPreview) },
            text = {
                SelectionContainer {
                    Text(text, style = OinkTheme.textStyles.mono)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showTextQr(text)
                    }) { Text(s.qrFromText) }
                    TextButton(onClick = {
                        val name = BackupManager.exportTxtFileName()
                        val bytes = text.encodeToByteArray()
                        val path = FileIo.join(repo.dataPath(), name)
                        FileIo.writeBytes(path, bytes)
                        showFileLinkQr(name, bytes)
                    }) { Text(s.qrFromFile) }
                    TextButton(onClick = {
                        val name = BackupManager.exportTxtFileName()
                        val path = FileIo.join(repo.dataPath(), name)
                        FileIo.writeText(path, text)
                        PlatformActions.shareOrRevealFile(path, "text/plain")
                        exportPreview = null
                        scope.launch { snack.showSnackbar(s.exportedTo(name)) }
                    }) { Text(s.confirmExport) }
                }
            },
            dismissButton = {
                TextButton(onClick = { exportPreview = null }) { Text(s.cancel) }
            },
        )
    }

    if (qrOpen) {
        val payload = qrLink ?: qrText
        QrCodeDialog(
            title = if (qrLink != null || qrLoading || (qrError != null && qrText == null)) {
                s.qrLinkTitle
            } else {
                s.qrTextTitle
            },
            payload = payload,
            loading = qrLoading,
            error = qrError,
            onCopy = payload?.let { text ->
                {
                    PlatformActions.copyToClipboard(text)
                    scope.launch { snack.showSnackbar(s.copied) }
                }
            },
            onDismiss = {
                qrOpen = false
                qrLoading = false
                qrError = null
                qrText = null
                qrLink = null
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
                    Text(s.aboutBody, style = OinkTheme.textStyles.body)
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
        style = OinkTheme.textStyles.body,
        color = OinkTheme.colors.text2,
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
    val hadSecurity = remember { repo.readSecurityQuestion() != null }
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
                    supportingText = {
                        val strength = PasswordStrengthMeter.score(neu)
                        if (neu.isNotEmpty()) {
                            Text(
                                when (strength) {
                                    PasswordStrength.TOO_SHORT -> s.errPasswordShort
                                    PasswordStrength.WEAK -> s.strengthWeak
                                    PasswordStrength.MEDIUM -> s.strengthMedium
                                    PasswordStrength.STRONG -> s.strengthStrong
                                },
                            )
                        }
                    },
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
                                    snack.showSnackbar(if (hadSecurity) s.securityStaleNote else s.masterChanged)
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

@Composable
private fun SecurityQuestionSetupDialog(
    repo: PasswordRepository,
    current: SecurityQuestionConfig?,
    snack: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val s = I18n.strings
    var selected by remember { mutableStateOf(current?.questionKey ?: SecurityQuestions.presetKeys.first()) }
    var answer by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(s.securitySetupTitle) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    s.securitySetupHint,
                    style = OinkTheme.textStyles.caption,
                    color = OinkTheme.colors.text2,
                )
                if (current != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.securityReplaceHint,
                        style = OinkTheme.textStyles.caption,
                        color = OinkTheme.colors.text2,
                    )
                }
                Spacer(Modifier.height(8.dp))
                SecurityQuestions.presetKeys.forEach { key ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(AppleWidgetsTheme.componentStyles.controlShape)
                            .clickable(enabled = !saving) { selected = key }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == key, onClick = { selected = key })
                        Text(s.securityQuestionText(key), style = OinkTheme.textStyles.body)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val answerLen = answer.trim().length
                val answerTooShort = answer.isNotBlank() && answerLen < PasswordRepository.SECURITY_MIN_ANSWER_LENGTH
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text(s.securityAnswerLabel) },
                    singleLine = true,
                    enabled = !saving,
                    isError = answerTooShort,
                    supportingText = {
                        Text(
                            if (answerTooShort) s.errSecurityAnswerTooShort else s.securityAnswerHint,
                            color = if (answerTooShort) OinkTheme.colors.danger else OinkTheme.colors.text2,
                        )
                    },
                )
                if (saving) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.processing)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && answer.trim().length >= PasswordRepository.SECURITY_MIN_ANSWER_LENGTH,
                onClick = {
                    scope.launch {
                        saving = true
                        val err = withContext(Dispatchers.Default) {
                            runCatching { repo.writeSecurityQuestion(selected, answer) }
                                .exceptionOrNull()?.message
                        }
                        saving = false
                        if (err != null) snack.showSnackbar(err)
                        else {
                            snack.showSnackbar(s.securitySaved)
                            onDismiss()
                        }
                    }
                },
            ) { Text(s.save) }
        },
        dismissButton = {
            if (current != null) {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        scope.launch {
                            saving = true
                            withContext(Dispatchers.Default) { repo.clearSecurityQuestion() }
                            saving = false
                            snack.showSnackbar(s.securityCleared)
                            onDismiss()
                        }
                    },
                ) { Text(s.securityClearBtn) }
            } else {
                TextButton(enabled = !saving, onClick = onDismiss) { Text(s.cancel) }
            }
        },
    )
}
