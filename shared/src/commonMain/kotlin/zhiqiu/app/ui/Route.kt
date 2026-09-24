package zhiqiu.app.ui

sealed interface Route {
    data object Boot : Route
    data object Setup : Route
    data object Unlock : Route
    data object Home : Route
    data object Settings : Route
    data class Detail(val id: String) : Route
    data class Edit(val id: String?) : Route
}
