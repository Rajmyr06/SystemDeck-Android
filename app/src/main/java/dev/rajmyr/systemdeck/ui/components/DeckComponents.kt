package dev.rajmyr.systemdeck.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import dev.rajmyr.systemdeck.ui.theme.DeckAmber
import dev.rajmyr.systemdeck.ui.theme.DeckBlue
import dev.rajmyr.systemdeck.ui.theme.DeckBorder
import dev.rajmyr.systemdeck.ui.theme.DeckCyan
import dev.rajmyr.systemdeck.ui.theme.DeckFonts
import dev.rajmyr.systemdeck.ui.theme.DeckGreen
import dev.rajmyr.systemdeck.ui.theme.DeckHairline
import dev.rajmyr.systemdeck.ui.theme.DeckMuted
import dev.rajmyr.systemdeck.ui.theme.DeckRadius
import dev.rajmyr.systemdeck.ui.theme.DeckRed
import dev.rajmyr.systemdeck.ui.theme.DeckSpacing
import dev.rajmyr.systemdeck.ui.theme.DeckSurface
import dev.rajmyr.systemdeck.ui.theme.DeckSurfaceElevated
import dev.rajmyr.systemdeck.ui.theme.DeckSurfaceSubtle
import dev.rajmyr.systemdeck.ui.theme.DeckText
import dev.rajmyr.systemdeck.ui.theme.DeckTertiary


val LocalDeckMetricDescriptions = staticCompositionLocalOf { false }

private val PanelShape = RoundedCornerShape(DeckRadius.Large)
private val CardShape = RoundedCornerShape(DeckRadius.Medium)

private fun String.isMetricPlaceholder(): Boolean {
    val normalized = trim()
    return normalized.isEmpty() ||
        normalized == "—" ||
        normalized == "-" ||
        normalized == "–" ||
        normalized.equals("n/a", ignoreCase = true)
}

@Composable
fun DeckPageHeader(
    title: String,
    subtitle: String,
    trailing: String? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 560.dp
        val status = trailing?.let {
            when {
                it.startsWith("Live", ignoreCase = true) -> "Live"
                it.contains("local", ignoreCase = true) || it.contains("device", ignoreCase = true) -> "Local"
                else -> it
            }
        }

        val showDetails = LocalDeckMetricDescriptions.current
        val titleBlock: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(if (showDetails) 3.dp else 0.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = DeckFonts.Ui,
                )
                if (showDetails && subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = DeckMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = DeckFonts.Ui,
                        maxLines = if (narrow) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (narrow) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                titleBlock()
                status?.let { DeckStatusPill(it) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.weight(1f)) { titleBlock() }
                status?.let {
                    Spacer(Modifier.width(16.dp))
                    DeckStatusPill(it)
                }
            }
        }
    }
}

@Composable
private fun DeckStatusPill(text: String) {
    val tone = when {
        text.contains("offline", ignoreCase = true) || text.contains("off", ignoreCase = true) -> DeckRed
        text.contains("experimental", ignoreCase = true) -> DeckAmber
        text.contains("live", ignoreCase = true) -> DeckGreen
        text.contains("local", ignoreCase = true) -> DeckBlue
        else -> DeckCyan
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).background(tone, CircleShape))
        Text(
            text = text,
            color = DeckMuted,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Ui,
        )
    }
}

