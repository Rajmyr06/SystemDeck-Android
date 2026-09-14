package dev.rajmyr.systemdeck.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetRefreshScheduler {
    const val PERIOD_MINUTES = 15L
    private const val UNIQUE_WORK_NAME = "systemdeck-widget-refresh"

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<SystemDeckWidgetWorker>(
            PERIOD_MINUTES,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    /** Reconciles periodic work after an upgrade without scheduling it unnecessarily. */
    fun syncWithInstalledWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, SystemDeckWidgetProvider::class.java)
        if (manager.getAppWidgetIds(component).isEmpty()) {
            cancel(appContext)
        } else {
            ensureScheduled(appContext)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
