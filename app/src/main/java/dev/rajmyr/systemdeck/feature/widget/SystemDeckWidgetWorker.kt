package dev.rajmyr.systemdeck.feature.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SystemDeckWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        SystemDeckWidgetProvider.requestUpdate(applicationContext)
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
