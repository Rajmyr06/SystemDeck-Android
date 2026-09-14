package dev.rajmyr.systemdeck.feature.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.rajmyr.systemdeck.core.alerts.AlertEvaluator
import dev.rajmyr.systemdeck.core.alerts.AlertPreferences
import dev.rajmyr.systemdeck.core.alerts.AlertSeverity
import dev.rajmyr.systemdeck.core.alerts.SystemAlert
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckRed
import dev.rajmyr.systemdeck.ui.theme.DeckText

@Composable
fun AlertsScreen(
    preferences: AlertPreferences,
    activeAlerts: List<SystemAlert>,
    backgroundAlertsEnabled: Boolean,
    onPreferencesChanged: (AlertPreferences) -> Unit,
    onBackgroundAlertsChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(notificationPermissionGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted || notificationPermissionGranted(context) }

    val notificationAccess = permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckPageHeader(
            title = "Alerts",
            subtitle = "Local threshold rules.",
            trailing = if (preferences.enabled) "Live rules" else "Off",
        )
        DeckSummaryGrid(
            items = listOf(
                DeckSummarySpec(
                    label = "Active",
                    value = activeAlerts.size.toString(),
                    supporting = if (activeAlerts.isEmpty()) "No active rule" else "${activeAlerts.size} active",
                    code = "AL",
                    accent = if (activeAlerts.isEmpty()) DeckGreen else DeckAmber,
                ),
                DeckSummarySpec(
                    label = "Notifications",
                    value = if (notificationAccess) "Allowed" else "Not allowed",
                    supporting = if (notificationAccess) "Enabled" else "Optional",
                    code = "NT",
                    accent = if (notificationAccess) DeckGreen else DeckAmber,
                ),
                DeckSummarySpec(
                    label = "Scope",
                    value = when {
                        !preferences.enabled -> "Off"
                        backgroundAlertsEnabled -> "Foreground + periodic"
                        else -> "Foreground"
                    },
                    supporting = when {
                        !preferences.enabled -> "Master alert engine disabled"
                        backgroundAlertsEnabled -> "Best-effort background checks ≥15 min"
                        else -> "While app is open"
                    },
                    code = if (backgroundAlertsEnabled) "BG" else "FG",
                    accent = DeckBlue,
                ),
            ),
        )

        DeckPanel(
            title = "Alert engine",
            subtitle = "Foreground rule evaluation.",
            code = "AL",
            accent = DeckBlue,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleRow(
                    title = "Threshold alerts",
                    supporting = "Evaluate rules from validated local telemetry.",
                    checked = preferences.enabled,
                    onCheckedChange = { enabled -> onPreferencesChanged(preferences.copy(enabled = enabled)) },
                )
                ToggleRow(
                    title = "Background checks",
                    supporting = "Optional WorkManager evaluation at a best-effort interval of at least 15 minutes; no foreground service.",
                    checked = backgroundAlertsEnabled,
                    onCheckedChange = onBackgroundAlertsChanged,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAccess) {
                TextButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                ) {
                    Text("Allow notification delivery")
                }
            }
        }

        if (activeAlerts.isNotEmpty()) {
            DeckPanel(
                title = "Active conditions",
                subtitle = "Rules matching the latest sample.",
                code = "NOW",
                accent = DeckAmber,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeAlerts.forEach { alert -> ActiveAlertRow(alert) }
                }
            }
        }

        DeckSectionLabel(
            title = "Rules",
        )

        AlertRulePanel(
            title = "Memory in use",
            subtitle = "Memory used percentage.",
            code = "MEM",
            enabled = preferences.memoryRuleEnabled,
            onEnabledChanged = { enabled -> onPreferencesChanged(preferences.copy(memoryRuleEnabled = enabled)) },
            value = preferences.memoryHighPercent,
            valueLabel = "${preferences.memoryHighPercent}% or above",
            min = 70,
            max = 98,
            step = 5,
            onValueChanged = { value -> onPreferencesChanged(preferences.copy(memoryHighPercent = value)) },
        )

        AlertRulePanel(
            title = "Battery level",
            subtitle = "Low battery while not charging.",
            code = "BAT",
            enabled = preferences.batteryRuleEnabled,
            onEnabledChanged = { enabled -> onPreferencesChanged(preferences.copy(batteryRuleEnabled = enabled)) },
            value = preferences.batteryLowPercent,
            valueLabel = "${preferences.batteryLowPercent}% or below",
            min = 5,
            max = 50,
            step = 5,
            onValueChanged = { value -> onPreferencesChanged(preferences.copy(batteryLowPercent = value)) },
        )

        ThermalRulePanel(
            preferences = preferences,
            onEnabledChanged = { enabled -> onPreferencesChanged(preferences.copy(thermalRuleEnabled = enabled)) },
            onThresholdChanged = { value -> onPreferencesChanged(preferences.copy(thermalStatusThreshold = value)) },
        )

        AlertRulePanel(
            title = "Internal storage",
            subtitle = "Writable internal storage.",
            code = "DSK",
            enabled = preferences.storageRuleEnabled,
            onEnabledChanged = { enabled -> onPreferencesChanged(preferences.copy(storageRuleEnabled = enabled)) },
            value = preferences.storageHighPercent,
            valueLabel = "${preferences.storageHighPercent}% or above",
            min = 70,
            max = 99,
            step = 5,
            onValueChanged = { value -> onPreferencesChanged(preferences.copy(storageHighPercent = value)) },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                color = DeckText,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                supporting,
                color = DeckMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlertRulePanel(
    title: String,
    subtitle: String,
    code: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    value: Int,
    valueLabel: String,
    min: Int,
    max: Int,
    step: Int,
    onValueChanged: (Int) -> Unit,
) {
    DeckPanel(
        title = title,
        subtitle = subtitle,
        code = code,
        accent = if (enabled) DeckCyan else DeckMuted,
        trailing = if (enabled) "Enabled" else "Off",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleRow(
                title = "Rule enabled",
                supporting = "Current threshold: $valueLabel",
                checked = enabled,
                onCheckedChange = onEnabledChanged,
            )
            ThresholdStepper(
                value = value,
                valueLabel = valueLabel,
                min = min,
                max = max,
                step = step,
                enabled = enabled,
                onValueChanged = onValueChanged,
            )
        }
    }
}

