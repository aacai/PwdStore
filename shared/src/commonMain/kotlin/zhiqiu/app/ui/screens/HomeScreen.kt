package zhiqiu.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zhiqiu.app.data.PasswordCategory
import zhiqiu.app.data.PasswordEntry
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.components.AppIcon
import zhiqiu.app.ui.components.AppIcons
import zhiqiu.app.ui.components.EntryListCard
import zhiqiu.app.ui.components.IconGlyph
import zhiqiu.app.ui.components.ThemeToggleIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: PasswordRepository,
    refreshKey: Int,
    onOpenSettings: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAdd: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val s = I18n.strings
    var entries by remember { mutableStateOf<List<PasswordEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PasswordCategory?>(null) }

    fun reload() {
        runCatching { entries = repo.getAllDecrypted(); error = null }
            .onFailure { error = it.message ?: s.loadFailed }
    }

    LaunchedEffect(refreshKey) { reload() }

    val filtered = remember(entries, query, category) {
        entries.filter { e ->
            val q = query.trim()
            val matchQ = q.isEmpty() ||
                e.title.contains(q, true) ||
                e.username.contains(q, true)
            val matchC = category == null || e.categoryEnum() == category
            matchQ && matchC
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.vault)
                        Text(
                            s.recordCount(entries.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) { ThemeToggleIcon() }
                    IconButton(onClick = onOpenSettings) { AppIcon(AppIcons.Settings, s.cdSettings) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            ) { AppIcon(AppIcons.Add, s.cdAdd) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(s.searchHint) },
                singleLine = true,
                leadingIcon = { AppIcon(AppIcons.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text(s.filterAll) },
                    shape = MaterialTheme.shapes.small,
                )
                PasswordCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) null else c },
                        label = { Text(s.categoryLabel(c)) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                error != null -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { reload() }) { Text(s.retry) }
                }
                entries.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(AppIcons.Key, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(s.emptyTitle, style = MaterialTheme.typography.titleMedium)
                    Text(s.emptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                filtered.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(s.noMatch)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { query = ""; category = null }) { Text(s.clearFilters) }
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(filtered, key = { it.id }) { e ->
                        EntryListCard(
                            title = e.title,
                            username = e.username,
                            notes = e.notes,
                            icon = { IconGlyph(e.iconKey, Modifier.size(22.dp)) },
                            onClick = { onOpenDetail(e.id) },
                        )
                    }
                }
            }
        }
    }
}
