package com.meteory.optimizer.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.meteory.optimizer.MeteoryApplication
import com.meteory.optimizer.R
import com.meteory.optimizer.data.GameProfileDao
import com.meteory.optimizer.data.GameProfileEntity
import com.meteory.optimizer.utils.AdbCommands
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class GamingService : Service() {

    @Inject lateinit var gameProfileDao: GameProfileDao

    private val scope         = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeGame: String? = null
    private val coreCount     = SystemInfo.getCpuCoreCount()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        startGameMonitor()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        activeGame?.let { scope.launch { restoreNormalMode() } }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, MeteoryApplication.CHANNEL_GAMING)
            .setContentTitle(getString(R.string.notification_gaming_title))
            .setContentText(getString(R.string.notification_gaming_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun startGameMonitor() {
        scope.launch {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val now    = System.currentTimeMillis()
                val stats  = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, now - 5000, now
                )
                val current = stats
                    .filter { it.lastTimeUsed > now - 3000 }
                    .maxByOrNull { it.lastTimeUsed }
                    ?.packageName

                if (current != null && current != activeGame) {
                    val profile = gameProfileDao.getProfile(current)
                    if (profile != null) {
                        restoreNormalMode()
                        activeGame = current
                        applyGamingMode(profile)
                    } else if (activeGame != null && current != activeGame) {
                        restoreNormalMode()
                        activeGame = null
                    }
                }
                delay(2000)
            }
        }
    }

    private suspend fun applyGamingMode(profile: GameProfileEntity) {
        val cmds = buildList {
            // CPU performance
            addAll(AdbCommands.lockCpuPerf(coreCount))
            // GPU
            add(AdbCommands.GPU_PERF_MODE)
            // Renderer
            when (profile.renderer) {
                "skiavk"  -> add(AdbCommands.FORCE_VULKAN)
                "opengles"-> add(AdbCommands.FORCE_OPENGLES)
            }
            // Network
            add(AdbCommands.TCP_NO_DELAY)
            add(AdbCommands.TCP_FAST_OPEN)
            add(AdbCommands.WIFI_SCAN_ALWAYS)
            // Animations
            add(AdbCommands.ANIMATIONS_OFF)
            add(AdbCommands.TRANSITIONS_OFF)
            // DnD
            if (profile.dndEnabled) add(AdbCommands.DND_ON)
            // RAM
            add(AdbCommands.DROP_CACHES)
        }
        cmds.forEach { ShizukuUtils.execBestEffort(it) }

        // Start HUD if enabled
        if (profile.hudEnabled) HudOverlayService.start(this)
    }

    private suspend fun restoreNormalMode() {
        val cmds = buildList {
            addAll(AdbCommands.resetCpuGov(coreCount))
            add(AdbCommands.GPU_RESET_MODE)
            add(AdbCommands.RESET_RENDERER)
            add(AdbCommands.DND_OFF)
            add(AdbCommands.ANIMATIONS_RESET)
            add(AdbCommands.TRANSITIONS_RESET)
        }
        cmds.forEach { ShizukuUtils.execBestEffort(it) }
        HudOverlayService.stop(this)
    }

    companion object {
        private const val NOTIF_ID = 1003

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GamingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GamingService::class.java))
        }
    }
}
