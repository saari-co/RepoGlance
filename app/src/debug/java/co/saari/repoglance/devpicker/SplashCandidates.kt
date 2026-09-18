package co.saari.repoglance.devpicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * GrillTrack round `checking-splash-round-1`, decision `checking-splash-018`:
 * what the live screen shows while the GitHub session is unknown
 * (`LiveUiState.Checking`). Debug source set only; the picked mark and motion
 * move into app/src/main after the maintainer confirms.
 *
 * Two dependent slots, grilled sequentially: the MARK first (five form
 * languages, every one built around a magnifying glass and drawn as a single
 * monochrome shape so it can become the status-bar/notification alpha mask),
 * then the MOTION (five choreographies, not speed tweaks). Every candidate
 * renders on the production Checking canvas: the locked message stays, and
 * nothing that looks like a count, a sign-in button, or a signed-out state
 * is drawn, because Unknown never renders as zero or as signed-out.
 */
enum class MarkCandidate(val letter: String, val shortName: String, val description: String) {
    COMMIT_LENS(
        "A",
        "Commit lens",
        "stroke magnifier; the lens frames one git commit node on its line",
    ),
    GLANCE_EYE(
        "B",
        "Glance eye",
        "stroke magnifier; the lens is a half-lidded eye taking a glance",
    ),
    REPO_UNDER_GLASS(
        "C",
        "Repo under glass",
        "an outlined repo card with the magnifier resting over its corner",
    ),
    DIAL_LENS(
        "D",
        "Dial lens",
        "the lens ring is twelve ticks like an instrument dial; solid handle",
    ),
    TILE_CUTOUT(
        "E",
        "Tile cutout",
        "solid rounded tile with the magnifier cut out as negative space",
    ),
    COMMIT_EYE(
        "A+B",
        "Commit eye",
        "HYBRID PREVIEW: stroke magnifier; inside, the GitHub-style almond eye whose pupil is the hollow commit node " +
            "sitting on its vertical commit line",
    ),
    ;

    val isHybrid: Boolean get() = this == COMMIT_EYE

    val label: String get() = "$letter $shortName"
}

enum class MotionCandidate(val letter: String, val shortName: String, val description: String) {
    SWEEP(
        "A",
        "Sweep",
        "the magnifier glides across three faint dots; the dot under the lens lights up (looping)",
    ),
    REVEAL(
        "B",
        "Draw-in",
        "the mark is revealed clockwise like a stroke being drawn, then settles into focus (once)",
    ),
    PEEK(
        "C",
        "Double-take",
        "the magnifier tilts on its handle for a quick look, pauses, and swings back with a small overshoot (looping)",
    ),
    PING(
        "D",
        "Ping",
        "the mark holds still while two rings pulse out of the lens, like a sonar ping to GitHub (looping)",
    ),
    HOLD(
        "E",
        "Hold",
        "nothing moves; the message fades in only after 400 ms so a fast check never flashes text",
    ),
    ;

    val label: String get() = "$letter $shortName"
}

private const val CHECKING_MESSAGE = "Checking your GitHub session…"
private const val TEXT_DELAY_MS = 400
private const val STATUS_ICON_DP = 20

/**
 * The production Checking canvas with one mark and one motion applied, plus a
 * strip that renders the same mark at status-bar size as a white alpha mask on
 * a dark bar and as a dark mask on a light bar. That strip is the honest
 * fidelity test for "reads well as the small Pixel status-bar icon": Android
 * draws notification and Quick Settings icons from the alpha channel only.
 */
