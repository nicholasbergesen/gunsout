package com.nicholasbergesen.gunsout.feature.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nicholasbergesen.gunsout.core.text.formatOneDecimalOrInt
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistoryPoint
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistoryRange
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistorySeries
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.ThemedCard
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val DAY_TOOLTIP_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM")
private val MONTH_TOOLTIP_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy")
private val DAY_AXIS_FORMAT = DateTimeFormatter.ofPattern("d MMM")
private val MONTH_AXIS_FORMAT = DateTimeFormatter.ofPattern("MMM")

@Composable
internal fun ProteinHistoryCard(
    series: ProteinHistorySeries,
    onRangeSelected: (ProteinHistoryRange) -> Unit
) {
    ThemedCard {
        SectionLabel("Protein history")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProteinHistoryRange.entries.forEach { range ->
                FilterChip(
                    selected = series.range == range,
                    onClick = { onRangeSelected(range) },
                    label = {
                        Text(
                            when (range) {
                                ProteinHistoryRange.WEEK -> "1W"
                                ProteinHistoryRange.MONTH -> "1M"
                                ProteinHistoryRange.YEAR -> "1Y"
                            }
                        )
                    }
                )
            }
        }

        if (series.points.none { it.loggedGrams != null }) {
            Text(
                "Add protein to start your history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ProteinBarChart(series)
        }
    }
}

@Composable
private fun ProteinBarChart(series: ProteinHistorySeries) {
    val points = series.points
    var selectedIndex by remember(series) {
        mutableStateOf(points.indexOfLast { it.loggedGrams != null }.takeIf { it >= 0 })
    }
    val selected = selectedIndex?.let(points::getOrNull)
    val values = points.flatMap { point ->
        listOfNotNull(point.loggedGrams, point.targetGrams)
    }
    val maxValue = max(1.0, (values.maxOrNull() ?: 1.0) * 1.1)
    val primary = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.secondary
    val targetColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    selected?.let {
        Text(
            formatPointTooltip(series.range, it),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "${formatOneDecimalOrInt(maxValue)} g",
        style = MaterialTheme.typography.labelSmall,
        color = muted
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics {
                contentDescription = selected?.let {
                    formatPointTooltip(series.range, it)
                } ?: "Protein history chart"
            }
            .pointerInput(series) {
                detectTapGestures { offset ->
                    if (points.isEmpty() || size.width == 0) return@detectTapGestures
                    val slotWidth = size.width.toFloat() / points.size
                    val index = (offset.x / slotWidth).toInt().coerceIn(points.indices)
                    if (points[index].loggedGrams != null) selectedIndex = index
                }
            }
    ) {
        val width = size.width
        val height = size.height
        repeat(5) { index ->
            val y = height * index / 4f
            drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        }
        val slotWidth = width / points.size.coerceAtLeast(1)
        val barWidth = (slotWidth * 0.62f).coerceAtLeast(2.dp.toPx())
        points.forEachIndexed { index, point ->
            val centerX = slotWidth * (index + 0.5f)
            point.loggedGrams?.let { grams ->
                val barHeight = (grams / maxValue * height).toFloat().coerceIn(1f, height)
                drawRect(
                    color = if (selectedIndex == index) selectedColor else primary,
                    topLeft = Offset(centerX - barWidth / 2f, height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
            point.targetGrams?.let { target ->
                val y = height - (target / maxValue * height).toFloat().coerceIn(0f, height)
                drawLine(
                    color = targetColor,
                    start = Offset(centerX - barWidth * 0.65f, y),
                    end = Offset(centerX + barWidth * 0.65f, y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(axisLabel(series.range, points.first()), style = MaterialTheme.typography.labelSmall)
        Text(
            axisLabel(series.range, points[points.lastIndex / 2]),
            style = MaterialTheme.typography.labelSmall
        )
        Text(axisLabel(series.range, points.last()), style = MaterialTheme.typography.labelSmall)
    }
    Text(
        "Bars show logged protein; dashes show the historical target. Missing days are gaps.",
        style = MaterialTheme.typography.bodySmall,
        color = muted
    )
}

private fun formatPointTooltip(
    range: ProteinHistoryRange,
    point: ProteinHistoryPoint
): String {
    val period = when (range) {
        ProteinHistoryRange.YEAR -> point.startDate.format(MONTH_TOOLTIP_FORMAT)
        else -> point.startDate.format(DAY_TOOLTIP_FORMAT)
    }
    val logged = formatOneDecimalOrInt(point.loggedGrams ?: 0.0)
    val target = point.targetGrams?.let {
        " · target ${formatOneDecimalOrInt(it)} g"
    }.orEmpty()
    return if (range == ProteinHistoryRange.YEAR) {
        "$period · $logged g/day across ${point.loggedDays} logged days$target"
    } else {
        "$period · $logged g$target"
    }
}

private fun axisLabel(range: ProteinHistoryRange, point: ProteinHistoryPoint): String =
    point.startDate.format(
        if (range == ProteinHistoryRange.YEAR) MONTH_AXIS_FORMAT else DAY_AXIS_FORMAT
    )
