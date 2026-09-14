package dev.rajmyr.systemdeck.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import dev.rajmyr.systemdeck.MainActivity
import dev.rajmyr.systemdeck.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class SystemDeckWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshScheduler.ensureScheduled(context)
        requestUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshScheduler.cancel(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetRefreshScheduler.ensureScheduled(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(
                id,
                buildViews(context.applicationContext, appWidgetManager, id),
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(
            appWidgetId,
            buildViews(context.applicationContext, appWidgetManager, appWidgetId),
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            requestUpdate(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "dev.rajmyr.systemdeck.action.WIDGET_REFRESH"
        private const val FOREGROUND_PUBLISH_INTERVAL_MS = 10_000L
        private const val COMPACT_WIDTH_DP = 300
        private const val COMPACT_HEIGHT_DP = 58
        private const val EXPANDED_HEIGHT_DP = 118
        private val lastForegroundPublishAt = AtomicLong(0L)

        /**
         * Bridge from the foreground telemetry pipeline.
         *
         * Foreground collectors can sample every second, but RemoteViews are
         * intentionally limited to one launcher update per 10 seconds.
         */
        @Suppress("UNUSED_PARAMETER")
        fun publish(
            context: Context,
            cpu: Any?,
            memory: Any?,
            battery: Any?,
            thermal: Any?,
        ) {
            val now = SystemClock.elapsedRealtime()
            while (true) {
                val previous = lastForegroundPublishAt.get()
                if (now - previous < FOREGROUND_PUBLISH_INTERVAL_MS) return
                if (lastForegroundPublishAt.compareAndSet(previous, now)) break
            }
            requestUpdate(context)
        }

        fun requestUpdate(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, SystemDeckWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, buildViews(appContext, manager, id))
            }
        }

        private fun buildViews(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
        ): RemoteViews {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val expanded = minHeight >= EXPANDED_HEIGHT_DP || maxHeight >= EXPANDED_HEIGHT_DP
            val compact = !expanded && (
                (minWidth in 1 until COMPACT_WIDTH_DP) ||
                    (maxHeight in 1 until COMPACT_HEIGHT_DP)
                )

            val telemetry = WidgetTelemetryReader.read(
                context = context,
                includeExtended = expanded,
            )

            return when {
                expanded -> buildExpandedViews(context, appWidgetId, telemetry)
                compact -> buildCompactViews(context, appWidgetId, telemetry)
                else -> buildFullViews(context, appWidgetId, telemetry)
            }
        }

        private fun buildFullViews(
            context: Context,
            appWidgetId: Int,
            telemetry: WidgetTelemetry,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_systemdeck)

            views.setTextViewText(R.id.widget_cpu_value, telemetry.cpu)
            views.setTextViewText(R.id.widget_cpu_sub, "max ${telemetry.cpuMax}")
            views.setTextViewText(
                R.id.widget_mem_value,
                telemetry.memoryPercent?.let { "$it%" } ?: "—",
            )
            views.setTextViewText(
                R.id.widget_mem_sub,
                "${telemetry.memoryUsed} / ${telemetry.memoryTotal}",
            )
            views.setTextViewText(
                R.id.widget_bat_value,
                telemetry.batteryPercent?.let { "$it%" } ?: "—",
            )
            views.setTextViewText(R.id.widget_bat_sub, telemetry.batteryState)
            views.setTextViewText(
                R.id.widget_temp_value,
                telemetry.temperatureC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—",
            )
            views.setTextViewText(R.id.widget_temp_sub, telemetry.thermalLabel)
            views.setTextViewText(R.id.widget_updated, timeStamp())

            bindActions(
                context = context,
                appWidgetId = appWidgetId,
                views = views,
                rootId = R.id.widget_root,
                refreshId = R.id.widget_refresh,
            )
            return views
        }

        private fun buildCompactViews(
            context: Context,
            appWidgetId: Int,
            telemetry: WidgetTelemetry,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_systemdeck_compact)
            views.setTextViewText(R.id.widget_compact_cpu, "CPU ${telemetry.cpu}")
            views.setTextViewText(
                R.id.widget_compact_mem,
                "MEM ${telemetry.memoryPercent?.let { "$it%" } ?: "—"}",
            )
            views.setTextViewText(
                R.id.widget_compact_bat,
                "BAT ${telemetry.batteryPercent?.let { "$it%" } ?: "—"}",
            )
            views.setTextViewText(
                R.id.widget_compact_thm,
                "THM ${telemetry.temperatureC?.let { String.format(Locale.US, "%.0f°", it) } ?: "—"}",
            )
            views.setTextViewText(R.id.widget_compact_updated, timeStamp())

            bindActions(
                context = context,
                appWidgetId = appWidgetId,
                views = views,
                rootId = R.id.widget_compact_root,
                refreshId = R.id.widget_compact_refresh,
            )
            return views
        }

        private fun buildExpandedViews(
            context: Context,
            appWidgetId: Int,
            telemetry: WidgetTelemetry,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_systemdeck_expanded)

            views.setTextViewText(R.id.widget_expanded_cpu_value, telemetry.cpu)
            views.setTextViewText(R.id.widget_expanded_cpu_sub, "max ${telemetry.cpuMax}")
            views.setTextViewText(
                R.id.widget_expanded_mem_value,
                telemetry.memoryPercent?.let { "$it%" } ?: "—",
            )
            views.setTextViewText(
                R.id.widget_expanded_mem_sub,
                "${telemetry.memoryUsed} / ${telemetry.memoryTotal}",
            )
            views.setTextViewText(
                R.id.widget_expanded_bat_value,
                telemetry.batteryPercent?.let { "$it%" } ?: "—",
            )
            views.setTextViewText(R.id.widget_expanded_bat_sub, telemetry.batteryState)
            views.setTextViewText(
                R.id.widget_expanded_temp_value,
                telemetry.temperatureC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—",
            )
            views.setTextViewText(R.id.widget_expanded_temp_sub, telemetry.thermalLabel)

            views.setTextViewText(
                R.id.widget_expanded_storage_value,
                telemetry.storagePercent?.let { "$it%" } ?: "—",
            )
            views.setTextViewText(
                R.id.widget_expanded_storage_sub,
                "${telemetry.storageUsed} / ${telemetry.storageTotal}",
            )
            views.setTextViewText(R.id.widget_expanded_gpu_value, telemetry.gpuBusy)
            views.setTextViewText(R.id.widget_expanded_gpu_sub, "max ${telemetry.gpuMax}")
            views.setTextViewText(R.id.widget_expanded_power_value, telemetry.batteryPower)
            views.setTextViewText(R.id.widget_expanded_power_sub, telemetry.batteryVoltage)
            views.setTextViewText(R.id.widget_expanded_thermal_state_value, telemetry.thermalStatus)
            views.setTextViewText(R.id.widget_expanded_updated, timeStamp())

            bindActions(
                context = context,
                appWidgetId = appWidgetId,
                views = views,
                rootId = R.id.widget_expanded_root,
                refreshId = R.id.widget_expanded_refresh,
            )
            return views
        }

        private fun bindActions(
            context: Context,
            appWidgetId: Int,
            views: RemoteViews,
            rootId: Int,
            refreshId: Int,
        ) {
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context,
                appWidgetId * 10,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(rootId, openPending)

            val refreshIntent = Intent(context, SystemDeckWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(refreshId, refreshPending)
        }

        private fun timeStamp(): String =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
