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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zhiqiu.app.data.BackupManager
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.PlatformActions
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.IconGlyph
import zhiqiu.app.ui.components.PasswordVisibilityToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    repo: PasswordRepository,
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val entry = remember(id) { repo.getById(id) }
    var showPwd by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (entry == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) { AppIcon(AppIcons.Back, "返回") }
                },
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("密码不存在")
            }
        }
        return
    }

    val history = entry.passwordHistory
    val visibleHistory = if (showAllHistory) history else history.take(3)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) { AppIcon(AppIcons.Back, "返回") }
                },
                actions = {
                    IconButton(onClick = onEdit) { AppIcon(AppIcons.Edit, "编辑") }
                    IconButton(onClick = { confirmDelete = 1 }) { AppIcon(AppIcons.Delete, "删除") }
                },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconGlyph(entry.iconKey, Modifier.size(40.dp))
                Spacer(Modifier.padding(8.dp))
                Column {
                    Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                    Text(entry.categoryEnum().label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            FieldRow("用户名", entry.username, copyable = true, snack = snack, scope = scope)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("密码", style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(if (showPwd) entry.password else "••••••••")
                    }
                }
                PasswordVisibilityToggle(showPwd) { showPwd = !showPwd }
                IconButton(onClick = {
                    PlatformActions.copyToClipboard(entry.password)
                    scope.launch { snack.showSnackbar("已复制") }
                }) { AppIcon(AppIcons.Copy, "复制") }
            }
            Spacer(Modifier.height(12.dp))
            Text("分类: ${entry.categoryEnum().label}")
            Text("创建时间: ${BackupManager.formatDateTime(entry.createdAt)}")
            if (entry.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                FieldRow("备注", entry.notes, copyable = true, snack = snack, scope = scope)
            }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("历史密码 (${history.size})", style = MaterialTheme.typography.titleMedium)
                visibleHistory.forEach { h ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("••••••••")
                            Text(
                                BackupManager.formatDateTime(h.changedAt),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = {
                            PlatformActions.copyToClipboard(h.password)
                            scope.launch { snack.showSnackbar("已复制") }
                        }) { AppIcon(AppIcons.Copy, "复制") }
                    }
                }
                if (history.size > 3 && !showAllHistory) {
                    TextButton(onClick = { showAllHistory = true }) {
                        Text("查看全部 ${history.size} 个")
                    }
                }
            }
        }
    }

    if (confirmDelete == 1) {
        ConfirmDialog(
            title = "删除条目？",
            text = "此操作无法撤销",
            onDismiss = { confirmDelete = 0 },
            onConfirm = { confirmDelete = 2 },
        )
    }
    if (confirmDelete == 2) {
        ConfirmDialog(
            title = "再次确认",
            text = "确定删除「${entry.title}」？",
            onDismiss = { confirmDelete = 0 },
            onConfirm = {
                repo.delete(entry.id)
                onDeleted()
            },
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    copyable: Boolean,
    snack: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            SelectionContainer { Text(value) }
        }
        if (copyable) {
            IconButton(onClick = {
                PlatformActions.copyToClipboard(value)
                scope.launch { snack.showSnackbar("已复制") }
            }) { AppIcon(AppIcons.Copy, "复制") }
        }
    }
}
