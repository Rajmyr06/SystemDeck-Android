package dev.rajmyr.systemdeck.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rajmyr.systemdeck.ui.theme.DeckBorder
import dev.rajmyr.systemdeck.ui.theme.DeckMuted

@Composable
fun SettingsScreen(
    compactDensity: Boolean,
    onCompactDensityChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.titleLarge)
        Text(
            "Phase 5 persists one real preference through DataStore to validate the settings pipeline.",
            color = DeckMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DeckBorder, RoundedCornerShape(6.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Compact density", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Reduces spacing without shrinking touch targets.",
                    color = DeckMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = compactDensity,
                onCheckedChange = onCompactDensityChanged,
            )
        }
    }
}
