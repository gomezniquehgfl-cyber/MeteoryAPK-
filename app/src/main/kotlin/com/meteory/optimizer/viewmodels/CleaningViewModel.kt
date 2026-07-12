package com.meteory.optimizer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meteory.optimizer.data.CleaningHistoryDao
import com.meteory.optimizer.data.CleaningHistoryEntity
import com.meteory.optimizer.data.CleaningHistoryEntity as CleanEntry
import com.meteory.optimizer.data.PreferencesManager
import com.meteory.optimizer.utils.SystemInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CleanCategory(
    val name: String,
    val sizeMb: Long,
    val canClear: Boolean = true,
    val isSelected: Boolean = true
)

data class CleaningUiState(
    val categories: List<CleanCategory>  = emptyList(),
    val totalJunkMb: Long                = 0L,
    val isScanning: Boolean              = false,
    val isCleaning: Boolean              = false,
    val lastFreedMb: Long                = 0L,
    val totalFreedAllTime: Long          = 0L,
    val history: List<CleanEntry>        = emptyList(),
    val storageUsedPct: Int              = 0,
    val storageTotalGb: Float            = 0f,
    val storageFreeGb: Float             = 0f,
    val autoThreshold: Int               = 90,
    val showSuccess: Boolean             = false
)

@HiltViewModel
class CleaningViewModel @Inject constructor(
    application: Application,
    private val cleaningHistoryDao: CleaningHistoryDao,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CleaningUiState())
    val state: StateFlow<CleaningUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cleaningHistoryDao.getRecentHistory().collect { history ->
                val total = cleaningHistoryDao.totalFreedMb() ?: 0L
                _state.update { it.copy(history = history, totalFreedAllTime = total) }
            }
        }
        viewModelScope.launch {
            prefs.autoCleanThreshold.collect { threshold ->
                _state.update { it.copy(autoThreshold = threshold) }
            }
        }
        refreshStorage()
    }

    fun scan() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, categories = emptyList()) }
            val ctx  = getApplication<Application>()
            val cats = mutableListOf<CleanCategory>()

            // App cache
            val cacheMb = getDirSizeMb(ctx.cacheDir)
            if (cacheMb > 0) cats += CleanCategory("Caché de apps", cacheMb)

            // External cache
            ctx.externalCacheDirs.filterNotNull().forEach {
                val mb = getDirSizeMb(it)
                if (mb > 0) cats += CleanCategory("Caché externo", mb)
            }

            // Temp files
            val tempDir = File(ctx.filesDir, "temp")
            val tempMb  = getDirSizeMb(tempDir)
            if (tempMb > 0) cats += CleanCategory("Temporales", tempMb)

            // Download folder residuals (public)
            val dlDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val dlMb = getDirSizeMb(dlDir, filter = { f ->
                f.extension.lowercase() in listOf("apk", "tmp", "log", "part")
            })
            if (dlMb > 0) cats += CleanCategory("APKs/Temp en Descargas", dlMb)

            // Thumbnails
            val thumbDir = File(android.os.Environment.getExternalStorageDirectory(),
                "DCIM/.thumbnails")
            val thumbMb = getDirSizeMb(thumbDir)
            if (thumbMb > 0) cats += CleanCategory("Miniaturas antiguas", thumbMb)

            val total = cats.sumOf { it.sizeMb }
            _state.update {
                it.copy(isScanning = false, categories = cats, totalJunkMb = total)
            }
        }
    }

    fun clean() {
        viewModelScope.launch {
            _state.update { it.copy(isCleaning = true) }
            val ctx   = getApplication<Application>()
            var freed = 0L

            val selected = _state.value.categories.filter { it.isSelected }

            for (cat in selected) {
                when {
                    cat.name.contains("Caché de apps")  -> freed += clearDir(ctx.cacheDir)
                    cat.name.contains("Caché externo")  -> ctx.externalCacheDirs.forEach {
                        freed += clearDir(it ?: return@forEach)
                    }
                    cat.name.contains("Temporales") -> {
                        freed += clearDir(File(ctx.filesDir, "temp"))
                    }
                    cat.name.contains("APKs") -> {
                        val dlDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        freed += clearDir(dlDir, filter = { f ->
                            f.extension.lowercase() in listOf("apk", "tmp", "log", "part")
                        })
                    }
                    cat.name.contains("Miniaturas") -> {
                        freed += clearDir(File(android.os.Environment.getExternalStorageDirectory(),
                            "DCIM/.thumbnails"))
                    }
                }
            }

            val freedMb = freed / 1024 / 1024
            cleaningHistoryDao.insert(CleanEntry(freedMb = freedMb, type = "manual"))
            refreshStorage()

            _state.update {
                it.copy(
                    isCleaning   = false,
                    lastFreedMb  = freedMb,
                    categories   = emptyList(),
                    totalJunkMb  = 0L,
                    showSuccess  = true
                )
            }
        }
    }

    fun dismissSuccess() = _state.update { it.copy(showSuccess = false) }

    fun toggleCategory(index: Int) {
        val updated = _state.value.categories.toMutableList()
        updated[index] = updated[index].copy(isSelected = !updated[index].isSelected)
        _state.update { it.copy(categories = updated) }
    }

    fun setAutoThreshold(v: Int) {
        viewModelScope.launch { prefs.setAutoCleanThreshold(v) }
    }

    private fun refreshStorage() {
        val storage = SystemInfo.getStorageInfo()
        _state.update {
            it.copy(
                storageTotalGb  = storage.totalGb,
                storageFreeGb   = storage.freeGb,
                storageUsedPct  = storage.usedPercent
            )
        }
    }

    private fun getDirSizeMb(
        dir: File?,
        filter: (File) -> Boolean = { true }
    ): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { it.isFile && filter(it) }
            .sumOf { it.length() } / 1024 / 1024
    }

    private fun clearDir(dir: File?, filter: (File) -> Boolean = { true }): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.walkBottomUp().filter { it.isFile && filter(it) }.forEach {
            freed += it.length()
            it.delete()
        }
        return freed
    }
}
