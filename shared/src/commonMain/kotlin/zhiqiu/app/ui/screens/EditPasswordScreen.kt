package zhiqiu.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zhiqiu.app.data.IconCatalog
import zhiqiu.app.data.PasswordCategory
import zhiqiu.app.data.PasswordEntry
import zhiqiu.app.data.PasswordGenerator
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PlatformTime
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.IconGlyph
import zhiqiu.app.ui.components.PasswordVisibilityToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPasswordScreen(
    repo: PasswordRepository,
    editId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val s = I18n.strings
    val existing = remember(editId) { editId?.let { repo.getById(it) } }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var username by remember { mutableStateOf(existing?.username.orEmpty()) }
    var password by remember { mutableStateOf(existing?.password.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var category by remember { mutableStateOf(existing?.categoryEnum() ?: PasswordCategory.OTHER) }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "other") }
    var showPwd by remember { mutableStateOf(repo.readShowPasswordPref()) }
    var catExpanded by remember { mutableStateOf(false) }
    var iconMenu by remember { mutableStateOf(false) }
    var genDialog by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(repo.readGeneratorPrefs()) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(if (editId == null) s.addEntry else s.editEntry) },
                navigationIcon = { IconButton(onClick = onBack) { AppIcon(AppIcons.Back, s.cdBack) } },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(s.name) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { IconGlyph(iconKey, Modifier.size(24.dp)) },
                trailingIcon = {
                    IconButton(onClick = { iconMenu = true }) {
                        AppIcon(AppIcons.Apps, s.cdChangeIcon)
                    }
                },
            )
            if (iconMenu) {
                AlertDialog(
                    onDismissRequest = { iconMenu = false },
                    title = { Text(s.selectIcon) },
                    text = {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            IconCatalog.all.chunked(3).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEach { opt ->
                                        TextButton(
                                            onClick = {
                                                iconKey = opt.key
                                                if (title.isBlank() ||
                                                    IconCatalog.all.any { s.iconLabel(it.key) == title }
                                                ) {
                                                    title = s.iconLabel(opt.key)
                                                }
                                                iconMenu = false
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconGlyph(opt.key, Modifier.size(28.dp))
                                                Spacer(Modifier.height(4.dp))
                                                Text(s.iconLabel(opt.key))
                                            }
                                        }
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { iconMenu = false }) { Text(s.close) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(s.usernameOrEmail) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(s.password) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityToggle(showPwd) {
                        showPwd = !showPwd
                        repo.writeShowPasswordPref(showPwd)
                    }
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { password = PasswordGenerator.generate(prefs) }) {
                    AppIcon(AppIcons.Refresh, s.cdRegenerate)
                }
                IconButton(onClick = { genDialog = true }) {
                    AppIcon(AppIcons.Settings, s.generatorSettings)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.categoryLabel(category),
                onValueChange = {},
                readOnly = true,
                label = { Text(s.category) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { catExpanded = true }) { Text(s.select) }
                },
            )
            if (catExpanded) {
                AlertDialog(
                    onDismissRequest = { catExpanded = false },
                    title = { Text(s.selectCategory) },
                    text = {
                        Column {
                            PasswordCategory.entries.forEach { c ->
                                TextButton(onClick = {
                                    category = c
                                    catExpanded = false
                                }) { Text(s.categoryLabel(c)) }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { catExpanded = false }) { Text(s.close) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(s.notes) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    when {
                        title.isBlank() || username.isBlank() || password.isBlank() ->
                            scope.launch { snack.showSnackbar(s.errRequiredFields) }

                        else -> {
                            if (editId == null) {
                                repo.add(
                                    PasswordEntry(
                                        id = "",
                                        title = title.trim(),
                                        username = username.trim(),
                                        password = password,
                                        category = category.name,
                                        createdAt = PlatformTime.nowMillis(),
                                        notes = notes,
                                        iconKey = iconKey,
                                    ),
                                )
                            } else {
                                val old = existing!!
                                repo.update(
                                    old.copy(
                                        title = title.trim(),
                                        username = username.trim(),
                                        password = password,
                                        category = category.name,
                                        notes = notes,
                                        iconKey = iconKey,
                                    ),
                                    previousPassword = old.password,
                                )
                            }
                            onSaved()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(s.save) }
        }
    }

    if (genDialog) {
        var local = prefs
        AlertDialog(
            onDismissRequest = { genDialog = false },
            title = { Text(s.generatorSettings) },
            text = {
                Column {
                    PrefSwitch(s.genUppercase, local.uppercase) {
                        local = local.copy(uppercase = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch(s.genLowercase, local.lowercase) {
                        local = local.copy(lowercase = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch(s.genDigits, local.digits) {
                        local = local.copy(digits = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch(s.genSymbols, local.symbols) {
                        local = local.copy(symbols = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    Text(s.passwordLength(local.length))
                    Slider(
                        value = local.length.toFloat(),
                        onValueChange = {
                            local = local.copy(length = it.toInt().coerceIn(4, 20))
                            prefs = local
                            repo.writeGeneratorPrefs(local)
                        },
                        valueRange = 4f..20f,
                        steps = 15,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    password = PasswordGenerator.generate(prefs)
                    genDialog = false
                }) { Text(s.generate) }
            },
            dismissButton = { TextButton(onClick = { genDialog = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun PrefSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
