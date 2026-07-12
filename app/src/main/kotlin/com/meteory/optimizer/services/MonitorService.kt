package com.meteory.optimizer.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.meteory.optimizer.MeteoryApplication
import com.meteory.optimizer.R
import com.meteory.optimizer.data.BatteryHistoryDao
import com.meteory.optimizer.data.BatteryHistoryEntity
import com.meteory.optimizer.data.SystemHealthLogDao
import com.meteory.optimizer.data.SystemHealthLogEntity
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MonitorService : Service() {

    @Inject lateinit var healthLogDao: SystemHealthLogDao
    @Inject lateinit var batteryHistoryDao: BatteryHistoryDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, MeteoryApplication.CHANNEL_MONITOR)
            .setContentTitle(getString(R.string.notification_monitor_title))
            .setContentText(getString(R.string.notification_monitor_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    val cpu   = SystemInfo.getCpuUsagePercent()
                    val ram   = SystemInfo.getRamInfo(this@MonitorService)
                    val temp  = SystemInfo.getCpuTemperature()
                    val score = SystemInfo.computeSystemScore(this@MonitorService)

                    healthLogDao.insert(
                        SystemHealthLogEntity(
                            score       = score,
                            cpuUsage    = cpu,
                            ramUsageMb  = ram.usedMb,
                            temperatureC = temp
                        )
                    )

                    val battery = SystemInfo.getBatteryInfo(this@MonitorService)
                    batteryHistoryDao.insert(
                        BatteryHistoryEntity(
                            level        = battery.level,
                            temperatureC = battery.temperatureC,
                            isCharging   = battery.isCharging
                        )
                    )

                    // Prune old data
                    val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                    healthLogDao.deleteOlderThan(cutoff)
                    batteryHistoryDao.deleteOlderThan(cutoff)

                } catch (e: Exception) { /* non-fatal */ }

                delay(30_000)
            }
        }
    }

    companion object {
        private const val NOTIF_ID = 1002

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
