package dev.rajmyr.systemdeck.ui.theme

import androidx.compose.ui.unit.dp

object DeckSpacing {
    val Xxs = 3.dp
    val Xs = 5.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
}

object DeckRadius {
    val Small = 7.dp
    val Medium = 10.dp
    val Large = 12.dp
}

object DeckLayout {
    val SidebarWidth = 188.dp
    val TopBarHeight = 52.dp
    val StatusBarHeight = 32.dp
    val ContentMaxGap = 12.dp

    val CompactBreakpoint = 840.dp
    val PanelStackBreakpoint = 760.dp
    val SummaryCardMinWidth = 220.dp
}


object DeckMotion {
    const val FastMillis = 140
    const val StandardMillis = 220
}


object DeckAccessibility {
    // Android-first touch target while keeping the visual treatment compact.
    val MinTouchTarget = 48.dp
    val DenseControlGap = 4.dp
}