@Composable
fun DeckPanel(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = DeckCyan,
    subtitle: String? = null,
    trailing: String? = null,
    code: String? = null,
    content: @Composable () -> Unit,
) {
    val showDetails = LocalDeckMetricDescriptions.current
    Column(
        modifier = modifier
            .background(DeckSurface, PanelShape)
            .border(1.dp, DeckHairline.copy(alpha = 0.82f), PanelShape)
            .animateContentSize()
            .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (!showDetails || subtitle.isNullOrBlank()) 18.dp else 34.dp)
                    .background(accent.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    color = DeckText,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DeckFonts.Ui,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf { showDetails && it.isNotBlank() }?.let { supporting ->
                    Text(
                        text = supporting,
                        color = DeckMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = DeckFonts.Ui,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val meta = trailing ?: code
            meta?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = if (trailing != null) DeckTertiary else accent.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = DeckFonts.Ui,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun DeckSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    code: String? = null,
    accent: Color = DeckCyan,
) {
    BoxWithConstraints(
        modifier = modifier
            .semantics(mergeDescendants = true) { }
            .background(DeckSurfaceElevated, CardShape)
            .border(1.dp, DeckHairline.copy(alpha = 0.78f), CardShape)
            .animateContentSize(),
    ) {
        val tight = maxWidth < 210.dp
        Column(
            modifier = Modifier.padding(
                horizontal = if (tight) 12.dp else 14.dp,
                vertical = if (tight) 11.dp else 13.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(5.dp).background(accent, CircleShape))
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = DeckMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DeckFonts.Ui,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                code?.takeIf { !tight && it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = DeckTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = DeckFonts.Ui,
                    )
                }
            }
            Text(
                text = value,
                color = DeckText,
                fontSize = if (tight) 20.sp else 22.sp,
                lineHeight = if (tight) 23.sp else 25.sp,
                fontFamily = DeckFonts.Mono,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.takeIf { LocalDeckMetricDescriptions.current && it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    color = DeckTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = DeckFonts.Ui,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DeckMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    accent: Color = DeckCyan,
) {
    DeckSummaryCard(
        label = label,
        value = value,
        modifier = modifier,
        supporting = supporting,
        accent = accent,
    )
}

@Composable
fun DeckGlyph(
    text: String,
    accent: Color = DeckCyan,
    compact: Boolean = false,
) {
    val size = if (compact) 30.dp else 32.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = accent,
            fontFamily = DeckFonts.Mono,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 8.sp else 8.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun DeckProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    accent: Color = DeckBlue,
    height: Int = 5,
) {
    val safeFraction = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(DeckSurfaceSubtle, RoundedCornerShape(height.dp)),
    ) {
        if (safeFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeFraction)
                    .height(height.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.72f), accent),
                        ),
                        RoundedCornerShape(height.dp),
                    ),
            )
        }
    }
}

@Composable
fun CollectorBadge(status: CollectorStatus) {
    val (label, color) = when (status) {
        CollectorStatus.Ok -> "Active" to DeckGreen
        CollectorStatus.Degraded -> "Degraded" to DeckCyan
        CollectorStatus.Unavailable -> "Unavailable" to DeckRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Ui,
        )
    }
}

@Composable
fun KeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DeckText,
) {
    if (value.isMetricPlaceholder()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            color = DeckMuted,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DeckFonts.Ui,
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Mono,
        )
    }
}

