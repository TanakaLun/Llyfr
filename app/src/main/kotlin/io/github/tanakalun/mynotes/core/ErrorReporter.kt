package io.github.tanakalun.mynotes.core

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

/** 统一错误出口：写日志文件并返回用户可读的失败信息。 */
class ErrorReporter(private val appContext: Context) {

    fun report(tag: String, e: Throwable, targetUri: Uri?): ErrorReport {
        Log.e(tag, "Error, targetUri=$targetUri", e)
        return try {
            val logFile = File(appContext.filesDir, "backup_error_${System.currentTimeMillis()}.log")
            logFile.writeText(
                buildString {
                    appendLine("=== MyNotes Backup Error ===")
                    appendLine("time=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine("tag=$tag")
                    appendLine("targetUri=$targetUri")
                    appendLine("exception=${e::class.java.name}: ${e.message}")
                    appendLine()
                    appendLine(e.stackTraceToString())
                },
            )
            Log.e(tag, "Stack trace written to ${logFile.absolutePath}")
            ErrorReport(e.message ?: "", logFile.absolutePath)
        } catch (e2: Exception) {
            Log.e(tag, "Failed to write error log", e2)
            ErrorReport(e.message ?: "", null)
        }
    }

    data class ErrorReport(
        val message: String,
        val logPath: String?,
    )
}