package zhiqiu.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.ThemeMode
import zhiqiu.app.ui.AppBackground
import zhiqiu.app.ui.PwdTheme
import zhiqiu.app.ui.Route
import zhiqiu.app.ui.screens.DetailScreen
import zhiqiu.app.ui.screens.EditPasswordScreen
import zhiqiu.app.ui.screens.HomeScreen
import zhiqiu.app.ui.screens.SettingsScreen
import zhiqiu.app.ui.screens.SetupMasterPasswordScreen
import zhiqiu.app.ui.screens.UnlockScreen

@Composable
fun App() {
    val repo = remember { PasswordRepository() }
    var themeMode by remember { mutableStateOf(repo.readTheme()) }
    var route by remember {
        mutableStateOf<Route>(
            when {
                !repo.hasMasterPassword() -> Route.Setup
                repo.tryPasswordFreeUnlock() -> Route.Home
                else -> Route.Unlock
            },
        )
    }
    var listEpoch by remember { mutableStateOf(0) }

    fun toggleThemeQuick() {
        themeMode = when (themeMode) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            else -> ThemeMode.DARK
        }
        repo.writeTheme(themeMode)
    }

    PwdTheme(themeMode) {
        AppBackground {
            when (val r = route) {
                Route.Boot, Route.Setup -> SetupMasterPasswordScreen(
                    repo = repo,
                    onDone = { route = Route.Home },
                )
                Route.Unlock -> UnlockScreen(
                    repo = repo,
                    onUnlocked = { route = Route.Home },
                    onReset = { route = Route.Setup },
                    onToggleTheme = ::toggleThemeQuick,
                )
                Route.Home -> HomeScreen(
                    repo = repo,
                    refreshKey = listEpoch,
                    onOpenSettings = { route = Route.Settings },
                    onOpenDetail = { route = Route.Detail(it) },
                    onAdd = { route = Route.Edit(null) },
                    onToggleTheme = ::toggleThemeQuick,
                )
                Route.Settings -> SettingsScreen(
                    repo = repo,
                    themeMode = themeMode,
                    onThemeChanged = {
                        themeMode = it
                        repo.writeTheme(it)
                    },
                    onBack = { route = Route.Home },
                    onLocked = { route = Route.Unlock },
                    onListChanged = { listEpoch++ },
                )
                is Route.Detail -> DetailScreen(
                    repo = repo,
                    id = r.id,
                    onBack = { route = Route.Home },
                    onEdit = { route = Route.Edit(r.id) },
                    onDeleted = {
                        listEpoch++
                        route = Route.Home
                    },
                )
                is Route.Edit -> EditPasswordScreen(
                    repo = repo,
                    editId = r.id,
                    onBack = {
                        route = if (r.id != null) Route.Detail(r.id) else Route.Home
                    },
                    onSaved = {
                        listEpoch++
                        route = if (r.id != null) Route.Detail(r.id) else Route.Home
                    },
                )
            }
        }
    }
}
