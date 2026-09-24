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
import androidx.compose.material3.OutlinedTextField
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
                title = { Text(if (editId == null) "添加密码" else "编辑密码") },
                navigationIcon = { IconButton(onClick = onBack) { AppIcon(AppIcons.Back, "返回") } },
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
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { IconGlyph(iconKey, Modifier.size(24.dp)) },
                trailingIcon = {
                    IconButton(onClick = { iconMenu = true }) {
                        AppIcon(AppIcons.Apps, "更改图标")
                    }
                },
            )
            if (iconMenu) {
                AlertDialog(
                    onDismissRequest = { iconMenu = false },
                    title = { Text("选择图标") },
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
                                                if (title.isBlank() || IconCatalog.all.any { it.label == title }) {
                                                    title = opt.label
                                                }
                                                iconMenu = false
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconGlyph(opt.key, Modifier.size(28.dp))
                                                Spacer(Modifier.height(4.dp))
                                                Text(opt.label)
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
                    dismissButton = { TextButton(onClick = { iconMenu = false }) { Text("关闭") } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名/邮箱") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
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
                    AppIcon(AppIcons.Refresh, "重新生成")
                }
                IconButton(onClick = { genDialog = true }) {
                    AppIcon(AppIcons.Settings, "生成设置")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = category.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("分类") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { catExpanded = true }) { Text("选择") }
                },
            )
            if (catExpanded) {
                AlertDialog(
                    onDismissRequest = { catExpanded = false },
                    title = { Text("选择分类") },
                    text = {
                        Column {
                            PasswordCategory.entries.forEach { c ->
                                TextButton(onClick = {
                                    category = c
                                    catExpanded = false
                                }) { Text(c.label) }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { catExpanded = false }) { Text("关闭") } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    when {
                        title.isBlank() || username.isBlank() || password.isBlank() ->
                            scope.launch { snack.showSnackbar("请填写必填项") }
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
            ) { Text("保存") }
        }
    }

    if (genDialog) {
        var local = prefs
        AlertDialog(
            onDismissRequest = { genDialog = false },
            title = { Text("生成设置") },
            text = {
                Column {
                    PrefSwitch("大写", local.uppercase) {
                        local = local.copy(uppercase = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch("小写", local.lowercase) {
                        local = local.copy(lowercase = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch("数字", local.digits) {
                        local = local.copy(digits = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    PrefSwitch("特殊字符", local.symbols) {
                        local = local.copy(symbols = it)
                        prefs = local
                        repo.writeGeneratorPrefs(local)
                    }
                    Text("当前长度: ${local.length} 位")
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
                }) { Text("生成") }
            },
            dismissButton = { TextButton(onClick = { genDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun PrefSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
