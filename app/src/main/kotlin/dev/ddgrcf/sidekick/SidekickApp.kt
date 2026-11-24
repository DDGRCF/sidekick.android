package dev.ddgrcf.sidekick

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SidekickApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_RELAY,
            "Relay Connection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Maintains end-to-end encrypted connection to Sidekick bridge"
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_RELAY = "sidekick_relay"
    }
}
