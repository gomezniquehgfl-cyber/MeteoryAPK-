package com.meteory.optimizer.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.meteory.optimizer.data.CleaningHistoryDao
import com.meteory.optimizer.data.CleaningHistoryEntity
import com.meteory.optimizer.utils.ShizukuUtils
import com.meteory.optimizer.utils.SystemInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class CleaningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cleaningHistoryDao: CleaningHistoryDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val freed = performCleaning()
            cleaningHistoryDao.insert(
                CleaningHistoryEntity(freedMb = freed, type = "auto")
            )
            Result.success(workDataOf("freed_mb" to freed))
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun performCleaning(): Long {
        var totalFreed = 0L

        // Clear app caches
        val cacheDir = applicationContext.cacheDir
        totalFreed += clearDirectory(cacheDir)

        // Clear external cache
        applicationContext.externalCacheDirs.forEach {
            totalFreed += clearDirectory(it ?: return@forEach)
        }

        // Drop system caches via Shizuku if available
        ShizukuUtils.execBestEffort("sync; echo 3 > /proc/sys/vm/drop_caches")

        // Clear temp directories
        val tempDir = File(applicationContext.filesDir, "temp")
        if (tempDir.exists()) totalFreed += clearDirectory(tempDir)

        return totalFreed / 1024 / 1024
    }

    private fun clearDirectory(dir: File): Long {
        if (!dir.exists()) return 0L
        var freed = 0L
        dir.walkBottomUp().forEach { file ->
            if (file.isFile) {
                freed += file.length()
                file.delete()
            }
        }
        return freed
    }

    companion object {
        const val WORK_NAME = "meteory_auto_clean"

        fun schedulePeriodicCleaning(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleaningWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runNow(context: Context): OneTimeWorkRequest {
            val request = OneTimeWorkRequestBuilder<CleaningWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            return request
        }
    }
}
