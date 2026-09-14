package dev.rajmyr.systemdeck.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class BatteryTelemetryCollector(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(BatteryManager::class.java)

    fun observe(intervalMillis: Long = 5_000L): Flow<BatterySnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): BatterySnapshot {
        val intent = runCatching {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        if (intent == null) {
            return BatterySnapshot.Empty.copy(
                sampledAtMillis = System.currentTimeMillis(),
                message = "ACTION_BATTERY_CHANGED was not available.",
            )
        }

        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val level = if (rawLevel >= 0 && scale > 0) {
            ((rawLevel.toDouble() / scale.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else null

        val statusCode = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val pluggedCode = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 }
        val temperatureRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = temperatureRaw
            .takeIf { it != Int.MIN_VALUE }
            ?.div(10.0)
            ?.takeIf { it in -20.0..100.0 }

        val currentNow = readIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentAverage = readIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeCounter = readIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it >= 0 }

        val cycleCount = if (Build.VERSION.SDK_INT >= 34 && intent.hasExtra(BatteryManager.EXTRA_CYCLE_COUNT)) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 }
        } else null

        val remaining = runCatching { batteryManager.computeChargeTimeRemaining() }
            .getOrNull()
            ?.takeIf { it >= 0L }

        return BatterySnapshot(
            status = if (level != null) CollectorStatus.Ok else CollectorStatus.Degraded,
            levelPercent = level,
            chargeStatus = statusLabel(statusCode),
            pluggedSource = pluggedLabel(pluggedCode),
            voltageMilliVolts = voltage,
            currentNowMicroAmps = currentNow,
            currentAverageMicroAmps = currentAverage,
            chargeCounterMicroAh = chargeCounter,
            cycleCount = cycleCount,
            temperatureCelsius = temperature,
            health = healthLabel(healthCode),
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
            chargeTimeRemainingMillis = remaining,
            sampledAtMillis = System.currentTimeMillis(),
            message = if (currentNow == null) {
                "Basic battery state is available. Instant current is not exposed by this device."
            } else null,
        )
    }

    private fun readIntProperty(id: Int): Int? {
        val value = runCatching { batteryManager.getIntProperty(id) }.getOrNull() ?: return null
        return value.takeUnless { it == Int.MIN_VALUE }
    }

    private fun statusLabel(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }

    private fun pluggedLabel(plugged: Int): String = when {
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "AC charger"
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Wireless"
        Build.VERSION.SDK_INT >= 33 && plugged and BatteryManager.BATTERY_PLUGGED_DOCK != 0 -> "Dock"
        else -> "On battery"
    }

    private fun healthLabel(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }
}
