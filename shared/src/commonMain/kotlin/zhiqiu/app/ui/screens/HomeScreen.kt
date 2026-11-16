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
import zhiqiu.app.ui.components.Button
import zhiqiu.applewidgets.kmp.basic.Chip
import zhiqiu.applewidgets.kmp.basic.FloatingActionButton
import zhiqiu.applewidgets.kmp.basic.FloatingActionButtonDefaults
import zhiqiu.applewidgets.kmp.basic.IconButton
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.applewidgets.kmp.theme.AppleWidgetsTheme
import zhiqiu.app.ui.components.OutlinedTextField
import zhiqiu.app.ui.components.AppScaffold as Scaffold
import zhiqiu.applewidgets.kmp.basic.Text
import zhiqiu.app.ui.components.AppTopBar as TopAppBar
import zhiqiu.app.ui.components.AppTopBarDefaults as TopAppBarDefaults
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
import zhiqiu.app.ui.screenWindowInsets

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
        contentWindowInsets = screenWindowInsets(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.vault)
                        Text(
                            s.recordCount(entries.size),
                            style = OinkTheme.textStyles.caption,
                            color = OinkTheme.colors.text2,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) { ThemeToggleIcon() }
                    IconButton(onClick = onOpenSettings) { AppIcon(AppIcons.Settings, s.cdSettings) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OinkTheme.colors.bg,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                colors = FloatingActionButtonDefaults.colors(
                    containerColor = OinkTheme.colors.accent,
                    contentColor = OinkTheme.colors.onAccent,
                ),
            ) { AppIcon(AppIcons.Add, s.cdAdd) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(s.searchHint) },
                singleLine = true,
                leadingIcon = AppIcons.Search,
                modifier = Modifier.fillMaxWidth(),
                shape = AppleWidgetsTheme.componentStyles.controlShape,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    text = s.filterAll,
                    selected = category == null,
                    onClick = { category = null },
                )
                PasswordCategory.entries.forEach { c ->
                    Chip(
                        text = s.categoryLabel(c),
                        selected = category == c,
                        onClick = { category = if (category == c) null else c },
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
                    AppIcon(AppIcons.Key, null, Modifier.size(48.dp), tint = OinkTheme.colors.accent)
                    Spacer(Modifier.height(12.dp))
                    Text(s.emptyTitle, style = OinkTheme.textStyles.h4)
                    Text(s.emptyHint, color = OinkTheme.colors.text2)
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
                            hasNotes = e.notes.isNotBlank(),
                            icon = { IconGlyph(e.iconKey, Modifier.size(22.dp)) },
                            onClick = { onOpenDetail(e.id) },
                        )
                    }
                }
            }
        }
    }
}
