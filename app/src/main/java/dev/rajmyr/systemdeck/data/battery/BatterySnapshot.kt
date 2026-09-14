package dev.rajmyr.systemdeck.data.battery

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class BatterySnapshot(
    val status: CollectorStatus,
    val levelPercent: Int?,
    val chargeStatus: String,
    val pluggedSource: String,
    val voltageMilliVolts: Int?,
    val currentNowMicroAmps: Int?,
    val currentAverageMicroAmps: Int?,
    val chargeCounterMicroAh: Int?,
    val cycleCount: Int?,
    val temperatureCelsius: Double?,
    val health: String,
    val technology: String?,
    val chargeTimeRemainingMillis: Long?,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    val powerWatts: Double?
        get() {
            val mv = voltageMilliVolts ?: return null
            val ua = currentNowMicroAmps ?: return null
            return (mv / 1000.0) * (ua / 1_000_000.0)
        }

    companion object {
        val Empty = BatterySnapshot(
            status = CollectorStatus.Unavailable,
            levelPercent = null,
            chargeStatus = "Waiting",
            pluggedSource = "—",
            voltageMilliVolts = null,
            currentNowMicroAmps = null,
            currentAverageMicroAmps = null,
            chargeCounterMicroAh = null,
            cycleCount = null,
            temperatureCelsius = null,
            health = "Unknown",
            technology = null,
            chargeTimeRemainingMillis = null,
            sampledAtMillis = 0L,
            message = "Waiting for first battery sample.",
        )
    }
}
