package dev.rajmyr.systemdeck.feature.sensors

import android.hardware.Sensor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.data.sensors.SensorDeviceSnapshot
import dev.rajmyr.systemdeck.data.sensors.SensorSnapshot
import dev.rajmyr.systemdeck.ui.components.CollectorBadge
import dev.rajmyr.systemdeck.ui.components.DeckDataRow
import dev.rajmyr.systemdeck.ui.components.DeckInfoBox
import dev.rajmyr.systemdeck.ui.components.DeckPageHeader
import dev.rajmyr.systemdeck.ui.components.DeckSectionLabel
import dev.rajmyr.systemdeck.ui.components.DeckPanel
import dev.rajmyr.systemdeck.ui.components.DeckSummaryCard
import dev.rajmyr.systemdeck.ui.components.DeckSummaryGrid
import dev.rajmyr.systemdeck.ui.components.DeckSummarySpec
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import java.util.Locale

@Composable
fun SensorsScreen(
    sensors: SensorSnapshot,
    compact: Boolean,
) {
    val gap = if (compact) 9.dp else 12.dp
    val groups = sensors.sensors.groupBy(::sensorGroup)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
        item {
            DeckPageHeader(
                title = "Sensors",
                subtitle = "Hardware sensors and live signals.",
                trailing = "Live · 1 s",
            )
        }


        item {
            DeckSummaryGrid(
                items = listOf(
                    DeckSummarySpec(
                        label = "Sensors",
                        value = sensors.totalSensors.toString(),
                        supporting = "SensorManager inventory",
                        code = "ALL",
                        accent = DeckBlue,
                    ),
                    DeckSummarySpec(
                        label = "Live",
                        value = sensors.liveSensors.toString(),
                        supporting = "Currently delivering values",
                        code = "LIVE",
                        accent = DeckGreen,
                    ),
                    DeckSummarySpec(
                        label = "Wake-up sensors",
                        value = sensors.wakeUpSensors.toString(),
                        supporting = "May wake the application processor",
                        code = "WAKE",
                        accent = DeckCyan,
                    ),
                    DeckSummarySpec(
                        label = "Dynamic sensors",
                        value = sensors.dynamicSensors.toString(),
                        supporting = "Attachable at runtime",
                        code = "DYN",
                        accent = DeckBlue,
                    ),
                ),
                gap = gap,
                minItemWidth = 220.dp,
                maxColumns = 4,
            )
        }

        item {
            DeckPanel(
                title = "Live signals",
                subtitle = "Motion, orientation and light.",
                code = "SIG",
                accent = DeckGreen,
            ) {
                val live = sensors.sensors.filter { it.values.isNotEmpty() }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (live.isEmpty()) {
                        Text(
                            text = "Waiting for live sensor events.",
                            color = DeckMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        live.take(10).forEach { sensor ->
                            DeckDataRow(
                                label = shortName(sensor),
                                description = "${sensor.vendor} · ${sensor.reportingMode}",
                                value = liveValue(sensor),
                                accent = accentFor(sensor),
                            )
                        }
                    }
                }
            }
        }

        listOf("Motion & orientation", "Environment", "Activity", "Other").forEach { group ->
            val groupSensors = groups[group].orEmpty()
            if (groupSensors.isNotEmpty()) {
                item {
                    DeckPanel(
                        title = group,
                        subtitle = "${groupSensors.size} sensors",
                        code = when (group) {
                            "Motion & orientation" -> "MOT"
                            "Environment" -> "ENV"
                            "Activity" -> "ACT"
                            else -> "HW"
                        },
                        accent = when (group) {
                            "Motion & orientation" -> DeckBlue
                            "Environment" -> DeckCyan
                            "Activity" -> DeckGreen
                            else -> DeckBlue
                        },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            groupSensors.forEach { sensor ->
                                DeckDataRow(
                                    label = shortName(sensor),
                                    description = "${sensor.vendor} · ${sensor.reportingMode}${if (sensor.wakeUp) " · wake-up" else ""}",
                                    value = if (sensor.values.isNotEmpty()) liveValue(sensor) else "Inventory only",
                                    accent = accentFor(sensor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sensorGroup(sensor: SensorDeviceSnapshot): String = when (sensor.type) {
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_GAME_ROTATION_VECTOR,
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
    Sensor.TYPE_ORIENTATION -> "Motion & orientation"

    Sensor.TYPE_LIGHT,
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_PROXIMITY,
    Sensor.TYPE_RELATIVE_HUMIDITY,
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Environment"

    Sensor.TYPE_STEP_COUNTER,
    Sensor.TYPE_STEP_DETECTOR,
    Sensor.TYPE_SIGNIFICANT_MOTION,
    SENSOR_TYPE_TILT_DETECTOR,
    SENSOR_TYPE_PICK_UP_GESTURE,
    Sensor.TYPE_STATIONARY_DETECT,
    Sensor.TYPE_MOTION_DETECT -> "Activity"

    else -> "Other"
}

private fun shortName(sensor: SensorDeviceSnapshot): String = when (sensor.type) {
    Sensor.TYPE_ACCELEROMETER -> "Accel"
    Sensor.TYPE_GYROSCOPE -> "Gyro"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic"
    Sensor.TYPE_LIGHT -> "Light"
    Sensor.TYPE_GRAVITY -> "Gravity"
    Sensor.TYPE_LINEAR_ACCELERATION -> "Linear"
    Sensor.TYPE_ROTATION_VECTOR -> "Rotation"
    Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game rot"
    Sensor.TYPE_STEP_COUNTER -> "Steps"
    Sensor.TYPE_STEP_DETECTOR -> "Step det"
    else -> sensor.name.take(12)
}

private fun liveValue(sensor: SensorDeviceSnapshot): String {
    if (sensor.values.isEmpty()) return "—"
    val values = sensor.values.take(4).joinToString(" · ") { String.format(Locale.US, "%.2f", it) }
    return when (sensor.type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION -> "$values m/s²"
        Sensor.TYPE_GYROSCOPE -> "$values rad/s"
        Sensor.TYPE_MAGNETIC_FIELD -> "$values µT"
        Sensor.TYPE_LIGHT -> "${String.format(Locale.US, "%.1f", sensor.values.first())} lx"
        else -> values
    }
}

private fun accentFor(sensor: SensorDeviceSnapshot) = when (sensorGroup(sensor)) {
    "Motion & orientation" -> DeckBlue
    "Environment" -> DeckCyan
    "Activity" -> DeckGreen
    else -> DeckBlue
}

// Public SensorManager inventory can report these type IDs even when a named
// constant is absent from the compile SDK surface. They are used only for UI
// classification; live subscription still uses SensorManager's real objects.
private const val SENSOR_TYPE_TILT_DETECTOR = 22
private const val SENSOR_TYPE_PICK_UP_GESTURE = 25
