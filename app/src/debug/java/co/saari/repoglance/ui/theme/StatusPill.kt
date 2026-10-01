package co.saari.repoglance.ui.theme

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
