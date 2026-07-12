package com.meteory.optimizer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.DeviceProfiles
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.utils.AdbCommands
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantUiState(
    val isValidated: Boolean       = false,
    val keyInput: String           = "",
    val validationError: String    = "",
    val messages: List<AssistantMessage> = emptyList(),
    val inputText: String          = "",
    val deviceModel: String        = "",
    val currentProfile: String     = "",
    val isProcessing: Boolean      = false
)

private const val VALID_KEY = "AQ.Ab8RN6ImI_JcS20AEsH768nBz2Vjt2G5Qlcvpw5fLr6ZqV9fSg"

@HiltViewModel
class AssistantViewModel @Inject constructor(
    application: Application,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.assistantValidated.collect { validated ->
                _state.update { it.copy(isValidated = validated) }
                if (validated) loadWelcome()
            }
        }
        val device = SystemInfo.getDeviceModel()
        _state.update { it.copy(deviceModel = "${device.brand} ${device.model}") }
    }

    fun updateKeyInput(v: String) = _state.update { it.copy(keyInput = v, validationError = "") }
    fun updateInput(v: String)    = _state.update { it.copy(inputText = v) }

    fun validateKey() {
        val key = _state.value.keyInput.trim()
        if (key == VALID_KEY) {
            viewModelScope.launch {
                prefs.setAssistantKey(key)
                prefs.setAssistantValidated(true)
            }
        } else {
            _state.update { it.copy(validationError = "Clave inválida. Verifica e intenta de nuevo.") }
        }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isProcessing) return

        _state.update {
            it.copy(
                messages = it.messages + AssistantMessage(text, isUser = true),
                inputText = "",
                isProcessing = true
            )
        }

        viewModelScope.launch {
            val response = processMessage(text)
            _state.update {
                it.copy(
                    messages = it.messages + AssistantMessage(response, isUser = false),
                    isProcessing = false
                )
            }
        }
    }

    private suspend fun processMessage(text: String): String {
        val lower = text.lowercase()

        // Detect model/brand to load profile
        val knownBrands = DeviceProfiles.getAllBrands()
        val matchedBrand = knownBrands.firstOrNull { it.lowercase() in lower }

        if (matchedBrand != null || lower.any { c -> c.isDigit() }) {
            val profile = DeviceProfiles.findProfile(text)
            prefs.setDeviceProfile(text)
            _state.update { it.copy(currentProfile = "${profile.brand} — perfil cargado") }

            // Apply profile
            val cmds = mutableListOf<String>()
            if (profile.cpuGovernorNormal.isNotBlank()) {
                val cores = SystemInfo.getCpuCoreCount()
                repeat(cores) { i ->
                    cmds += AdbCommands.setCpuGovernor(i, profile.cpuGovernorNormal)
                }
            }
            cmds.forEach { ShizukuUtils.execBestEffort(it) }

            return buildProfileResponse(profile, text)
        }

        return when {
            "fps" in lower || "gaming" in lower ->
                "Para mejorar FPS: activa **Modo Gaming Brutal** en la pestaña Gaming. Asegúrate de tener Shizuku activo para obtener resultados óptimos (CPU al máximo, GPU en modo performance, Vulkan forzado)."

            "temperatura" in lower || "calor" in lower ->
                "Control térmico: reduciré el gobernador CPU a un nivel conservador y monitorizaré la temperatura. Si supera ${_state.value.currentProfile.substringAfter("Temp: ").substringBefore("°")}°C ajustaré dinámicamente la carga."

            "ram" in lower || "memoria" in lower ->
                "RAM libre: ${SystemInfo.getRamInfo(getApplication()).availableMb}MB. Recomiendo: cerrar apps en segundo plano, deshabilitar servicios autoiniciados, y activar 'Liberar RAM' en la pestaña CPU/RAM."

            "batería" in lower || "bateria" in lower ->
                "Estado de batería: ${SystemInfo.getBatteryInfo(getApplication()).level}% — ${SystemInfo.getBatteryInfo(getApplication()).health}. Activa protección al 80% para prolongar vida útil del ciclo."

            "limpi" in lower ->
                "Iniciando análisis de limpieza... Ve a la pestaña **Limpieza** para ver todo el espacio recuperable y seleccionar qué limpiar."

            "vulkan" in lower ->
                "Forzando Vulkan (skiavk)... Mejor rendimiento gráfico en la mayoría de chips modernos. Si experimentas inestabilidad, cambia a OpenGL ES."

            "opengl" in lower ->
                "Forzando OpenGL ES... Más compatible con dispositivos MediaTek e Infinix/Tecno. Recomendado si Vulkan causa cierres."

            "shizuku" in lower ->
                if (ShizukuUtils.isAvailable) "✅ Shizuku está activo. Tengo acceso privilegiado para cambios avanzados de CPU/GPU/Red."
                else "❌ Shizuku no detectado. Instala Shizuku desde Play Store y actívalo vía ADB o Developer Options para desbloquear funciones avanzadas."

            "ayuda" in lower || "help" in lower ->
                """**Comandos disponibles:**
• [Marca/Modelo] → Cargar perfil específico del dispositivo
• "FPS / Gaming" → Optimización para juegos
• "RAM / Memoria" → Gestión de memoria
• "Temperatura / Calor" → Control térmico
• "Batería" → Análisis y protección
• "Vulkan / OpenGL" → Cambio de renderizador
• "Limpieza" → Análisis de almacenamiento
• "Shizuku" → Estado de acceso privilegiado"""

            else ->
                "Soy **Meteory Asistente FPS**, especializado en optimización y rendimiento. Escribe tu marca/modelo de dispositivo para cargar un perfil específico, o pregúntame sobre FPS, RAM, batería, temperatura o renderizado."
        }
    }

    private fun buildProfileResponse(
        profile: com.meteory.optimizer.data.DeviceProfile,
        query: String
    ) = """✅ **Configuración aplicada para ${profile.brand}**

📱 Dispositivo detectado: $query
⚙️ Governor CPU Gaming: `${profile.cpuGovernorGaming}`
⚙️ Governor CPU Normal: `${profile.cpuGovernorNormal}`
🎮 Renderizador recomendado: `${profile.preferredRenderer}`
🖥️ Tasa máxima soportada: **${profile.maxHz}Hz**
🌡️ Umbral térmico: **${profile.thermalThrottleTemp}°C**

💡 ${profile.notes}

Los ajustes han sido aplicados. Activa el **Modo Gaming Brutal** antes de jugar para máximo rendimiento."""

    private fun loadWelcome() {
        if (_state.value.messages.isNotEmpty()) return
        val device = SystemInfo.getDeviceModel()
        _state.update {
            it.copy(
                messages = listOf(
                    AssistantMessage(
                        content = """¡Bienvenido a **Meteory Asistente FPS**! 🚀

Dispositivo detectado: **${device.brand} ${device.model}** (Android ${device.androidVersion})

Puedo optimizar tu dispositivo para máximo rendimiento. Escribe tu modelo exacto para cargar un perfil personalizado, o pregúntame sobre:
• FPS y Gaming
• RAM y CPU
• Batería y temperatura
• Renderizado (Vulkan/OpenGL)

¿Cómo te puedo ayudar?""",
                        isUser = false
                    )
                )
            )
        }
    }
}
