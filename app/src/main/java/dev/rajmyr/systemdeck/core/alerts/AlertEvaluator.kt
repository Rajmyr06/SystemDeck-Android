package dev.rajmyr.systemdeck.core.alerts

import dev.rajmyr.systemdeck.core.format.DeckFormat
import dev.rajmyr.systemdeck.data.battery.BatterySnapshot
import dev.rajmyr.systemdeck.data.memory.MemorySnapshot
import dev.rajmyr.systemdeck.data.storage.StorageSnapshot
import dev.rajmyr.systemdeck.data.thermal.ThermalSnapshot

class AlertEvaluator {
    fun evaluate(
        preferences: AlertPreferences,
        memory: MemorySnapshot,
        battery: BatterySnapshot,
        thermal: ThermalSnapshot,
        storage: StorageSnapshot,
    ): List<SystemAlert> {
        if (!preferences.enabled) return emptyList()

        return buildList {
            memoryAlert(preferences, memory)?.let(::add)
            batteryAlert(preferences, battery)?.let(::add)
            thermalAlert(preferences, thermal)?.let(::add)
            storageAlert(preferences, storage)?.let(::add)
        }
    }

    private fun memoryAlert(
        preferences: AlertPreferences,
        memory: MemorySnapshot,
    ): SystemAlert? {
        if (!preferences.memoryRuleEnabled || memory.totalKiB <= 0L || memory.usedKiB < 0L) return null
        val percent = DeckFormat.percent(memory.usedKiB, memory.totalKiB)
        if (percent < preferences.memoryHighPercent) return null

        return SystemAlert(
            id = "memory_high",
            title = "Memory usage is high",
            message = "Physical memory in use reached $percent%. This rule is based on MemTotal minus MemAvailable.",
            severity = if (percent >= 95) AlertSeverity.Critical else AlertSeverity.Warning,
            metricValue = "$percent%",
            ruleDescription = "Alert at ${preferences.memoryHighPercent}% or above",
        )
    }

    private fun batteryAlert(
        preferences: AlertPreferences,
        battery: BatterySnapshot,
    ): SystemAlert? {
        if (!preferences.batteryRuleEnabled) return null
        val level = battery.levelPercent ?: return null
        val charging = battery.chargeStatus.equals("Charging", ignoreCase = true) ||
            battery.chargeStatus.equals("Full", ignoreCase = true)
        if (charging || level > preferences.batteryLowPercent) return null

        return SystemAlert(
            id = "battery_low",
            title = "Battery level is low",
            message = "Battery is at $level% and is not currently charging.",
            severity = if (level <= 10) AlertSeverity.Critical else AlertSeverity.Warning,
            metricValue = "$level%",
            ruleDescription = "Alert at ${preferences.batteryLowPercent}% or below",
        )
    }

    private fun thermalAlert(
        preferences: AlertPreferences,
        thermal: ThermalSnapshot,
    ): SystemAlert? {
        if (!preferences.thermalRuleEnabled || thermal.systemStatusCode < 0) return null
        if (thermal.systemStatusCode < preferences.thermalStatusThreshold) return null

        return SystemAlert(
            id = "thermal_status",
            title = "Android thermal status increased",
            message = "The platform reports ${thermal.systemStatus}. This rule follows Android's thermal throttling status, not a guessed temperature limit.",
            severity = if (thermal.systemStatusCode >= 4) AlertSeverity.Critical else AlertSeverity.Warning,
            metricValue = thermal.systemStatus,
            ruleDescription = "Alert from ${thermalThresholdLabel(preferences.thermalStatusThreshold)}",
        )
    }

    private fun storageAlert(
        preferences: AlertPreferences,
        storage: StorageSnapshot,
    ): SystemAlert? {
        if (!preferences.storageRuleEnabled || storage.totalBytes <= 0L) return null
        if (storage.usagePercent < preferences.storageHighPercent) return null

        return SystemAlert(
            id = "storage_high",
            title = "Internal storage is filling up",
            message = "The writable data volume is ${storage.usagePercent}% used.",
            severity = if (storage.usagePercent >= 97) AlertSeverity.Critical else AlertSeverity.Warning,
            metricValue = "${storage.usagePercent}%",
            ruleDescription = "Alert at ${preferences.storageHighPercent}% or above",
        )
    }

    companion object {
        fun thermalThresholdLabel(code: Int): String = when (code) {
            0 -> "None"
            1 -> "Light"
            2 -> "Moderate"
            3 -> "Severe"
            4 -> "Critical"
            5 -> "Emergency"
            6 -> "Shutdown"
            else -> "Status $code"
        }
    }
}
