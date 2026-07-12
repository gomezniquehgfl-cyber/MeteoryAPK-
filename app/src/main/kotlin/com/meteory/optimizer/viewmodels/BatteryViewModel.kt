package com.meteory.optimizer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.BatteryHistoryDao
import com.meteory.optimizer.data.BatteryHistoryEntity
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatteryUiState(
    val level: Int              = 100,
    val isCharging: Boolean     = false,
    val chargeType: String      = "Desconectado",
    val tempC: Float            = 0f,
    val voltageV: Float         = 0f,
    val health: String          = "Buena",
    val capacityMah: Long       = 0L,
    val estimatedMinutes: Int   = 0,
    val batteryMode: String     = "balanced",
    val protectEnabled: Boolean = false,
    val protectPct: Int         = 80,
    val levelHistory: List<BatteryHistoryEntity> = emptyList(),
    val wakelocksInfo: String   = "Obteniendo datos..."
)

@HiltViewModel
class BatteryViewModel @Inject constructor(
    application: Application,
    private val batteryHistoryDao: BatteryHistoryDao,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BatteryUiState())
    val state: StateFlow<BatteryUiState> = _state.asStateFlow()

    val batteryModes = listOf(
        "balanced"    to "⚖️ Equilibrado",
        "saver"       to "🍃 Ahorro",
        "high_saver"  to "💤 Alto Ahorro",
        "ultra_saver" to "🔋 Ultra Ahorro"
    )

    init {
        viewModelScope.launch {
            batteryHistoryDao.getRecentHistory().collect { history ->
                _state.update { it.copy(levelHistory = history) }
            }
        }
        viewModelScope.launch {
            prefs.batteryProtect.collect { v -> _state.update { it.copy(protectEnabled = v) } }
        }
        viewModelScope.launch {
            prefs.batteryProtectPct.collect { v -> _state.update { it.copy(protectPct = v) } }
        }
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                val ctx     = getApplication<Application>()
                val battery = SystemInfo.getBatteryInfo(ctx)
                val est     = estimateMinutesRemaining(battery.level, battery.isCharging)

                _state.update {
                    it.copy(
                        level           = battery.level,
                        isCharging      = battery.isCharging,
                        chargeType      = battery.chargeType,
                        tempC           = battery.temperatureC,
                        voltageV        = battery.voltageV,
                        health          = battery.health,
                        capacityMah     = battery.capacity / 1000,
                        estimatedMinutes = est
                    )
                }
                delay(10_000)
            }
        }
    }

    fun setBatteryMode(mode: String) {
        viewModelScope.launch {
            _state.update { it.copy(batteryMode = mode) }
        }
    }

    fun setBatteryProtect(enabled: Boolean) {
        viewModelScope.launch { prefs.setBatteryProtect(enabled) }
    }

    fun setBatteryProtectPct(pct: Int) {
        viewModelScope.launch { prefs.setBatteryProtectPct(pct) }
    }

    private fun estimateMinutesRemaining(level: Int, charging: Boolean): Int {
        return if (charging) (100 - level) * 2 else level * 5
    }
}
