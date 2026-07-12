package com.meteory.optimizer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("meteory_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    companion object {
        val KEY_DARK_THEME         = booleanPreferencesKey("dark_theme")
        val KEY_ASSISTANT_KEY      = stringPreferencesKey("assistant_key")
        val KEY_ASSISTANT_VALIDATED = booleanPreferencesKey("assistant_validated")
        val KEY_CURRENT_PROFILE    = stringPreferencesKey("current_profile")
        val KEY_HUD_ENABLED        = booleanPreferencesKey("hud_enabled")
        val KEY_HUD_OPACITY        = floatPreferencesKey("hud_opacity")
        val KEY_HUD_POSITION_X     = floatPreferencesKey("hud_pos_x")
        val KEY_HUD_POSITION_Y     = floatPreferencesKey("hud_pos_y")
        val KEY_GAMING_MODE        = booleanPreferencesKey("gaming_mode")
        val KEY_RENDERER           = stringPreferencesKey("renderer")
        val KEY_REFRESH_HZ         = intPreferencesKey("refresh_hz")
        val KEY_DEVICE_PROFILE     = stringPreferencesKey("device_profile")
        val KEY_AUTO_CLEAN_THRESHOLD = intPreferencesKey("auto_clean_threshold")
        val KEY_BATTERY_PROTECT    = booleanPreferencesKey("battery_protect")
        val KEY_BATTERY_PROTECT_PCT = intPreferencesKey("battery_protect_pct")
        val KEY_PERFORMANCE_PROFILE = stringPreferencesKey("performance_profile")
        val KEY_ONBOARDED          = booleanPreferencesKey("onboarded")
    }

    val darkTheme: Flow<Boolean>     = store.data.map { it[KEY_DARK_THEME] ?: true }
    val assistantKey: Flow<String>   = store.data.map { it[KEY_ASSISTANT_KEY] ?: "" }
    val assistantValidated: Flow<Boolean> = store.data.map { it[KEY_ASSISTANT_VALIDATED] ?: false }
    val hudEnabled: Flow<Boolean>    = store.data.map { it[KEY_HUD_ENABLED] ?: false }
    val hudOpacity: Flow<Float>      = store.data.map { it[KEY_HUD_OPACITY] ?: 0.85f }
    val gamingMode: Flow<Boolean>    = store.data.map { it[KEY_GAMING_MODE] ?: false }
    val renderer: Flow<String>       = store.data.map { it[KEY_RENDERER] ?: "auto" }
    val refreshHz: Flow<Int>         = store.data.map { it[KEY_REFRESH_HZ] ?: 60 }
    val deviceProfile: Flow<String>  = store.data.map { it[KEY_DEVICE_PROFILE] ?: "" }
    val autoCleanThreshold: Flow<Int>= store.data.map { it[KEY_AUTO_CLEAN_THRESHOLD] ?: 90 }
    val batteryProtect: Flow<Boolean>= store.data.map { it[KEY_BATTERY_PROTECT] ?: false }
    val batteryProtectPct: Flow<Int> = store.data.map { it[KEY_BATTERY_PROTECT_PCT] ?: 80 }
    val performanceProfile: Flow<String> = store.data.map { it[KEY_PERFORMANCE_PROFILE] ?: "balanced" }
    val onboarded: Flow<Boolean>     = store.data.map { it[KEY_ONBOARDED] ?: false }

    suspend fun setDarkTheme(v: Boolean)         = store.edit { it[KEY_DARK_THEME] = v }
    suspend fun setAssistantKey(v: String)        = store.edit { it[KEY_ASSISTANT_KEY] = v }
    suspend fun setAssistantValidated(v: Boolean) = store.edit { it[KEY_ASSISTANT_VALIDATED] = v }
    suspend fun setHudEnabled(v: Boolean)         = store.edit { it[KEY_HUD_ENABLED] = v }
    suspend fun setHudOpacity(v: Float)           = store.edit { it[KEY_HUD_OPACITY] = v }
    suspend fun setHudPosition(x: Float, y: Float) = store.edit {
        it[KEY_HUD_POSITION_X] = x; it[KEY_HUD_POSITION_Y] = y
    }
    suspend fun setGamingMode(v: Boolean)         = store.edit { it[KEY_GAMING_MODE] = v }
    suspend fun setRenderer(v: String)            = store.edit { it[KEY_RENDERER] = v }
    suspend fun setRefreshHz(v: Int)              = store.edit { it[KEY_REFRESH_HZ] = v }
    suspend fun setDeviceProfile(v: String)       = store.edit { it[KEY_DEVICE_PROFILE] = v }
    suspend fun setAutoCleanThreshold(v: Int)     = store.edit { it[KEY_AUTO_CLEAN_THRESHOLD] = v }
    suspend fun setBatteryProtect(v: Boolean)     = store.edit { it[KEY_BATTERY_PROTECT] = v }
    suspend fun setBatteryProtectPct(v: Int)      = store.edit { it[KEY_BATTERY_PROTECT_PCT] = v }
    suspend fun setPerformanceProfile(v: String)  = store.edit { it[KEY_PERFORMANCE_PROFILE] = v }
    suspend fun setOnboarded(v: Boolean)          = store.edit { it[KEY_ONBOARDED] = v }
}
