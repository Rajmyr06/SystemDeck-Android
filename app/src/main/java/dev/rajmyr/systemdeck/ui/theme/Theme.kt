package dev.rajmyr.systemdeck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    headlineSmall = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.45).sp,
        color = DeckText,
    ),
    titleLarge = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
        color = DeckText,
    ),
    titleMedium = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.05).sp,
        color = DeckText,
    ),
    bodyLarge = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
        color = DeckText,
    ),
    bodyMedium = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        color = DeckText,
    ),
    bodySmall = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp,
        color = DeckMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = DeckFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.08.sp,
        color = DeckText,
    ),
    labelMedium = TextStyle(
        fontFamily = DeckFonts.Data,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp,
        fontFeatureSettings = "tnum",
        color = DeckText,
    ),
)

private val DeckShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(9.dp),
    extraLarge = RoundedCornerShape(10.dp),
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
