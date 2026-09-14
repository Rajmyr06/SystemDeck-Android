package dev.rajmyr.systemdeck.core.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes shareable exports to app cache. No broad storage permission is required. */
object TelemetryExportManager {
    fun reportUri(context: Context, content: String): Uri = write(
        context = context,
        prefix = "systemdeck-snapshot",
        extension = "txt",
        content = content,
    )

    fun sessionCsvUri(context: Context, content: String): Uri = write(
        context = context,
        prefix = "systemdeck-session",
        extension = "csv",
        content = content,
    )

    fun shareIntent(
        uri: Uri,
        mimeType: String,
        subject: String,
    ): Intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(subject, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun write(
        context: Context,
        prefix: String,
        extension: String,
        content: String,
    ): Uri {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(directory, "$prefix-$timestamp.$extension")
        file.writeText(content, Charsets.UTF_8)
        pruneOldExports(directory, keepNewest = 20)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
    }

    private fun pruneOldExports(directory: File, keepNewest: Int) {
        directory.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keepNewest)
            ?.forEach { runCatching { it.delete() } }
    }
}
