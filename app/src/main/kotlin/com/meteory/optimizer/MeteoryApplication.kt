package com.meteory.optimizer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeteoryApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        listOf(
            NotificationChannel(
                CHANNEL_HUD,
                getString(R.string.channel_hud),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) },
            NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) },
            NotificationChannel(
                CHANNEL_GAMING,
                getString(R.string.channel_gaming),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        ).forEach(manager::createNotificationChannel)
    }

    companion object {
        const val CHANNEL_HUD     = "meteory_hud"
        const val CHANNEL_MONITOR = "meteory_monitor"
        const val CHANNEL_GAMING  = "meteory_gaming"
    }
}
