package zhiqiu.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import zhiqiu.app.ui.components.AlertDialog
import zhiqiu.app.ui.components.Button
import zhiqiu.app.ui.components.FilledTonalButton
import zhiqiu.applewidgets.kmp.basic.IconButton
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.applewidgets.kmp.theme.AppleWidgetsTheme
import zhiqiu.app.ui.components.OutlinedTextField
import zhiqiu.app.ui.components.AppScaffold as Scaffold
import zhiqiu.app.ui.components.AppSnackbarHost as SnackbarHost
import zhiqiu.app.ui.components.AppSnackbarHostState as SnackbarHostState
import zhiqiu.applewidgets.kmp.basic.Surface
import zhiqiu.applewidgets.kmp.basic.SurfaceDefaults
import zhiqiu.applewidgets.kmp.basic.Text
import zhiqiu.app.ui.components.TextButton
import zhiqiu.app.ui.components.AppTopBar as TopAppBar
import zhiqiu.app.ui.components.AppTopBarDefaults as TopAppBarDefaults
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhiqiu.app.data.BiometricAuth
import zhiqiu.app.data.BiometryKind
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PasswordStrength
import zhiqiu.app.data.PasswordStrengthMeter
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.HeroLockBadge
import zhiqiu.app.ui.components.PasswordVisibilityToggle
import zhiqiu.app.ui.components.SoftCard
import zhiqiu.app.ui.components.ThemeToggleIcon
import zhiqiu.app.ui.screenWindowInsets

