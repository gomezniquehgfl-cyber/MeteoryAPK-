package com.meteory.optimizer.utils

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

object ShizukuUtils {

    private const val TAG = "ShizukuUtils"
    private const val REQUEST_CODE = 1001

    val isInstalled: Boolean
        get() = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

    val isGranted: Boolean
        get() = try {
            Shizuku.checkSelfPermission() ==
                    PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }

    val isAvailable: Boolean
        get() = isInstalled && isGranted


    fun requestPermission(
        listener: Shizuku.OnRequestPermissionResultListener
    ) {
        Shizuku.addRequestPermissionResultListener(listener)

        if (!isGranted) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }


    suspend fun execPrivileged(
        command: String
    ): ShellUtils.ShellResult =
        withContext(Dispatchers.IO) {

            if (!isAvailable) {
                return@withContext ShellUtils.ShellResult(
                    "",
                    "Shizuku no autorizado",
                    -1
                )
            }

            try {

                val process: ShizukuRemoteProcess =
                    Shizuku.newProcess(
                        arrayOf(
                            "sh",
                            "-c",
                            command
                        ),
                        null,
                        null
                    )

                val output =
                    process.inputStream
                        .bufferedReader()
                        .readText()

                val error =
                    process.errorStream
                        .bufferedReader()
                        .readText()

                val exit =
                    process.waitFor()

                ShellUtils.ShellResult(
                    output.trim(),
                    error.trim(),
                    exit
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error ejecutando Shizuku: ${e.message}"
                )

                ShellUtils.ShellResult(
                    "",
                    e.message ?: "Error desconocido",
                    -1
                )
            }
        }


    suspend fun execBestEffort(
        command: String
    ): ShellUtils.ShellResult {

        return if (isAvailable) {
            execPrivileged(command)
        } else {
            ShellUtils.execShell(command)
        }
    }
}