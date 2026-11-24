package dev.ddgrcf.sidekick.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.AndroidEntryPoint
import dev.ddgrcf.sidekick.SidekickApp
import dev.ddgrcf.sidekick.core.relay.RelaySocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RelayForegroundService : Service() {
    @Inject
    lateinit var socketManager: RelaySocketManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, SidekickApp.CHANNEL_RELAY)
            .setContentTitle("Sidekick Remote")
            .setContentText("Encrypted bridge tunnel active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        startForeground(101, notification)

        val pm = getSystemService(PowerManager::class.java)
        if (wakeLock == null && pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sidekick:relay").apply {
                acquire(10 * 60 * 1000L)
            }
        }
        socketManager.start(scope)
        return START_STICKY
    }

    override fun onDestroy() {
        socketManager.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RelayForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RelayForegroundService::class.java))
        }

        fun scheduleReconnectWorker(context: Context) {
            val req = PeriodicWorkRequestBuilder<ReconnectWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sidekick_reconnect",
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}

class ReconnectWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}