@Composable
internal fun SplashCandidateCanvas(mark: MarkCandidate, motion: MotionCandidate, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier.fillMaxSize().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedMark(
                mark = mark,
                motion = motion,
                color = color,
                background = background,
                modifier = Modifier.size(140.dp).testTag("repoglance:splash-mark"),
            )
            Spacer(Modifier.height(24.dp))
            MessageLine(motion)
        }
        StatusBarStrip(mark, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MessageLine(motion: MotionCandidate) {
    val alpha = remember { Animatable(if (motion == MotionCandidate.HOLD) 0f else 1f) }
    LaunchedEffect(motion) {
        if (motion == MotionCandidate.HOLD) {
            alpha.snapTo(0f)
            alpha.animateTo(1f, tween(durationMillis = 250, delayMillis = TEXT_DELAY_MS))
        }
    }
    Text(
        CHECKING_MESSAGE,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.alpha(alpha.value).testTag("repoglance:splash-message"),
    )
}

@Composable
private fun StatusBarStrip(mark: MarkCandidate, modifier: Modifier = Modifier) {
    val dark = Color(0xFF1B1B1F)
    val light = Color(0xFFF2F2F6)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "status-bar size: ${STATUS_ICON_DP}dp alpha mask",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row {
            StatusBar(mark, bar = dark, ink = Color.White, tag = "dark")
            Spacer(Modifier.width(8.dp))
            StatusBar(mark, bar = light, ink = Color(0xFF1B1B1F), tag = "light")
        }
    }
}

