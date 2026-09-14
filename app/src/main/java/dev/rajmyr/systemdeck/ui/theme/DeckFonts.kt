package dev.rajmyr.systemdeck.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Keep SystemDeck dependent only on Android system fonts.
 *
 * UI and telemetry both use the native sans-serif family. Telemetry alignment
 * is handled by tabular-number OpenType features in the typography styles,
 * avoiding the harsher default Android monospace look.
 */
object DeckFonts {
    val Ui: FontFamily = FontFamily.SansSerif
    val Data: FontFamily = FontFamily.SansSerif

    // Compatibility alias for existing call sites.
    val Mono: FontFamily = Data
}
