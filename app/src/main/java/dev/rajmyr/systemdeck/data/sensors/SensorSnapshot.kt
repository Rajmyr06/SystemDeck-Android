package dev.rajmyr.systemdeck.data.sensors

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class SensorDeviceSnapshot(
    val name: String,
    val vendor: String,
    val stringType: String,
    val type: Int,
    val version: Int,
    val powerMilliAmps: Float,
    val resolution: Float,
    val maximumRange: Float,
    val minDelayMicros: Int,
    val reportingMode: String,
    val wakeUp: Boolean,
    val dynamic: Boolean,
    val values: List<Float> = emptyList(),
    val accuracy: Int? = null,
    val lastEventNanos: Long? = null,
)

data class SensorSnapshot(
    val status: CollectorStatus,
    val totalSensors: Int,
    val liveSensors: Int,
    val wakeUpSensors: Int,
    val dynamicSensors: Int,
    val sensors: List<SensorDeviceSnapshot>,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    companion object {
        val Empty = SensorSnapshot(
            status = CollectorStatus.Unavailable,
            totalSensors = 0,
            liveSensors = 0,
            wakeUpSensors = 0,
            dynamicSensors = 0,
            sensors = emptyList(),
            sampledAtMillis = 0L,
            message = "Sensor inventory has not been sampled yet.",
        )
    }
}