@Composable
fun SetupMasterPasswordScreen(
    repo: PasswordRepository,
    onDone: () -> Unit,
) {
    val s = I18n.strings
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun submit() {
        when {
            password.length < 4 -> scope.launch { snack.showSnackbar(s.errPasswordShort) }
            password != confirm -> scope.launch { snack.showSnackbar(s.errPasswordMismatch) }
            else -> {
                loading = true
                scope.launch {
                    val err = withContext(Dispatchers.Default) {
                        runCatching { repo.setupMasterPassword(password) }.exceptionOrNull()?.message
                    }
                    loading = false
                    if (err != null) snack.showSnackbar(s.setupFailedWith(err))
                    else onDone()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = screenWindowInsets(),
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroLockBadge()
            Spacer(Modifier.height(20.dp))
            Text(s.setupTitle, style = OinkTheme.textStyles.h3)
            Spacer(Modifier.height(8.dp))
            Text(
                s.setupHint,
                style = OinkTheme.textStyles.body,
                color = OinkTheme.colors.text2,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s.masterPassword) },
                    singleLine = true,
                    leadingIcon = AppIcons.Lock,
                    visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show1) { show1 = !show1 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                    supportingText = {
                        val strength = PasswordStrengthMeter.score(password)
                        if (password.isNotEmpty()) {
                            Text(
                                when (strength) {
                                    PasswordStrength.TOO_SHORT -> s.errPasswordShort
                                    PasswordStrength.WEAK -> s.strengthWeak
                                    PasswordStrength.MEDIUM -> s.strengthMedium
                                    PasswordStrength.STRONG -> s.strengthStrong
                                },
                                color = when (strength) {
                                    PasswordStrength.STRONG -> OinkTheme.colors.accent
                                    PasswordStrength.MEDIUM -> OinkTheme.colors.info
                                    else -> OinkTheme.colors.danger
                                },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(s.confirmPassword) },
                    singleLine = true,
                    leadingIcon = AppIcons.Lock,
                    visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show2) { show2 = !show2 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                ) {
                    Text(if (loading) s.processing else s.startUsing)
                }
            }
        }
    }
}

@Composable
fun UnlockScreen(
    repo: PasswordRepository,
    onUnlocked: () -> Unit,
    onRecovered: (String) -> Unit,
    onReset: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val s = I18n.strings
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(0) }
    var showVerify by remember { mutableStateOf(false) }
    val hasSecurityQuestion = remember { repo.readSecurityQuestion() != null }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var auth by remember { mutableStateOf(repo.readAuthConfig()) }
    val available = remember { BiometricAuth.isAvailable() }
    val kind = remember { BiometricAuth.sensorKind() }
    val canBio = auth.biometricEnabled && available
    val inviteBio = available && !auth.biometricEnabled
    var prompted by remember { mutableStateOf(false) }

    fun unlockOk() {
        repo.onManualUnlockSuccess()
        onUnlocked()
    }

    fun submit() {
        if (password.isBlank()) {
            scope.launch { snack.showSnackbar(s.errEmptyPassword) }
            return
        }
        loading = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { repo.verifyMasterPassword(password) }
            loading = false
            if (ok) unlockOk() else snack.showSnackbar(s.errWrongPassword)
        }
    }

    fun runBiometric() {
        BiometricAuth.authenticate(
            title = s.unlock,
            subtitle = s.biometricUnlockFor(kind),
            negativeButton = s.biometricUsePassword,
            onSuccess = {
                scope.launch {
                    val ok = withContext(Dispatchers.Default) { repo.tryBiometricUnlock() }
                    if (ok) {
                        unlockOk()
                    } else {
                        snack.showSnackbar(s.biometricNoCredential)
                    }
                }
            },
            onError = { msg -> scope.launch { snack.showSnackbar(msg) } },
        )
    }

    fun enableBiometric() {
        if (password.isBlank()) {
            scope.launch { snack.showSnackbar(s.biometricEnableNeedsPassword) }
            return
        }
        BiometricAuth.authenticate(
            title = s.biometricSetupTitle,
            subtitle = s.biometricSetupSubtitle,
            negativeButton = s.cancel,
            onSuccess = {
                scope.launch {
                    val typed = withContext(Dispatchers.Default) { repo.verifyMasterPassword(password) }
                    if (!typed) {
                        snack.showSnackbar(s.errWrongPassword)
                        return@launch
                    }
                    auth = auth.copy(biometricEnabled = true)
                    repo.writeAuthConfig(auth)
                    unlockOk()
                }
            },
            onError = { msg -> scope.launch { snack.showSnackbar(msg) } },
        )
    }

    LaunchedEffect(Unit) {
        if (canBio && !prompted) {
            prompted = true
            runBiometric()
        }
    }

    Scaffold(
        contentWindowInsets = screenWindowInsets(),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(s.unlock) },
                actions = { IconButton(onClick = onToggleTheme) { ThemeToggleIcon() } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OinkTheme.colors.bg,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (canBio) {
                HeroLockBadge(AppIcons.Fingerprint, s.biometricUnlockFor(kind)) { runBiometric() }
            } else {
                HeroLockBadge()
            }
            Spacer(Modifier.height(16.dp))
            Text(s.welcomeBack, style = OinkTheme.textStyles.h3)
            Text(
                if (canBio) s.biometricUnlockFor(kind) else s.unlockHint,
                style = OinkTheme.textStyles.body,
                color = OinkTheme.colors.text2,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s.masterPassword) },
                    singleLine = true,
                    leadingIcon = AppIcons.Lock,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show) { show = !show } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                ) { Text(s.unlock) }
                if (canBio) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { runBiometric() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = AppleWidgetsTheme.componentStyles.controlShape,
                    ) {
                        AppIcon(AppIcons.Fingerprint, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(s.biometricUnlockFor(kind))
                    }
                }
                if (auth.biometricEnabled && !available) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        s.biometricUnavailable,
                        style = OinkTheme.textStyles.caption,
                        color = OinkTheme.colors.text2,
                    )
                }
                if (inviteBio) {
                    Spacer(Modifier.height(16.dp))
                    BiometricInvite(kind) { enableBiometric() }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                if (hasSecurityQuestion) showVerify = true else confirmReset = 1
            }) { Text(s.forgotPassword) }
        }
    }

    if (showVerify) {
        SecurityQuestionVerifyDialog(
            repo = repo,
            onRecovered = { recovered ->
                showVerify = false
                onRecovered(recovered)
            },
            onFallback = {
                showVerify = false
                confirmReset = 1
            },
            onDismiss = { showVerify = false },
        )
    }

    if (confirmReset == 1) {
        ConfirmDialog(
            title = s.resetTitle,
            text = s.resetText,
            onDismiss = { confirmReset = 0 },
            onConfirm = { confirmReset = 2 },
        )
    }
    if (confirmReset == 2) {
        ConfirmDialog(
            title = s.resetConfirmTitle,
            text = s.resetConfirmText,
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
private fun SecurityQuestionVerifyDialog(
    repo: PasswordRepository,
    onRecovered: (String) -> Unit,
    onFallback: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = I18n.strings
    val config = remember { repo.readSecurityQuestion() }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cooldown) {
        while (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    fun submitVerify() {
        if (answer.isBlank()) {
            error = s.errSecurityAnswerBlank
            return
        }
        loading = true
        scope.launch {
            val recovered = withContext(Dispatchers.Default) { repo.recoverMasterPassword(answer) }
            loading = false
            if (recovered != null) {
                onRecovered(recovered)
                return@launch
            }
            val remain = repo.securityLockRemainingMs()
            if (remain > 0) {
                cooldown = ((remain + 999) / 1000).toInt()
                error = s.securityTooManyAttempts
            } else {
                error = s.errSecurityAnswerWrong
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(s.securityVerifyTitle) },
        text = {
            Column {
                Text(
                    s.securityVerifyHint,
                    style = OinkTheme.textStyles.caption,
                    color = OinkTheme.colors.text2,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    config?.let { s.securityQuestionText(it.questionKey) } ?: "",
                    style = OinkTheme.textStyles.h4,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; error = null },
                    label = { Text(s.securityAnswerLabel) },
                    singleLine = true,
                    isError = error != null,
                    enabled = !loading && cooldown == 0,
                    supportingText = {
                        Column {
                            if (error != null) {
                                Text(error!!, color = OinkTheme.colors.danger)
                            } else if (cooldown == 0) {
                                Text(
                                    s.securityAttemptsLeft(repo.securityAttemptsLeft()),
                                    color = OinkTheme.colors.text2,
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!loading && cooldown == 0) submitVerify() }),
                )
                if (loading) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.processing)
                }
                TextButton(
                    enabled = !loading,
                    onClick = onFallback,
                ) { Text(s.securityFallback) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && cooldown == 0,
                onClick = { submitVerify() },
            ) { Text(s.securityVerifyBtn) }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
fun ResetMasterPasswordScreen(
    repo: PasswordRepository,
    recovered: String,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val s = I18n.strings
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun submit() {
        when {
            password.length < 4 -> scope.launch { snack.showSnackbar(s.errPasswordShort) }
            password != confirm -> scope.launch { snack.showSnackbar(s.errPasswordMismatch) }
            else -> {
                loading = true
                scope.launch {
                    val err = withContext(Dispatchers.Default) {
                        runCatching { repo.changeMasterPassword(recovered, password) }
                            .exceptionOrNull()?.message
                    }
                    loading = false
                    if (err != null) snack.showSnackbar(err) else onDone()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = screenWindowInsets(),
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(s.securityResetPwdTitle) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { AppIcon(AppIcons.Back, s.cdBack) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OinkTheme.colors.bg,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroLockBadge()
            Spacer(Modifier.height(20.dp))
            Text(s.securityResetPwdTitle, style = OinkTheme.textStyles.h3)
            Spacer(Modifier.height(8.dp))
            Text(
                s.securityResetPwdHint,
                style = OinkTheme.textStyles.body,
                color = OinkTheme.colors.text2,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s.masterPassword) },
                    singleLine = true,
                    leadingIcon = AppIcons.Lock,
                    visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show1) { show1 = !show1 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                    supportingText = {
                        val strength = PasswordStrengthMeter.score(password)
                        if (password.isNotEmpty()) {
                            Text(
                                when (strength) {
                                    PasswordStrength.TOO_SHORT -> s.errPasswordShort
                                    PasswordStrength.WEAK -> s.strengthWeak
                                    PasswordStrength.MEDIUM -> s.strengthMedium
                                    PasswordStrength.STRONG -> s.strengthStrong
                                },
                                color = when (strength) {
                                    PasswordStrength.STRONG -> OinkTheme.colors.accent
                                    PasswordStrength.MEDIUM -> OinkTheme.colors.info
                                    else -> OinkTheme.colors.danger
                                },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(s.confirmPassword) },
                    singleLine = true,
                    leadingIcon = AppIcons.Lock,
                    visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { PasswordVisibilityToggle(show2) { show2 = !show2 } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AppleWidgetsTheme.componentStyles.controlShape,
                ) {
                    Text(if (loading) s.processing else s.ok)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                s.securityResetPwdNote,
                style = OinkTheme.textStyles.caption,
                color = OinkTheme.colors.text2,
            )
        }
    }
}

@Composable
private fun BiometricInvite(kind: BiometryKind, onEnable: () -> Unit) {
    val s = I18n.strings
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = SurfaceDefaults.surfaceColors(
            containerColor = OinkTheme.colors.accentSoft.copy(alpha = 0.4f),
            contentColor = OinkTheme.colors.text,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                AppIcons.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = OinkTheme.colors.accent,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    s.biometricInviteTitle(kind),
                    style = OinkTheme.textStyles.label,
                )
                Text(
                    s.biometricInviteHint(kind),
                    style = OinkTheme.textStyles.caption,
                    color = OinkTheme.colors.text2,
                )
            }
            Button(
                onClick = onEnable,
                shape = AppleWidgetsTheme.componentStyles.controlShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) { Text(s.enable) }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val s = I18n.strings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(s.ok) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        shape = AppleWidgetsTheme.componentStyles.cardShape,
    )
}