@Composable
fun DeckDataRow(
    label: String,
    description: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DeckText,
    accent: Color = DeckBlue,
) {
    if (value.isMetricPlaceholder()) return
    val showDescription = LocalDeckMetricDescriptions.current && description.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (showDescription) 5.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(if (showDescription) 26.dp else 14.dp)
                .background(accent.copy(alpha = 0.72f), RoundedCornerShape(1.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                color = DeckText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDescription) {
                Text(
                    text = description,
                    color = DeckTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Mono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DeckInfoBox(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accent: Color = DeckBlue,
) {
    Row(
        modifier = modifier
            .background(DeckSurfaceSubtle, CardShape)
            .border(1.dp, DeckHairline.copy(alpha = 0.72f), CardShape)
            .animateContentSize()
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(30.dp)
                .background(accent.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = DeckText,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = DeckMuted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DeckMetaItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = DeckCyan,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            label,
            color = DeckMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            value,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DeckFonts.Mono,
        )
    }
}


@Composable
fun DeckSectionLabel(
    title: String,
    supporting: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = DeckText,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = DeckFonts.Ui,
            fontWeight = FontWeight.SemiBold,
        )
        supporting?.takeIf { LocalDeckMetricDescriptions.current && it.isNotBlank() }?.let {
            Text(
                text = it,
                color = DeckMuted,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = DeckFonts.Ui,
            )
        }
    }
}

@Composable
fun DeckAdaptiveGrid(
    itemCount: Int,
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 220.dp,
    maxColumns: Int = itemCount.coerceAtLeast(1),
    gap: Dp = 12.dp,
    itemContent: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    if (itemCount <= 0) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val computed = ((maxWidth.value + gap.value) / (minItemWidth.value + gap.value))
            .toInt()
            .coerceAtLeast(1)
        val columns = minOf(itemCount, maxColumns.coerceAtLeast(1), computed)

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            var index = 0
            while (index < itemCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    repeat(columns) { column ->
                        val current = index + column
                        if (current < itemCount) {
                            itemContent(current, Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                index += columns
            }
        }
    }
}

data class DeckSummarySpec(
    val label: String,
    val value: String,
    val supporting: String? = null,
    val code: String? = null,
    val accent: Color = DeckCyan,
)

@Composable
fun DeckSummaryGrid(
    items: List<DeckSummarySpec>,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    minItemWidth: Dp = 220.dp,
    maxColumns: Int = items.size.coerceAtLeast(1),
) {
    val visibleItems = items.filterNot { it.value.isMetricPlaceholder() }
    if (visibleItems.isEmpty()) return

    DeckAdaptiveGrid(
        itemCount = visibleItems.size,
        modifier = modifier,
        minItemWidth = minItemWidth,
        maxColumns = minOf(maxColumns, visibleItems.size),
        gap = gap,
    ) { index, itemModifier ->
        val item = visibleItems[index]
        DeckSummaryCard(
            label = item.label,
            value = item.value,
            modifier = itemModifier,
            supporting = item.supporting,
            code = item.code,
            accent = item.accent,
        )
    }
}

@Composable
fun DeckSparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    accent: Color = DeckBlue,
) {
    val finite = values.filter { it.isFinite() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        if (finite.size < 2) {
            val centerY = size.height / 2f
            drawLine(
                color = DeckHairline,
                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                strokeWidth = 1.dp.toPx(),
            )
            return@Canvas
        }

        val min = finite.minOrNull() ?: return@Canvas
        val max = finite.maxOrNull() ?: return@Canvas
        val span = (max - min).takeIf { it > 0.0001 } ?: 1.0
        val topInset = size.height * 0.12f
        val bottomInset = size.height * 0.12f
        val usableHeight = size.height - topInset - bottomInset
        val step = size.width / (finite.size - 1).coerceAtLeast(1)

        drawLine(
            color = DeckHairline.copy(alpha = 0.72f),
            start = androidx.compose.ui.geometry.Offset(0f, topInset),
            end = androidx.compose.ui.geometry.Offset(size.width, topInset),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = DeckHairline.copy(alpha = 0.72f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height - bottomInset),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height - bottomInset),
            strokeWidth = 1.dp.toPx(),
        )

        val linePath = Path()
        val areaPath = Path()
        var lastX = 0f
        var lastY = size.height / 2f

        finite.forEachIndexed { index, value ->
            val x = index * step
            val normalized = ((value - min) / span).toFloat().coerceIn(0f, 1f)
            val y = size.height - bottomInset - normalized * usableHeight
            if (index == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(x, size.height - bottomInset)
                areaPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }

        areaPath.lineTo(lastX, size.height - bottomInset)
        areaPath.close()

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.0f)),
                startY = topInset,
                endY = size.height - bottomInset,
            ),
            style = Fill,
        )
        drawPath(
            path = linePath,
            color = accent,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
        drawCircle(
            color = DeckSurface,
            radius = 3.4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY),
        )
        drawCircle(
            color = accent,
            radius = 2.1.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY),
        )
    }
}

