package com.meteory.optimizer.utils

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

object SystemInfo {

    // ─────────────────────────────────────────────────────────────────────
    // CPU
    // ─────────────────────────────────────────────────────────────────────

    private var lastIdleCpu  = 0L
    private var lastTotalCpu = 0L

    suspend fun getCpuUsagePercent(): Int = withContext(Dispatchers.IO) {
        try {
            val lines = File("/proc/stat").readLines()
            val cpu   = lines.first { it.startsWith("cpu ") }
            val vals  = cpu.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            val idle  = vals[3] + vals[4]
            val total = vals.sum()

            val diffIdle  = idle  - lastIdleCpu
            val diffTotal = total - lastTotalCpu

            lastIdleCpu  = idle
            lastTotalCpu = total

            if (diffTotal == 0L) 0
            else ((diffTotal - diffIdle) * 100L / diffTotal).toInt().coerceIn(0, 100)
        } catch (e: Exception) { 0 }
    }

    fun getCpuCoreCount(): Int = Runtime.getRuntime().availableProcessors()

    suspend fun getCpuFrequencyMhz(core: Int = 0): Int = withContext(Dispatchers.IO) {
        val path = "/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq"
        ShellUtils.readFile(path)?.toLongOrNull()?.div(1000)?.toInt() ?: 0
    }

    suspend fun getCpuMaxFrequencyMhz(core: Int = 0): Int = withContext(Dispatchers.IO) {
        val path = "/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq"
        ShellUtils.readFile(path)?.toLongOrNull()?.div(1000)?.toInt() ?: 0
    }

    // ─────────────────────────────────────────────────────────────────────
    // RAM
    // ─────────────────────────────────────────────────────────────────────

    fun getRamInfo(context: Context): RamInfo {
        val am   = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return RamInfo(
            totalMb    = (info.totalMem / 1024 / 1024).toInt(),
            availableMb = (info.availMem / 1024 / 1024).toInt(),
            usedPercent = ((1.0 - info.availMem.toDouble() / info.totalMem) * 100).roundToInt()
        )
    }

    data class RamInfo(val totalMb: Int, val availableMb: Int, val usedPercent: Int) {
        val usedMb: Int get() = totalMb - availableMb
    }

    // ─────────────────────────────────────────────────────────────────────
    // Temperature
    // ─────────────────────────────────────────────────────────────────────

    suspend fun getCpuTemperature(): Float = withContext(Dispatchers.IO) {
        val paths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone7/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in paths) {
            val raw = ShellUtils.readFile(path)?.toFloatOrNull() ?: continue
            return@withContext if (raw > 1000f) raw / 1000f else raw
        }
        0f
    }

    suspend fun getBatteryTemperature(context: Context): Float = withContext(Dispatchers.IO) {
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        temp / 10f
    }

    // ─────────────────────────────────────────────────────────────────────
    // Battery
    // ─────────────────────────────────────────────────────────────────────

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val chargeType: String,
        val temperatureC: Float,
        val voltageV: Float,
        val health: String,
        val capacity: Long
    )

    fun getBatteryInfo(context: Context): BatteryInfo {
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        val level    = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale    = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status   = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged  = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val temp     = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val voltage  = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        val health   = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val chargeType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC      -> "CA (Rápida)"
            BatteryManager.BATTERY_PLUGGED_USB     -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inalámbrica"
            else -> "Desconectado"
        }

        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD         -> "Buena"
            BatteryManager.BATTERY_HEALTH_OVERHEAT     -> "Sobrecalentada"
            BatteryManager.BATTERY_HEALTH_DEAD         -> "Muerta"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobre Voltaje"
            else -> "Desconocida"
        }

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val capacity = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        return BatteryInfo(
            level        = level * 100 / scale,
            isCharging   = isCharging,
            chargeType   = chargeType,
            temperatureC = temp,
            voltageV     = voltage,
            health       = healthStr,
            capacity     = capacity
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Storage
    // ─────────────────────────────────────────────────────────────────────

    data class StorageInfo(val totalGb: Float, val freeGb: Float, val usedPercent: Int)

    fun getStorageInfo(): StorageInfo {
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val blockSize   = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val freeBlocks  = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes  = freeBlocks  * blockSize

        return StorageInfo(
            totalGb    = totalBytes / 1024f / 1024f / 1024f,
            freeGb     = freeBytes  / 1024f / 1024f / 1024f,
            usedPercent = ((1.0 - freeBytes.toDouble() / totalBytes) * 100).roundToInt()
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Network
    // ─────────────────────────────────────────────────────────────────────

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastNetTime = 0L

    data class NetworkSpeed(val downloadKbps: Long, val uploadKbps: Long)

    fun getNetworkSpeed(): NetworkSpeed {
        val now = System.currentTimeMillis()
        val rx  = TrafficStats.getTotalRxBytes()
        val tx  = TrafficStats.getTotalTxBytes()

        val elapsed = (now - lastNetTime).coerceAtLeast(1L)
        val dlKbps  = if (lastNetTime == 0L) 0L else (rx - lastRxBytes) * 1000L / elapsed / 1024L
        val ulKbps  = if (lastNetTime == 0L) 0L else (tx - lastTxBytes) * 1000L / elapsed / 1024L

        lastRxBytes = rx
        lastTxBytes = tx
        lastNetTime = now

        return NetworkSpeed(dlKbps.coerceAtLeast(0L), ulKbps.coerceAtLeast(0L))
    }

    fun getWifiSignalStrength(context: Context): Int {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return WifiManager.calculateSignalLevel(wm.connectionInfo.rssi, 5)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Device
    // ─────────────────────────────────────────────────────────────────────

    data class DeviceModel(
        val brand: String,
        val model: String,
        val androidVersion: String,
        val sdkInt: Int,
        val chipset: String
    )

    fun getDeviceModel(): DeviceModel = DeviceModel(
        brand          = Build.BRAND.replaceFirstChar { it.uppercase() },
        model          = Build.MODEL,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt         = Build.VERSION.SDK_INT,
        chipset        = Build.HARDWARE
    )

    // ─────────────────────────────────────────────────────────────────────
    // System score
    // ─────────────────────────────────────────────────────────────────────

    suspend fun computeSystemScore(context: Context): Int {
        val ram     = getRamInfo(context)
        val storage = getStorageInfo()
        val cpu     = getCpuUsagePercent()
        val temp    = getCpuTemperature()

        var score = 100
        score -= (ram.usedPercent / 5)
        score -= (storage.usedPercent / 10)
        score -= (cpu / 10)
        if (temp > 50f) score -= ((temp - 50f) * 1.5f).toInt()

        return score.coerceIn(0, 100)
    }
}
