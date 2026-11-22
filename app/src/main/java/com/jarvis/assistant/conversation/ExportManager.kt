package com.jarvis.assistant.conversation

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jarvis.assistant.core.ErrorLog
import java.io.File

class ExportManager(
    private val context: Context,
    private val errorLog: ErrorLog
) {
    fun shareTextExport(fileName: String, content: String): Result<Unit> {
        return runCatching {
            val dir = File(context.cacheDir, "exports")
            if (!dir.exists()) dir.mkdirs()
            pruneOldExports(dir)
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, safeName)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export JARVIS Conversation").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            errorLog.record("ExportManager", "Failed to share export", it)
        }
    }

    private fun pruneOldExports(dir: File) {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        dir.listFiles()?.forEach { f ->
            if (f.lastModified() < cutoff) f.delete()
        }
    }
}
