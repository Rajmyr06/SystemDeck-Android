package dev.rajmyr.systemdeck.core.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.rajmyr.systemdeck.MainActivity

class LocalNotificationPublisher(
    private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val state = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createChannel()
    }

    /**
     * Reconciles current alert conditions with notifications.
     *
     * Active-condition state is persisted so the foreground collector and the
     * periodic WorkManager worker cannot repeatedly notify for the same
     * unresolved condition after process restarts.
     */
    fun sync(alerts: List<SystemAlert>, enabled: Boolean) = synchronized(PERSISTENCE_LOCK) {
        val activeIds = state.getStringSet(KEY_ACTIVE_IDS, emptySet())
            .orEmpty()
            .toMutableSet()
        val current = alerts.mapTo(mutableSetOf()) { it.id }

        (activeIds - current).forEach { id ->
            notificationManager.cancel(notificationId(id))
            activeIds.remove(id)
        }

        if (!enabled || !canNotify()) {
            activeIds.forEach { id -> notificationManager.cancel(notificationId(id)) }
            activeIds.clear()
            persistActive(activeIds)
            return@synchronized
        }

        val now = System.currentTimeMillis()
        alerts.forEach { alert ->
            val last = state.getLong(lastNotifiedKey(alert.id), 0L)
            val isNewCondition = alert.id !in activeIds
            val cooldownExpired = now - last >= NOTIFICATION_COOLDOWN_MS
            if (isNewCondition) {
                if (cooldownExpired) {
                    publish(alert)
                    state.edit().putLong(lastNotifiedKey(alert.id), now).apply()
                    activeIds += alert.id
                }
            } else {
                activeIds += alert.id
            }
        }
        persistActive(activeIds)
    }

    private fun persistActive(ids: Set<String>) {
        state.edit().putStringSet(KEY_ACTIVE_IDS, ids.toSet()).apply()
    }

    private fun publish(alert: SystemAlert) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(alert.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Keep the permission check at the actual notify() call site so both
        // Android Lint and runtime behavior are safe if permission changes
        // between condition evaluation and publication.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        try {
            notificationManager.notify(notificationId(alert.id), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked asynchronously. Treat notification
            // publication as best-effort; telemetry collection must not crash.
        }
    }

    private fun canNotify(): Boolean {
        val runtimePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimePermissionGranted && notificationManager.areNotificationsEnabled()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SystemDeck alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local threshold alerts generated from SystemDeck telemetry."
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(id: String): Int = when (id) {
        "memory_high" -> 8101
        "battery_low" -> 8102
        "thermal_status" -> 8103
        "storage_high" -> 8104
        else -> 8199
    }

    private fun lastNotifiedKey(id: String) = "last_notified_$id"

    companion object {
        const val CHANNEL_ID = "systemdeck_alerts"
        private const val NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000L
        private const val PREFS_NAME = "systemdeck_alert_notification_state"
        private const val KEY_ACTIVE_IDS = "active_ids"
        private val PERSISTENCE_LOCK = Any()
    }
}
