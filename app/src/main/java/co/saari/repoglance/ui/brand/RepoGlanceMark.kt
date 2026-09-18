package co.saari.repoglance.ui.brand

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import co.saari.repoglance.R

private const val VIEWPORT = 24f
private const val LENS_CENTRE = 10.1f
private const val LENS_RADIUS = 6.7f
private const val RING_PERIOD_MS = 1600
private const val RING_STAGGER_MS = 550
private const val RING_GROWTH = 1.3f
private const val RING_ALPHA = 0.45f
private const val RING_STROKE = 0.02f

@Composable
fun RepoGlanceMark(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Icon(
        painter = painterResource(R.drawable.ic_repoglance_mark),
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
fun CheckingMark(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val ink = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!reducedMotion) {
            PingRings(ink, Modifier.matchParentSize())
        }
        RepoGlanceMark(modifier = Modifier.fillMaxSize(), tint = ink)
    }
}

@Composable
private fun PingRings(ink: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "checking-ping")
    val first by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RING_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "ring-1",
    )
    val second by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(RING_PERIOD_MS, easing = LinearEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset(RING_STAGGER_MS),
        ),
        label = "ring-2",
    )
    Canvas(modifier) {
        val unit = size.minDimension / VIEWPORT
        val centre = Offset(LENS_CENTRE * unit, LENS_CENTRE * unit)
        listOf(first, second).forEach { phase ->
            drawCircle(
                color = ink.copy(alpha = RING_ALPHA * (1f - phase)),
                radius = LENS_RADIUS * unit * (1f + RING_GROWTH * phase),
                center = centre,
                style = Stroke(width = size.minDimension * RING_STROKE),
            )
        }
    }
}
