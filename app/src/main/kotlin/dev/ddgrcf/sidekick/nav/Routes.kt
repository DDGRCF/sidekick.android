package dev.ddgrcf.sidekick.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object LoginRoute : AppRoute

    @Serializable
    data class TotpRoute(val mfaTicket: String, val totpEnrolled: Boolean) : AppRoute

    @Serializable
    data object HomeRoute : AppRoute

    @Serializable
    data class BridgeRoute(val bridgeId: String, val bridgeName: String) : AppRoute

    @Serializable
    data class SessionRoute(
        val bridgeId: String,
        val sid: String,
        val tool: String,
        val title: String,
        val convId: String,
        val structuredHistory: Boolean,
    ) : AppRoute

    @Serializable
    data object PairRoute : AppRoute

    @Serializable
    data object SettingsRoute : AppRoute

    @Serializable
    data object DemoRoute : AppRoute
}
