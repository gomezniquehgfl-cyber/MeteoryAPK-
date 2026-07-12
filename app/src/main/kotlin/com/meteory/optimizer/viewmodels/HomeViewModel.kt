package com.meteory.optimizer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val cpuPercent: Int     = 0,
    val ramPercent: Int     = 0,
    val ramFreeMb: Int      = 0,
    val tempC: Float        = 0f,
    val batteryLevel: Int   = 100,
    val isCharging: Boolean = false,
    val systemScore: Int    = 80,
    val storageFreeGb: Float = 0f,
    val storageUsedPct: Int  = 0,
    val dlKbps: Long        = 0,
    val ulKbps: Long        = 0,
    val shizukuAvailable: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val darkTheme = prefs.darkTheme
    val gamingMode = prefs.gamingMode

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                val ctx      = getApplication<Application>()
                val cpu      = SystemInfo.getCpuUsagePercent()
                val ram      = SystemInfo.getRamInfo(ctx)
                val temp     = SystemInfo.getCpuTemperature()
                val battery  = SystemInfo.getBatteryInfo(ctx)
                val storage  = SystemInfo.getStorageInfo()
                val net      = SystemInfo.getNetworkSpeed()
                val score    = SystemInfo.computeSystemScore(ctx)

                _state.update {
                    it.copy(
                        cpuPercent    = cpu,
                        ramPercent    = ram.usedPercent,
                        ramFreeMb     = ram.availableMb,
                        tempC         = temp,
                        batteryLevel  = battery.level,
                        isCharging    = battery.isCharging,
                        systemScore   = score,
                        storageFreeGb = storage.freeGb,
                        storageUsedPct = storage.usedPercent,
                        dlKbps        = net.downloadKbps,
                        ulKbps        = net.uploadKbps,
                        shizukuAvailable = com.meteory.optimizer.utils.ShizukuUtils.isAvailable
                    )
                }
                delay(2000)
            }
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            prefs.darkTheme.first().let { prefs.setDarkTheme(!it) }
        }
    }
}
