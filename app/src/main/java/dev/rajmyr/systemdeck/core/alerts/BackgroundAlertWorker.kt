package dev.rajmyr.systemdeck.core.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.rajmyr.systemdeck.core.preferences.AppPreferencesRepository
import dev.rajmyr.systemdeck.data.battery.BatteryTelemetryCollector
import dev.rajmyr.systemdeck.data.memory.MemoryTelemetryCollector
import dev.rajmyr.systemdeck.data.storage.StorageTelemetryCollector
import dev.rajmyr.systemdeck.data.thermal.ThermalTelemetryCollector
import kotlinx.coroutines.flow.first

/**
 * Best-effort background threshold evaluation.
 *
 * This deliberately avoids a foreground service. Android/HyperOS may defer the
 * periodic worker, so background alerting is a low-frequency safety net rather
 * than a real-time monitor.
 */
class BackgroundAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val preferencesRepository = AppPreferencesRepository(context)
        val backgroundEnabled = preferencesRepository.backgroundAlertsEnabled.first()
        val preferences = preferencesRepository.alertPreferences.first()

        if (!backgroundEnabled || !preferences.enabled) {
            return@runCatching Result.success()
        }

        val alerts = AlertEvaluator().evaluate(
            preferences = preferences,
            memory = MemoryTelemetryCollector().read(),
            battery = BatteryTelemetryCollector(context).read(),
            thermal = ThermalTelemetryCollector(context).read(),
            storage = StorageTelemetryCollector(context).read(),
        )

        LocalNotificationPublisher(context).sync(
            alerts = alerts,
            enabled = true,
        )
        Result.success()
    }.getOrElse {
        // Periodic monitoring is best-effort. Avoid an aggressive retry loop on
        // OEM/API failures; WorkManager will evaluate again on the next period.
        Result.success()
    }
}