@Composable
private fun StatusBar(mark: MarkCandidate, bar: Color, ink: Color, tag: String) {
    Row(
        modifier = Modifier
            .background(bar, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("repoglance:splash-statusbar-$tag"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("12:30", style = MaterialTheme.typography.labelMedium, color = ink)
        Spacer(Modifier.width(10.dp))
        Canvas(Modifier.size(STATUS_ICON_DP.dp)) { drawMark(mark, ink, bar) }
        Spacer(Modifier.width(6.dp))
        Canvas(Modifier.size(STATUS_ICON_DP.dp)) { drawNeighbourGlyph(ink) }
        Spacer(Modifier.width(6.dp))
        Canvas(Modifier.size(STATUS_ICON_DP.dp)) { drawBatteryGlyph(ink) }
    }
}

@Composable
private fun AnimatedMark(
    mark: MarkCandidate,
    motion: MotionCandidate,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    when (motion) {
        MotionCandidate.SWEEP -> SweepMark(mark, color, background, modifier)
        MotionCandidate.REVEAL -> RevealMark(mark, color, background, modifier)
        MotionCandidate.PEEK -> PeekMark(mark, color, background, modifier)
        MotionCandidate.PING -> PingMark(mark, color, background, modifier)
        MotionCandidate.HOLD -> Canvas(modifier) { drawMark(mark, color, background) }
    }
}

@Composable
private fun SweepMark(mark: MarkCandidate, color: Color, background: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sweep")
    val t by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "sweep-x",
    )
    Canvas(modifier) {
        val travel = size.width * 0.22f
        val dotY = size.height * 0.94f
        val dotXs = listOf(-1f, 0f, 1f)
        dotXs.forEach { dx ->
            val lit = (1f - abs(dx - t) * 1.6f).coerceIn(0f, 1f)
            drawCircle(
                color.copy(alpha = 0.2f + 0.8f * lit),
                radius = size.width * (0.025f + 0.015f * lit),
                center = Offset(size.width / 2 + dx * travel, dotY),
            )
        }
        translate(left = t * travel) {
            scale(0.78f, pivot = Offset(size.width / 2, size.height * 0.42f)) { drawMark(mark, color, background) }
        }
    }
}

@Composable
private fun RevealMark(mark: MarkCandidate, color: Color, background: Color, modifier: Modifier = Modifier) {
    val sweep = remember { Animatable(0f) }
    val settle = remember { Animatable(1.1f) }
    LaunchedEffect(mark) {
        sweep.snapTo(0f)
        settle.snapTo(1.1f)
        sweep.animateTo(360f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
        settle.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val pie = Path().apply {
            moveTo(size.width / 2, size.height / 2)
            arcTo(Rect(Offset.Zero, size).inflate(size.width), -90f, sweep.value, false)
            close()
        }
        clipPath(pie) {
            scale(settle.value) { drawMark(mark, color, background) }
        }
    }
}

@Composable
private fun PeekMark(mark: MarkCandidate, color: Color, background: Color, modifier: Modifier = Modifier) {
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(mark) {
        tilt.snapTo(0f)
        while (isActive) {
            delay(700)
            tilt.animateTo(16f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
            delay(320)
            tilt.animateTo(-7f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            tilt.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            delay(900)
        }
    }
    Canvas(modifier) {
        rotate(tilt.value, pivot = Offset(size.width * 0.88f, size.height * 0.88f)) {
            drawMark(mark, color, background)
        }
    }
}

@Composable
private fun PingMark(mark: MarkCandidate, color: Color, background: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ping")
    val spec = infiniteRepeatable<Float>(tween(durationMillis = 1600, easing = LinearEasing), RepeatMode.Restart)
    val first by transition.animateFloat(0f, 1f, spec, label = "ring-1")
    val second by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1600, easing = LinearEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset(550),
        ),
        label = "ring-2",
    )
    Canvas(modifier) {
        val centre = lensCentre(mark, size)
        val lensR = lensRadius(mark, size)
        listOf(first, second).forEach { phase ->
            drawCircle(
                color.copy(alpha = 0.45f * (1f - phase)),
                radius = lensR * (1f + 1.3f * phase),
                center = centre,
                style = Stroke(width = size.width * 0.02f),
            )
        }
        drawMark(mark, color, background)
    }
}

private fun lensCentre(mark: MarkCandidate, size: Size): Offset = when (mark) {
    MarkCandidate.REPO_UNDER_GLASS -> Offset(size.width * 0.60f, size.height * 0.60f)
    MarkCandidate.TILE_CUTOUT -> Offset(size.width * 0.44f, size.height * 0.44f)
    else -> Offset(size.width * 0.42f, size.height * 0.42f)
}

private fun lensRadius(mark: MarkCandidate, size: Size): Float = when (mark) {
    MarkCandidate.REPO_UNDER_GLASS -> size.width * 0.22f
    MarkCandidate.TILE_CUTOUT -> size.width * 0.20f
    else -> size.width * 0.28f
}

/** Draws one mark filling the DrawScope square in a single ink colour. */
internal fun DrawScope.drawMark(mark: MarkCandidate, ink: Color, background: Color) {
    when (mark) {
        MarkCandidate.COMMIT_LENS -> drawCommitLens(ink)
        MarkCandidate.GLANCE_EYE -> drawGlanceEye(ink)
        MarkCandidate.REPO_UNDER_GLASS -> drawRepoUnderGlass(ink)
        MarkCandidate.DIAL_LENS -> drawDialLens(ink)
        MarkCandidate.TILE_CUTOUT -> drawTileCutout(ink, background)
        MarkCandidate.COMMIT_EYE -> drawCommitEye(ink, background)
    }
}

private fun DrawScope.strokeWidth(): Float = size.minDimension * 0.09f

private fun DrawScope.drawMagnifier(ink: Color, centre: Offset, radius: Float, handleTo: Offset) {
    val w = strokeWidth()
    drawCircle(ink, radius = radius, center = centre, style = Stroke(width = w))
    val dir = handleTo - centre
    val len = dir.getDistance()
    val start = centre + dir * ((radius + w / 2) / len)
    drawLine(ink, start, handleTo, strokeWidth = w * 1.25f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCommitLens(ink: Color) {
    val s = size.minDimension
    val centre = Offset(s * 0.42f, s * 0.42f)
    val r = s * 0.28f
    drawMagnifier(ink, centre, r, Offset(s * 0.88f, s * 0.88f))
    val w = strokeWidth() * 0.7f
    drawLine(ink, Offset(centre.x, centre.y - r * 0.62f), Offset(centre.x, centre.y - r * 0.28f), w, StrokeCap.Round)
    drawLine(ink, Offset(centre.x, centre.y + r * 0.28f), Offset(centre.x, centre.y + r * 0.62f), w, StrokeCap.Round)
    drawCircle(ink, radius = r * 0.2f, center = centre)
}

private fun DrawScope.drawGlanceEye(ink: Color) {
    val s = size.minDimension
    val centre = Offset(s * 0.42f, s * 0.42f)
    val r = s * 0.28f
    drawMagnifier(ink, centre, r, Offset(s * 0.88f, s * 0.88f))
    val w = strokeWidth() * 0.7f
    val lid = Rect(Offset(centre.x - r * 0.62f, centre.y - r * 0.5f), Size(r * 1.24f, r * 1.1f))
    drawArc(
        ink,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = lid.topLeft,
        size = lid.size,
        style = Stroke(width = w, cap = StrokeCap.Round),
    )
    drawCircle(ink, radius = r * 0.2f, center = Offset(centre.x, centre.y + r * 0.08f))
}

private fun DrawScope.drawCommitEye(ink: Color, background: Color) {
    val s = size.minDimension
    val centre = Offset(s * 0.42f, s * 0.42f)
    val r = s * 0.28f
    drawMagnifier(ink, centre, r, Offset(s * 0.88f, s * 0.88f))
    val w = strokeWidth() * 0.62f
    drawLine(ink, Offset(centre.x, centre.y - r * 0.72f), Offset(centre.x, centre.y + r * 0.72f), w, StrokeCap.Round)
    val eye = Path().apply {
        moveTo(centre.x - r * 0.66f, centre.y)
        quadraticTo(centre.x, centre.y - r * 0.78f, centre.x + r * 0.66f, centre.y)
        quadraticTo(centre.x, centre.y + r * 0.78f, centre.x - r * 0.66f, centre.y)
        close()
    }
    drawPath(eye, background)
    drawPath(eye, ink, style = Stroke(width = w))
    drawCircle(ink, radius = r * 0.24f, center = centre, style = Stroke(width = w))
}

private fun DrawScope.drawRepoUnderGlass(ink: Color) {
    val s = size.minDimension
    val w = strokeWidth() * 0.85f
    val card = Rect(Offset(s * 0.10f, s * 0.10f), Size(s * 0.54f, s * 0.58f))
    drawRoundRect(
        ink,
        topLeft = card.topLeft,
        size = card.size,
        cornerRadius = CornerRadius(s * 0.06f),
        style = Stroke(width = w),
    )
    drawLine(ink, Offset(card.left, card.top + s * 0.15f), Offset(card.right, card.top + s * 0.15f), w, StrokeCap.Butt)
    drawMagnifier(ink, Offset(s * 0.60f, s * 0.60f), s * 0.22f, Offset(s * 0.92f, s * 0.92f))
}

private fun DrawScope.drawDialLens(ink: Color) {
    val s = size.minDimension
    val centre = Offset(s * 0.42f, s * 0.42f)
    val r = s * 0.28f
    val w = strokeWidth()
    val ticks = 12
    repeat(ticks) { i ->
        val a = Math.toRadians(i * 360.0 / ticks).toFloat()
        val tip = Offset(centre.x + cos(a) * r, centre.y + sin(a) * r)
        drawCircle(ink, radius = w * 0.55f, center = tip)
    }
    drawCircle(ink, radius = w * 0.6f, center = centre)
    val start = Offset(centre.x + r * 0.74f, centre.y + r * 0.74f)
    drawLine(ink, start, Offset(s * 0.88f, s * 0.88f), strokeWidth = w * 1.25f, cap = StrokeCap.Round)
}

private fun DrawScope.drawTileCutout(ink: Color, background: Color) {
    val s = size.minDimension
    drawRoundRect(ink, size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f))
    val centre = Offset(s * 0.44f, s * 0.44f)
    val r = s * 0.20f
    val w = strokeWidth() * 0.9f
    drawCircle(background, radius = r, center = centre, style = Stroke(width = w))
    drawLine(background, Offset(s * 0.62f, s * 0.62f), Offset(s * 0.80f, s * 0.80f), w * 1.2f, StrokeCap.Round)
}

private fun DrawScope.drawNeighbourGlyph(ink: Color) {
    val s = size.minDimension
    val w = s * 0.09f
    drawLine(ink, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.8f, s * 0.5f), w, StrokeCap.Round)
    drawLine(ink, Offset(s * 0.5f, s * 0.2f), Offset(s * 0.5f, s * 0.8f), w, StrokeCap.Round)
}

private fun DrawScope.drawBatteryGlyph(ink: Color) {
    val s = size.minDimension
    drawRoundRect(
        ink,
        topLeft = Offset(s * 0.34f, s * 0.12f),
        size = Size(s * 0.32f, s * 0.76f),
        cornerRadius = CornerRadius(s * 0.06f),
    )
}