@Composable
private fun ThermalRulePanel(
    preferences: AlertPreferences,
    onEnabledChanged: (Boolean) -> Unit,
    onThresholdChanged: (Int) -> Unit,
) {
    val label = AlertEvaluator.thermalThresholdLabel(preferences.thermalStatusThreshold)
    DeckPanel(
        title = "Android thermal status",
        subtitle = "Android thermal status.",
        code = "THM",
        accent = if (preferences.thermalRuleEnabled) DeckAmber else DeckMuted,
        trailing = if (preferences.thermalRuleEnabled) "Enabled" else "Off",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleRow(
                title = "Rule enabled",
                supporting = "Alert from $label status or above.",
                checked = preferences.thermalRuleEnabled,
                onCheckedChange = onEnabledChanged,
            )
            ThresholdStepper(
                value = preferences.thermalStatusThreshold,
                valueLabel = label,
                min = 1,
                max = 6,
                step = 1,
                enabled = preferences.thermalRuleEnabled,
                onValueChanged = onThresholdChanged,
            )
        }
    }
}

@Composable
private fun ThresholdStepper(
    value: Int,
    valueLabel: String,
    min: Int,
    max: Int,
    step: Int,
    enabled: Boolean,
    onValueChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = valueLabel,
            color = if (enabled) DeckText else DeckMuted,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.widthIn(min = 130.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                enabled = enabled && value > min,
                onClick = { onValueChanged((value - step).coerceAtLeast(min)) },
            ) { Text("−") }
            TextButton(
                enabled = enabled && value < max,
                onClick = { onValueChanged((value + step).coerceAtMost(max)) },
            ) { Text("+") }
        }
    }
}

@Composable
private fun ActiveAlertRow(alert: SystemAlert) {
    val accent = when (alert.severity) {
        AlertSeverity.Info -> DeckBlue
        AlertSeverity.Warning -> DeckAmber
        AlertSeverity.Critical -> DeckRed
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(alert.metricValue, color = accent, style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(alert.title, color = DeckText, style = MaterialTheme.typography.titleSmall)
            Text(alert.message, color = DeckMuted, style = MaterialTheme.typography.bodySmall)
            Text(alert.ruleDescription, color = accent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun notificationPermissionGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}
