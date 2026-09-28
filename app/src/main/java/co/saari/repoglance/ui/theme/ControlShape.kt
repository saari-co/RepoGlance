package co.saari.repoglance.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Edge { NONE, HAIRLINE, STATE }

enum class Fill { FILLED, TONAL, OUTLINED }

enum class CardFill { ELEVATED, TONAL, OUTLINED }

@Immutable
data class ControlShape(
    val chip: Shape,
    val chipEdge: Edge,
    val chipFill: Fill,
    val pill: Shape,
    val pillEdge: Edge,
    val pillFill: Fill,
    val button: Shape,
    val buttonFill: Fill,
    val banner: Shape,
    val bannerEdge: Edge,
    val bannerFill: Fill,
    val card: Shape,
    val cardFill: CardFill,
    val secondaryEdge: Boolean = true,
    val stateEdgeWidth: Dp = 1.5.dp,
) {
    companion object {
        val Material = ControlShape(
            chip = RoundedCornerShape(8.dp),
            chipEdge = Edge.HAIRLINE,
            chipFill = Fill.OUTLINED,
            pill = CircleShape,
            pillEdge = Edge.NONE,
            pillFill = Fill.TONAL,
            button = CircleShape,
            buttonFill = Fill.FILLED,
            banner = RectangleShape,
            bannerEdge = Edge.NONE,
            bannerFill = Fill.TONAL,
            card = RoundedCornerShape(12.dp),
            cardFill = CardFill.ELEVATED,
        )

        val Tonal = ControlShape(
            chip = RoundedCornerShape(8.dp),
            chipEdge = Edge.NONE,
            chipFill = Fill.TONAL,
            pill = CircleShape,
            pillEdge = Edge.NONE,
            pillFill = Fill.TONAL,
            button = CircleShape,
            buttonFill = Fill.TONAL,
            banner = RoundedCornerShape(16.dp),
            bannerEdge = Edge.NONE,
            bannerFill = Fill.TONAL,
            card = RoundedCornerShape(24.dp),
            cardFill = CardFill.TONAL,
            secondaryEdge = false,
        )

        val Default = Tonal
    }
}

val LocalControlShape = staticCompositionLocalOf { ControlShape.Default }

private const val STATE_EDGE_ALPHA = 0.65f

@Composable
private fun edgeStroke(edge: Edge, state: Color, width: Dp): BorderStroke? = when (edge) {
    Edge.NONE -> null
    Edge.HAIRLINE -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Edge.STATE -> BorderStroke(width, state.copy(alpha = STATE_EDGE_ALPHA))
}

@Composable
fun ControlChip(label: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val shape = LocalControlShape.current
    AssistChip(
        onClick = {},
        label = label,
        modifier = modifier,
        shape = shape.chip,
        border = edgeStroke(shape.chipEdge, MaterialTheme.colorScheme.outline, shape.stateEdgeWidth),
        colors = if (shape.chipFill == Fill.OUTLINED) {
            AssistChipDefaults.assistChipColors()
        } else {
            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        },
    )
}

@Composable
fun StatusPill(tone: StatusTone, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = LocalControlShape.current
    Surface(
        color = if (shape.pillFill == Fill.OUTLINED) Color.Transparent else tone.container,
        contentColor = tone.onContainer,
        shape = shape.pill,
        border = edgeStroke(shape.pillEdge, tone.ink, shape.stateEdgeWidth),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun StatusBanner(tone: StatusTone, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = LocalControlShape.current
    Surface(
        color = if (shape.bannerFill == Fill.OUTLINED) Color.Transparent else tone.container,
        contentColor = tone.onContainer,
        shape = shape.banner,
        border = edgeStroke(shape.bannerEdge, tone.ink, shape.stateEdgeWidth),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = LocalControlShape.current
    when (shape.buttonFill) {
        Fill.FILLED -> Button(onClick, modifier, enabled, shape = shape.button, content = content)
        Fill.TONAL -> Button(
            onClick,
            modifier,
            enabled,
            shape = shape.button,
            colors = ButtonDefaults.filledTonalButtonColors(),
            content = content,
        )
        Fill.OUTLINED -> OutlinedButton(
            onClick,
            modifier,
            enabled,
            shape = shape.button,
            border = BorderStroke(
                shape.stateEdgeWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = STATE_EDGE_ALPHA),
            ),
            content = content,
        )
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = LocalControlShape.current
    OutlinedButton(
        onClick,
        modifier,
        enabled,
        shape = shape.button,
        border = if (shape.secondaryEdge) ButtonDefaults.outlinedButtonBorder(enabled) else null,
        content = content,
    )
}

@Composable
fun ControlCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = LocalControlShape.current
    val body: @Composable ColumnScope.() -> Unit = { content() }
    when (shape.cardFill) {
        CardFill.ELEVATED -> Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape.card,
            colors = CardDefaults.elevatedCardColors(),
            elevation = CardDefaults.elevatedCardElevation(),
            content = body,
        )
        CardFill.TONAL -> Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            content = body,
        )
        CardFill.OUTLINED -> Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape.card,
            colors = CardDefaults.outlinedCardColors(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            content = body,
        )
    }
}
