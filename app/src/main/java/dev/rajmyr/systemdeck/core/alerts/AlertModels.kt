package dev.rajmyr.systemdeck.core.alerts

import androidx.compose.runtime.Immutable

@Immutable
data class AlertPreferences(
    val enabled: Boolean = false,
    val memoryRuleEnabled: Boolean = true,
    val memoryHighPercent: Int = 85,
    val batteryRuleEnabled: Boolean = true,
    val batteryLowPercent: Int = 20,
    val thermalRuleEnabled: Boolean = true,
    val thermalStatusThreshold: Int = 2,
    val storageRuleEnabled: Boolean = true,
    val storageHighPercent: Int = 90,
)

enum class AlertSeverity {
    Info,
    Warning,
    Critical,
}

@Immutable
data class SystemAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val metricValue: String,
    val ruleDescription: String,
)
