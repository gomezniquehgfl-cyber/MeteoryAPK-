package com.meteory.optimizer.viewmodels

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.data.SystemHealthLogDao
import com.meteory.optimizer.data.SystemHealthLogEntity
import com.meteory.optimizer.utils.AdbCommands
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessInfo(
    val name: String,
    val packageName: String,
    val pid: Int,
    val memoryMb: Int
)

data class PerformanceUiState(
    val cpuPercent: Int          = 0,
    val cpuFreqMhz: Int          = 0,
    val cpuMaxMhz: Int           = 0,
    val cpuCores: Int            = 0,
    val ramInfo: SystemInfo.RamInfo = SystemInfo.RamInfo(0, 0, 0),
    val tempC: Float             = 0f,
    val systemScore: Int         = 80,
    val profile: String          = "balanced",
    val cpuHistory: List<Int>    = emptyList(),
    val ramHistory: List<Int>    = emptyList(),
    val runningProcesses: List<ProcessInfo> = emptyList(),
    val isKillingBg: Boolean     = false,
    val healthLogs: List<SystemHealthLogEntity> = emptyList(),
    val healthActions: List<String> = emptyList()
)

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    application: Application,
    private val prefs: PreferencesManager,
    private val healthLogDao: SystemHealthLogDao
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PerformanceUiState())
    val state: StateFlow<PerformanceUiState> = _state.asStateFlow()

    val profiles = listOf("balanced" to "⚖️ Equilibrado", "performance" to "🚀 Velocidad Máxima", "eco" to "🍃 Ahorro")

    init {
        viewModelScope.launch {
            prefs.performanceProfile.collect { p -> _state.update { it.copy(profile = p) } }
        }
        viewModelScope.launch {
            healthLogDao.getRecentLogs().collect { logs ->
                _state.update { it.copy(healthLogs = logs) }
            }
        }
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                val ctx    = getApplication<Application>()
                val cpu    = SystemInfo.getCpuUsagePercent()
                val freq   = SystemInfo.getCpuFrequencyMhz()
                val maxF   = SystemInfo.getCpuMaxFrequencyMhz()
                val ram    = SystemInfo.getRamInfo(ctx)
                val temp   = SystemInfo.getCpuTemperature()
                val score  = SystemInfo.computeSystemScore(ctx)

                val cpuHist = (_state.value.cpuHistory + cpu).takeLast(30)
                val ramHist = (_state.value.ramHistory + ram.usedPercent).takeLast(30)

                val actions = buildHealthActions(cpu, ram, temp, score)

                _state.update {
                    it.copy(
                        cpuPercent  = cpu,
                        cpuFreqMhz  = freq,
                        cpuMaxMhz   = maxF,
                        cpuCores    = SystemInfo.getCpuCoreCount(),
                        ramInfo     = ram,
                        tempC       = temp,
                        systemScore = score,
                        cpuHistory  = cpuHist,
                        ramHistory  = ramHist,
                        healthActions = actions
                    )
                }
                delay(2000)
            }
        }
    }

    fun applyProfile(profile: String) {
        viewModelScope.launch {
            prefs.setPerformanceProfile(profile)
            val cmds = when (profile) {
                "performance" -> AdbCommands.lockCpuPerf(SystemInfo.getCpuCoreCount()) +
                        listOf(AdbCommands.GPU_PERF_MODE, AdbCommands.SET_SWAPPINESS_LOW)
                "eco"         -> AdbCommands.resetCpuGov(SystemInfo.getCpuCoreCount()) +
                        listOf(AdbCommands.GPU_RESET_MODE)
                else          -> AdbCommands.resetCpuGov(SystemInfo.getCpuCoreCount())
            }
            cmds.forEach { ShizukuUtils.execBestEffort(it) }
        }
    }

    fun killBackgroundApps() {
        viewModelScope.launch {
            _state.update { it.copy(isKillingBg = true) }
            val am = getApplication<Application>()
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val tasks = am.getRunningAppProcesses() ?: emptyList()
            tasks.filter {
                it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                it.pkgList?.none { pkg -> pkg == getApplication<Application>().packageName } == true
            }.forEach { proc ->
                proc.pkgList?.forEach { pkg ->
                    am.killBackgroundProcesses(pkg)
                }
            }
            delay(1000)
            _state.update { it.copy(isKillingBg = false) }
        }
    }

    fun freeRam() {
        viewModelScope.launch {
            ShizukuUtils.execBestEffort(AdbCommands.DROP_CACHES)
            ShizukuUtils.execBestEffort(AdbCommands.COMPACT_MEMORY)
        }
    }

    private fun buildHealthActions(cpu: Int, ram: SystemInfo.RamInfo, temp: Float, score: Int): List<String> {
        val actions = mutableListOf<String>()
        if (cpu > 70)              actions += "Cerrar apps en segundo plano"
        if (ram.usedPercent > 80)  actions += "Liberar RAM — ${ram.availableMb}MB disponible"
        if (temp > 55f)            actions += "Temperatura alta ${temp.toInt()}°C — reducir carga"
        if (ram.usedPercent > 70)  actions += "Deshabilitar apps de arranque innecesarias"
        if (score < 60)            actions += "Limpiar caché del sistema"
        if (actions.isEmpty())     actions += "✅ Sistema en buen estado"
        return actions
    }
}
