package zhiqiu.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import zhiqiu.app.data.AppLockController
import zhiqiu.app.data.AppSession
import zhiqiu.app.data.PasswordRepository
import zhiqiu.app.data.ThemeMode
import zhiqiu.app.i18n.I18n
import zhiqiu.app.ui.AppBackground
import zhiqiu.app.ui.PlatformBackHandler
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
    var language by remember {
        mutableStateOf(repo.readLanguage().also { I18n.language = it })
    }
    var route by remember {
        mutableStateOf<Route>(
            when {
                !repo.hasMasterPassword() -> Route.Setup
                repo.biometricEnabled() -> Route.Unlock
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

    fun lockToUnlock() {
        if (!AppSession.session.isUnlocked) return
        if (!AppLockController.shouldLockNow()) return
        repo.lockNow()
        route = Route.Unlock
    }

    val windowInfo = LocalWindowInfo.current
    val lockLatest by rememberUpdatedState(::lockToUnlock)
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (windowInfo.isWindowFocused) return@LaunchedEffect
        delay(AppLockController.BACKGROUND_LOCK_DELAY_MS)
        if (!isActive) return@LaunchedEffect
        if (!windowInfo.isWindowFocused) lockLatest()
    }

    val canSystemBack = when (route) {
        is Route.Detail, is Route.Edit, Route.Settings -> true
        else -> false
    }
    PlatformBackHandler(enabled = canSystemBack) {
        when (val r = route) {
            is Route.Detail -> route = Route.Home
            is Route.Edit -> route = if (r.id != null) Route.Detail(r.id) else Route.Home
            Route.Settings -> route = Route.Home
            else -> Unit
        }
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
                    language = language,
                    onThemeChanged = {
                        themeMode = it
                        repo.writeTheme(it)
                    },
                    onLanguageChanged = {
                        language = it
                        I18n.language = it
                        repo.writeLanguage(it)
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
