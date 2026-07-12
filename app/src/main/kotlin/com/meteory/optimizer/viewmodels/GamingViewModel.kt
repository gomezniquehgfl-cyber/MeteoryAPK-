package com.meteory.optimizer.viewmodels

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.GameProfileDao
import com.meteory.optimizer.data.GameProfileEntity
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.utils.AdbCommands
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val name: String,
    val hasGameProfile: Boolean = false
)

data class GamingUiState(
    val isGamingModeOn: Boolean      = false,
    val renderer: String             = "auto",
    val refreshHz: Int               = 60,
    val dndEnabled: Boolean          = true,
    val hudEnabled: Boolean          = true,
    val backgroundKill: Boolean      = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val gameProfiles: List<GameProfileEntity> = emptyList(),
    val selectedApp: InstalledApp?   = null,
    val isScanningApps: Boolean      = false,
    val rendererTestResult: String   = "",
    val isTestingRenderer: Boolean   = false,
    val currentFps: Int              = 0,
    val currentCpu: Int              = 0,
    val currentTemp: Float           = 0f
)

@HiltViewModel
class GamingViewModel @Inject constructor(
    application: Application,
    private val gameProfileDao: GameProfileDao,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GamingUiState())
    val state: StateFlow<GamingUiState> = _state.asStateFlow()

    val availableHz = listOf(60, 90, 120, 144)

    init {
        viewModelScope.launch {
            prefs.gamingMode.collect { on ->
                _state.update { it.copy(isGamingModeOn = on) }
            }
        }
        viewModelScope.launch {
            prefs.renderer.collect { r ->
                _state.update { it.copy(renderer = r) }
            }
        }
        viewModelScope.launch {
            prefs.refreshHz.collect { hz ->
                _state.update { it.copy(refreshHz = hz) }
            }
        }
        viewModelScope.launch {
            gameProfileDao.getAllProfiles().collect { profiles ->
                _state.update { it.copy(gameProfiles = profiles) }
            }
        }
        startMetricsPolling()
    }

    fun scanInstalledApps() {
        viewModelScope.launch {
            _state.update { it.copy(isScanningApps = true) }
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
            val profilePkgs = _state.value.gameProfiles.map { it.packageName }.toSet()
            val apps = packages
                .filter { it.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { pkg ->
                    InstalledApp(
                        packageName   = pkg.packageName,
                        name          = pm.getApplicationLabel(pkg.applicationInfo!!).toString(),
                        hasGameProfile = pkg.packageName in profilePkgs
                    )
                }
                .sortedBy { it.name }

            _state.update { it.copy(installedApps = apps, isScanningApps = false) }
        }
    }

    fun selectApp(app: InstalledApp) = _state.update { it.copy(selectedApp = app) }

    fun saveGameProfile(pkg: String, name: String) {
        viewModelScope.launch {
            val existing = gameProfileDao.getProfile(pkg)
            val profile  = (existing ?: GameProfileEntity(packageName = pkg, appName = name))
                .copy(
                    renderer      = _state.value.renderer,
                    refreshHz     = _state.value.refreshHz,
                    dndEnabled    = _state.value.dndEnabled,
                    hudEnabled    = _state.value.hudEnabled,
                    backgroundKill = _state.value.backgroundKill,
                    updatedAt     = System.currentTimeMillis()
                )
            gameProfileDao.upsert(profile)
        }
    }

    fun deleteProfile(profile: GameProfileEntity) {
        viewModelScope.launch { gameProfileDao.delete(profile) }
    }

    fun toggleGamingMode() {
        viewModelScope.launch {
            val next = !_state.value.isGamingModeOn
            prefs.setGamingMode(next)
            if (next) {
                val cmds = AdbCommands.lockCpuPerf(SystemInfo.getCpuCoreCount()) +
                        listOf(AdbCommands.GPU_PERF_MODE, AdbCommands.DND_ON)
                cmds.forEach { ShizukuUtils.execBestEffort(it) }
            } else {
                val cmds = AdbCommands.resetCpuGov(SystemInfo.getCpuCoreCount()) +
                        listOf(AdbCommands.GPU_RESET_MODE, AdbCommands.DND_OFF)
                cmds.forEach { ShizukuUtils.execBestEffort(it) }
            }
        }
    }

    fun setRenderer(renderer: String) {
        viewModelScope.launch {
            prefs.setRenderer(renderer)
            val cmd = when (renderer) {
                "skiavk"   -> AdbCommands.FORCE_VULKAN
                "opengles" -> AdbCommands.FORCE_OPENGLES
                else       -> return@launch
            }
            ShizukuUtils.execBestEffort(cmd)
        }
    }

    fun autoSelectRenderer() {
        viewModelScope.launch {
            _state.update { it.copy(isTestingRenderer = true, rendererTestResult = "Probando Vulkan 30s...") }

            // Test Vulkan
            ShizukuUtils.execBestEffort(AdbCommands.FORCE_VULKAN)
            delay(30_000)
            val cpuVk  = SystemInfo.getCpuUsagePercent()
            val tempVk = SystemInfo.getCpuTemperature()

            _state.update { it.copy(rendererTestResult = "Probando OpenGL 30s...") }

            // Test OpenGL
            ShizukuUtils.execBestEffort(AdbCommands.FORCE_OPENGLES)
            delay(30_000)
            val cpuGl  = SystemInfo.getCpuUsagePercent()
            val tempGl = SystemInfo.getCpuTemperature()

            // Pick better (lower temp + cpu)
            val scoreVk = cpuVk + tempVk.toInt()
            val scoreGl = cpuGl + tempGl.toInt()
            val best    = if (scoreVk <= scoreGl) "skiavk" else "opengles"
            val label   = if (best == "skiavk") "Vulkan" else "OpenGL ES"

            prefs.setRenderer(best)
            _state.update {
                it.copy(
                    isTestingRenderer  = false,
                    renderer           = best,
                    rendererTestResult = "✅ Mejor: $label (CPU: ${if (best == "skiavk") cpuVk else cpuGl}%, Temp: ${"%.1f".format(if (best == "skiavk") tempVk else tempGl)}°C)"
                )
            }
        }
    }

    fun setRefreshHz(hz: Int) {
        viewModelScope.launch {
            prefs.setRefreshHz(hz)
            ShizukuUtils.execBestEffort(AdbCommands.setRefreshRate(hz))
            _state.update { it.copy(refreshHz = hz) }
        }
    }

    fun toggleDnd(v: Boolean)    = _state.update { it.copy(dndEnabled = v) }
    fun toggleHud(v: Boolean)    = _state.update { it.copy(hudEnabled = v) }
    fun toggleBgKill(v: Boolean) = _state.update { it.copy(backgroundKill = v) }

    private fun startMetricsPolling() {
        viewModelScope.launch {
            while (isActive) {
                val cpu  = SystemInfo.getCpuUsagePercent()
                val temp = SystemInfo.getCpuTemperature()
                _state.update { it.copy(currentCpu = cpu, currentTemp = temp) }
                delay(1500)
            }
        }
    }
}
