package dev.rajmyr.systemdeck.feature.diagnostics

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.ui.theme.DeckBorder
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckMuted

@Composable
fun DiagnosticsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("DIAGNOSTICS", style = MaterialTheme.typography.titleLarge)
        Text(
            "Only foundation checks are active in Phase 5. Collector health begins in Phase 6.",
            color = DeckMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DeckBorder, RoundedCornerShape(6.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DiagnosticRow("app.shell", "OK")
            DiagnosticRow("device.provider", "OK")
            DiagnosticRow("preferences.datastore", "OK")
            DiagnosticRow("telemetry.collectors", "NOT WIRED")
            DiagnosticRow("background.service", "NOT ENABLED")
            DiagnosticRow("root", "NOT REQUIRED")
        }
    }
}

@Composable
private fun DiagnosticRow(name: String, state: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, fontFamily = FontFamily.Monospace)
        Text(
            state,
            color = DeckCyan,
            fontFamily = FontFamily.Monospace,
        )
    }
}
