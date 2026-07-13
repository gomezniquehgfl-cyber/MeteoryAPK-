package com.meteory.optimizer.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShellUtils {

    data class ShellResult(
        val output: String,
        val error: String,
        val exitCode: Int
    )


    suspend fun execShell(
        cmd: String
    ): ShellResult = withContext(Dispatchers.IO) {

        try {

            val process = Runtime.getRuntime()
                .exec(arrayOf("sh", "-c", cmd))


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


            ShellResult(
                output.trim(),
                error.trim(),
                exit
            )


        } catch (e: Exception) {

            ShellResult(
                "",
                e.message ?: "Error",
                -1
            )
        }
    }
}
