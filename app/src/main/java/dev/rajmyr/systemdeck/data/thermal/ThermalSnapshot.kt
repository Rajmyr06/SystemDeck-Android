package dev.rajmyr.systemdeck.data.thermal

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class ThermalSensorSnapshot(
    val label: String,
    val source: String,
    val celsius: Double,
)

data class ThermalSnapshot(
    val status: CollectorStatus,
    val systemStatus: String,
    val systemStatusCode: Int,
    val headroom: Float?,
    val sensors: List<ThermalSensorSnapshot>,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    fun sensor(label: String): ThermalSensorSnapshot? =
        sensors.firstOrNull { it.label.equals(label, ignoreCase = true) }

    companion object {
        val Empty = ThermalSnapshot(
            status = CollectorStatus.Unavailable,
            systemStatus = "Waiting",
            systemStatusCode = -1,
            headroom = null,
            sensors = emptyList(),
            sampledAtMillis = 0L,
            message = "Waiting for first thermal sample.",
        )
    }
}
