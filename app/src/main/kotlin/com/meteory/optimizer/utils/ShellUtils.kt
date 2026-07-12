package com.meteory.optimizer.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShellUtils {

    private const val TAG = "ShellUtils"

    data class ShellResult(
        val output: String,
        val error: String,
        val exitCode: Int
    ) {
        val success: Boolean get() = exitCode == 0
    }

    suspend fun exec(vararg command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(false)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error  = process.errorStream.bufferedReader().readText()
            val exit   = process.waitFor()

            ShellResult(output.trim(), error.trim(), exit)
        } catch (e: Exception) {
            Log.e(TAG, "exec failed: ${e.message}")
            ShellResult("", e.message ?: "", -1)
        }
    }

    suspend fun execShell(cmd: String): ShellResult =
        exec("sh", "-c", cmd)

    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        try {
            java.io.File(path).readText().trim()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readFileLines(path: String): List<String> = withContext(Dispatchers.IO) {
        try {
            java.io.File(path).readLines()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun readFileSync(path: String): String? = try {
        java.io.File(path).readText().trim()
    } catch (e: Exception) {
        null
    }
}
