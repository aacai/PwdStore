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
import androidx.compose.material3.TopAppBarDefaults
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
import zhiqiu.app.i18n.I18n
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
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroLockBadge()
            Spacer(Modifier.height(20.dp))
            Text(s.setupTitle, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                s.setupHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s.masterPassword) },
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
                    label = { Text(s.confirmPassword) },
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
                    Text(if (loading) s.processing else s.startUsing)
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
    val s = I18n.strings
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bioEnabled = remember { repo.readAuthConfig().biometricEnabled }
    val bioAvailable = remember { BiometricAuth.isAvailable() }
    val canBio = bioEnabled && bioAvailable
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
            subtitle = s.biometricUnlock,
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

    LaunchedEffect(Unit) {
        if (canBio && !prompted) {
            prompted = true
            runBiometric()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(s.unlock) },
                actions = { IconButton(onClick = onToggleTheme) { ThemeToggleIcon() } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
            Spacer(Modifier.height(16.dp))
            Text(s.welcomeBack, style = MaterialTheme.typography.headlineSmall)
            Text(
                s.unlockHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(s.masterPassword) },
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
                ) { Text(s.unlock) }
                if (canBio) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { runBiometric() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        AppIcon(AppIcons.Fingerprint, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(s.biometricUnlock)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmReset = 1 }) { Text(s.forgotPassword) }
        }
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
        shape = MaterialTheme.shapes.large,
    )
}
