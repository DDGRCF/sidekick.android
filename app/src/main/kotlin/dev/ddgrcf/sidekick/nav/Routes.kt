package dev.ddgrcf.sidekick.nav

sealed interface AppRoute {
    data object LoginRoute : AppRoute
    data class TotpRoute(val mfaTicket: String, val totpEnrolled: Boolean) : AppRoute
    data object HomeRoute : AppRoute
    data class BridgeRoute(val bridgeId: String, val bridgeName: String) : AppRoute
    data class SessionRoute(
        val bridgeId: String,
        val sid: String,
        val tool: String,
        val title: String,
        val convId: String,
        val structuredHistory: Boolean,
    ) : AppRoute
    data object PairRoute : AppRoute
    data object SettingsRoute : AppRoute
}
