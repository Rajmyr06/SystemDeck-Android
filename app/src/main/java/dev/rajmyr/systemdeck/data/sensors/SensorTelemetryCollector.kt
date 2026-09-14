package dev.rajmyr.systemdeck.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class SensorTelemetryCollector(
    context: Context,
) {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)

    private data class LatestValue(
        val values: List<Float>,
        val accuracy: Int,
        val timestampNanos: Long,
    )

    fun observe(intervalMillis: Long = 1_000L): Flow<SensorSnapshot> = flow {
        val inventory = runCatching { sensorManager.getSensorList(Sensor.TYPE_ALL) }.getOrDefault(emptyList())
        val latest = mutableMapOf<Sensor, LatestValue>()
        val accuracy = mutableMapOf<Sensor, Int>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                synchronized(latest) {
                    latest[event.sensor] = LatestValue(
                        values = event.values.toList(),
                        accuracy = accuracy[event.sensor] ?: event.accuracy,
                        timestampNanos = event.timestamp,
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                synchronized(latest) {
                    accuracy[sensor] = value
                }
            }
        }

        // Keep the live subscription deliberately small. Inventory remains
        // complete, while only low-risk channels needed by the dashboard are
        // registered at 5 Hz to avoid turning the telemetry app into a sensor drain.
        val liveTypes = setOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
        )

        val registered = inventory
            .filter { it.type in liveTypes }
            .filter { sensor ->
                runCatching {
                    sensorManager.registerListener(
                        listener,
                        sensor,
                        200_000,
                    )
                }.getOrDefault(false)
            }

        try {
            while (currentCoroutineContext().isActive) {
                val latestCopy = synchronized(latest) { latest.toMap() }
                emit(buildSnapshot(inventory, registered, latestCopy))
                delay(intervalMillis)
            }
        } finally {
            runCatching { sensorManager.unregisterListener(listener) }
        }
    }.flowOn(Dispatchers.Default)

    private fun buildSnapshot(
        inventory: List<Sensor>,
        registered: List<Sensor>,
        latest: Map<Sensor, LatestValue>,
    ): SensorSnapshot {
        val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
        val staleAfterNanos = LIVE_EVENT_STALE_MILLIS * 1_000_000L

        val sensors = inventory.map { sensor ->
            val candidate = latest[sensor]
            val live = candidate?.takeIf { event ->
                val age = nowElapsedNanos - event.timestampNanos
                age in 0..staleAfterNanos
            }

            SensorDeviceSnapshot(
                name = sensor.name,
                vendor = sensor.vendor,
                stringType = sensor.stringType,
                type = sensor.type,
                version = sensor.version,
                powerMilliAmps = sensor.power,
                resolution = sensor.resolution,
                maximumRange = sensor.maximumRange,
                minDelayMicros = sensor.minDelay,
                reportingMode = reportingMode(sensor.reportingMode),
                wakeUp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) sensor.isWakeUpSensor else false,
                dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) sensor.isDynamicSensor else false,
                values = live?.values.orEmpty(),
                accuracy = live?.accuracy,
                lastEventNanos = live?.timestampNanos,
            )
        }

        val status = when {
            inventory.isEmpty() -> CollectorStatus.Unavailable
            registered.isEmpty() -> CollectorStatus.Degraded
            else -> CollectorStatus.Ok
        }

        return SensorSnapshot(
            status = status,
            totalSensors = sensors.size,
            liveSensors = sensors.count { it.values.isNotEmpty() },
            wakeUpSensors = sensors.count { it.wakeUp },
            dynamicSensors = sensors.count { it.dynamic },
            sensors = sensors,
            sampledAtMillis = System.currentTimeMillis(),
            message = when (status) {
                CollectorStatus.Ok -> null
                CollectorStatus.Degraded -> "Sensor inventory is readable, but no low-risk live channels could be registered."
                CollectorStatus.Unavailable -> "Android SensorManager returned no hardware sensors."
            },
        )
    }

    private fun reportingMode(mode: Int): String = when (mode) {
        Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
        Sensor.REPORTING_MODE_ON_CHANGE -> "On change"
        Sensor.REPORTING_MODE_ONE_SHOT -> "One shot"
        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special trigger"
        else -> "Unknown"
    }

    companion object {
        private const val LIVE_EVENT_STALE_MILLIS = 5_000L
    }
}
