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
    var entries by remember { mutableStateOf<List<PasswordEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PasswordCategory?>(null) }

    fun reload() {
        runCatching { entries = repo.getAllDecrypted(); error = null }
            .onFailure { error = it.message ?: "加载失败" }
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
                        Text("密码库")
                        Text(
                            "${entries.size} 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) { ThemeToggleIcon() }
                    IconButton(onClick = onOpenSettings) { AppIcon(AppIcons.Settings, "设置") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            ) { AppIcon(AppIcons.Add, "添加") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索名称/用户名") },
                singleLine = true,
                leadingIcon = { AppIcon(AppIcons.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("全部") },
                    shape = MaterialTheme.shapes.small,
                )
                PasswordCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) null else c },
                        label = { Text(c.label) },
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
                    Button(onClick = { reload() }) { Text("重试") }
                }
                entries.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(AppIcons.Key, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("还没有添加任何密码", style = MaterialTheme.typography.titleMedium)
                    Text("点右下角开始添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                filtered.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("没有找到匹配的密码")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { query = ""; category = null }) { Text("清除筛选") }
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
