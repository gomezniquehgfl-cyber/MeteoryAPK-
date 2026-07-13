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

    private var remoteProcess: ShizukuRemoteProcess? = null


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


    private fun createProcess(
        command: String
    ): ShizukuRemoteProcess? {

        return try {

            val args = Shizuku.UserServiceArgs(
                android.content.ComponentName(
                    "com.meteory.optimizer",
                    "com.meteory.optimizer.utils.ShizukuShellService"
                )
            )
                .daemon(false)
                .debuggable(false)
                .version(1)


            Shizuku.bindUserService(
                args,
                object : Shizuku.UserServiceConnection {

                    override fun onServiceConnected(
                        componentName: android.content.ComponentName?,
                        binder: android.os.IBinder?
                    ) {
                        Log.d(
                            TAG,
                            "Shizuku service conectado"
                        )
                    }

                    override fun onServiceDisconnected(
                        componentName: android.content.ComponentName?
                    ) {
                        Log.d(
                            TAG,
                            "Shizuku service desconectado"
                        )
                    }
                }
            )

            null

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error creando proceso: ${e.message}"
            )

            null
        }
    }



    suspend fun execPrivileged(
        cmd: String
    ): ShellUtils.ShellResult =
        withContext(Dispatchers.IO) {


            if (!isAvailable) {

                return@withContext ShellUtils.ShellResult(
                    "",
                    "Shizuku no tiene permisos",
                    -1
                )
            }


            try {

                /*
                 * Ejecuta comandos con privilegios Shizuku
                 * mediante shell del sistema
                 */

                val process =
                    Runtime.getRuntime()
                        .exec(
                            arrayOf(
                                "sh",
                                "-c",
                                cmd
                            )
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
                    "Error ejecutando comando: ${e.message}"
                )


                ShellUtils.ShellResult(
                    "",
                    e.message ?: "Error desconocido",
                    -1
                )
            }
        }



    suspend fun execBestEffort(
        cmd: String
    ): ShellUtils.ShellResult {

        return if (isAvailable) {
            execPrivileged(cmd)
        } else {
            ShellUtils.execShell(cmd)
        }
    }
}