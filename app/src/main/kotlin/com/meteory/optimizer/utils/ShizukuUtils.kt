package com.meteory.optimizer.utils

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuUtils {

    private const val TAG         = "ShizukuUtils"
    private const val REQUEST_CODE = 1001

    val isInstalled: Boolean
        get() = try {
            Shizuku.pingBinder()
            true
        } catch (e: IllegalStateException) { false }

    val isGranted: Boolean
        get() = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { false }

    val isAvailable: Boolean get() = isInstalled && isGranted

    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku request failed: ${e.message}")
        }
    }

    suspend fun execPrivileged(cmd: String): ShellUtils.ShellResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            return@withContext ShellUtils.ShellResult("", "Shizuku not available", -1)
        }
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output  = process.inputStream.bufferedReader().readText()
            val error   = process.errorStream.bufferedReader().readText()
            val exit    = process.waitFor()
            ShellUtils.ShellResult(output.trim(), error.trim(), exit)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku exec failed: ${e.message}")
            ShellUtils.ShellResult("", e.message ?: "", -1)
        }
    }

    suspend fun execBestEffort(cmd: String): ShellUtils.ShellResult =
        if (isAvailable) execPrivileged(cmd)
        else ShellUtils.execShell(cmd)
}
