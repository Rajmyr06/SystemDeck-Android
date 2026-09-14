package dev.rajmyr.systemdeck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DeckColorScheme = darkColorScheme(
    primary = DeckBlue,
    secondary = DeckCyan,
    background = DeckBackground,
    surface = DeckSurface,
    surfaceVariant = DeckSurfaceElevated,
    onPrimary = DeckBackground,
    onSecondary = DeckBackground,
    onBackground = DeckText,
    onSurface = DeckText,
    outline = DeckBorder,
    error = DeckRed,
)

private val DeckTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        color = DeckText,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        color = DeckText,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = DeckText,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = DeckText,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = DeckText,
    ),
)

private val DeckShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun SystemDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DeckColorScheme,
        typography = DeckTypography,
        shapes = DeckShapes,
        content = content,
    )
}
