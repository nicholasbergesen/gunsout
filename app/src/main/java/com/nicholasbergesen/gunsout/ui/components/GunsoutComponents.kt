package com.nicholasbergesen.gunsout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicholasbergesen.gunsout.ui.theme.LocalThemeStyle
import com.nicholasbergesen.gunsout.ui.theme.ThemeStyle
import com.nicholasbergesen.gunsout.ui.theme.accentCardBrushFor

@Composable
fun MockupScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

@Composable
fun BigValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    )
}

@Composable
fun AccentText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalThemeStyle.current
    val shape = MaterialTheme.shapes.medium
    val border = cardBorder(style)
    val elevation = cardElevation(style)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (accent) Color.Transparent else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = border,
        shadowElevation = elevation
    ) {
        val contentModifier = if (accent) {
            Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val brush = accentCardBrushFor(style, Size(size.width, size.height))
                    onDrawBehind { drawRect(brush) }
                }
                .padding(13.dp)
        } else {
            Modifier.fillMaxWidth().padding(13.dp)
        }
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun StatusChip(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChipButton(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrappingRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun ProgressPill(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .height(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

data class MetricItem(
    val label: String,
    val value: String,
    val sub: String? = null,
    val progress: Float? = null
)

@Composable
fun MetricGrid(items: List<MetricItem>, modifier: Modifier = Modifier) {
    ThemedCard(modifier = modifier) {
        val rows = items.chunked(if (items.size >= 3) 3 else 2)
        rows.forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionLabel(item.label)
                        Text(
                            item.value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        item.sub?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        item.progress?.let { ProgressPill(progress = it, modifier = Modifier.padding(top = 2.dp)) }
                    }
                }
                repeat((if (items.size >= 3) 3 else 2) - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

data class ListRow(
    val title: String,
    val subtitle: String? = null,
    val trail: String? = null,
    val positive: Boolean = false
)

@Composable
fun ThemedListGroup(
    title: String? = null,
    rows: List<ListRow>,
    modifier: Modifier = Modifier,
    onRowClick: ((Int) -> Unit)? = null
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        title?.let { SectionLabel(it, modifier = Modifier.padding(horizontal = 2.dp)) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = cardBorder(LocalThemeStyle.current),
            shadowElevation = cardElevation(LocalThemeStyle.current)
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    if (index > 0) DividerLine()
                    ListGroupRow(row = row, onClick = onRowClick?.let { { it(index) } })
                }
            }
        }
    }
}

@Composable
private fun ListGroupRow(row: ListRow, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier
                    .width(9.dp)
                    .height(9.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(Modifier.weight(1f)) {
                Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                row.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            row.trail?.let {
                if (row.positive) {
                    StatusChip(it, selected = true)
                } else {
                    TextButton(onClick = onClick ?: {}, enabled = onClick != null) { Text(it) }
                }
            }
        }
    }
    if (onClick == null) {
        content()
    } else {
        Surface(onClick = onClick, color = Color.Transparent, content = content)
    }
}

@Composable
fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun ActionRow(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    if (secondaryText == null) {
        Button(onClick = onPrimary, modifier = modifier.fillMaxWidth()) { Text(primaryText) }
        return
    }

    WrappingRow(modifier = modifier) {
        secondaryText?.let {
            OutlinedButton(onClick = onSecondary ?: {}) { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Button(onClick = onPrimary) { Text(primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun cardBorder(style: ThemeStyle): BorderStroke? = when (style) {
    ThemeStyle.NEO_BRUTALIST -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.outline)
    ThemeStyle.SOFT_PASTEL -> null
    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

private fun cardElevation(style: ThemeStyle) = when (style) {
    ThemeStyle.CLEAN_LIGHT_MINIMAL -> 1.dp
    ThemeStyle.GLASSMORPHISM -> 8.dp
    ThemeStyle.SOFT_PASTEL -> 6.dp
    else -> 0.dp
}
