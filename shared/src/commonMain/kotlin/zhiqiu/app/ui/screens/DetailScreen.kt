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
import androidx.compose.material3.TopAppBarDefaults
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
import zhiqiu.app.i18n.I18n
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
    val s = I18n.strings
    val entry = remember(id) { repo.getById(id) }
    var showPwd by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (entry == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(s.detail) },
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
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(s.entryMissing)
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
                title = { Text(s.detail) },
                navigationIcon = {
                    IconButton(onClick = onBack) { AppIcon(AppIcons.Back, s.cdBack) }
                },
                actions = {
                    IconButton(onClick = onEdit) { AppIcon(AppIcons.Edit, s.cdEdit) }
                    IconButton(onClick = { confirmDelete = 1 }) { AppIcon(AppIcons.Delete, s.cdDelete) }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconGlyph(entry.iconKey, Modifier.size(40.dp))
                Spacer(Modifier.padding(8.dp))
                Column {
                    Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        s.categoryLabel(entry.categoryEnum()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            FieldRow(s.username, entry.username, copyable = true, snack = snack, scope = scope)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(s.password, style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(if (showPwd) entry.password else s.masked)
                    }
                }
                PasswordVisibilityToggle(showPwd) { showPwd = !showPwd }
                IconButton(onClick = {
                    PlatformActions.copyToClipboard(entry.password)
                    scope.launch { snack.showSnackbar(s.copied) }
                }) { AppIcon(AppIcons.Copy, s.cdCopy) }
            }
            Spacer(Modifier.height(12.dp))
            Text("${s.category}: ${s.categoryLabel(entry.categoryEnum())}")
            Text("${s.created}: ${BackupManager.formatDateTime(entry.createdAt)}")
            if (entry.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                FieldRow(s.notes, entry.notes, copyable = true, snack = snack, scope = scope)
            }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(s.historyCount(history.size), style = MaterialTheme.typography.titleMedium)
                visibleHistory.forEach { h ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.masked)
                            Text(
                                BackupManager.formatDateTime(h.changedAt),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = {
                            PlatformActions.copyToClipboard(h.password)
                            scope.launch { snack.showSnackbar(s.copied) }
                        }) { AppIcon(AppIcons.Copy, s.cdCopy) }
                    }
                }
                if (history.size > 3 && !showAllHistory) {
                    TextButton(onClick = { showAllHistory = true }) {
                        Text(s.viewAllHistory(history.size))
                    }
                }
            }
        }
    }

    if (confirmDelete == 1) {
        ConfirmDialog(
            title = s.deleteTitle,
            text = s.irreversible,
            onDismiss = { confirmDelete = 0 },
            onConfirm = { confirmDelete = 2 },
        )
    }
    if (confirmDelete == 2) {
        ConfirmDialog(
            title = s.resetConfirmTitle,
            text = s.confirmDeleteEntry(entry.title),
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
    val s = I18n.strings
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
                scope.launch { snack.showSnackbar(s.copied) }
            }) { AppIcon(AppIcons.Copy, s.cdCopy) }
        }
    }
}
