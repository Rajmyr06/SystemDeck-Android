package dev.rajmyr.systemdeck.core.alerts

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackgroundAlertScheduler {
    const val PERIOD_MINUTES = 15L
    private const val UNIQUE_WORK_NAME = "systemdeck-background-alerts"

    fun setEnabled(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<BackgroundAlertWorker>(
            PERIOD_MINUTES,
            TimeUnit.MINUTES,
        ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
