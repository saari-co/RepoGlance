package co.saari.repoglance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import co.saari.repoglance.render.CiColorRole
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class StatusTone(
    val ink: Color,
    val container: Color,
    val onContainer: Color,
)

@Immutable
data class StatusColors(
    val ok: StatusTone,
    val working: StatusTone,
    val failing: StatusTone,
    val neutral: StatusTone,
) {
    fun tone(role: CiColorRole): StatusTone = when (role) {
        CiColorRole.POSITIVE -> ok
        CiColorRole.IN_PROGRESS -> working
        CiColorRole.NEGATIVE -> failing
        CiColorRole.NEUTRAL -> neutral
    }
}

object FamilyStatus {
    val EMERALD = Color(0xFF22C55E)
    val AMBER = Color(0xFFF59E0B)
    val RED = Color(0xFFEF4444)

    private const val MAX_ROTATION = 15.0
    private const val HALF = 0.5
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0
    private const val DARK_LUMINANCE = 0.5f
    private const val ACHROMATIC_CHROMA = 5.0
    private const val INK_DARK_MAX_L = 78.0
    private const val CONTAINER_L_LIGHT = 92.0
    private const val CONTAINER_C_LIGHT = 24.0
    private const val ON_CONTAINER_L_LIGHT = 30.0
    private const val ON_CONTAINER_C_LIGHT = 45.0
    private const val CONTAINER_L_DARK = 30.0
    private const val CONTAINER_C_DARK = 30.0
    private const val ON_CONTAINER_L_DARK = 90.0
    private const val ON_CONTAINER_C_DARK = 20.0

    fun colors(scheme: ColorScheme): StatusColors {
        val dark = scheme.background.luminance() < DARK_LUMINANCE
        return StatusColors(
            ok = tone(harmonise(EMERALD, scheme.primary), dark),
            working = tone(harmonise(AMBER, scheme.primary), dark),
            failing = tone(harmonise(RED, scheme.primary), dark),
            neutral = StatusTone(scheme.outline, scheme.surfaceVariant, scheme.onSurfaceVariant),
        )
    }

    fun harmonise(design: Color, source: Color): Color {
        val (dl, dc, dh) = lch(design)
        val (_, sc, sh) = lch(source)
        if (sc < ACHROMATIC_CHROMA) return design
        var diff = sh - dh
        if (diff > HALF_TURN) diff -= FULL_TURN
        if (diff < -HALF_TURN) diff += FULL_TURN
        val rotation = (abs(diff) * HALF).coerceAtMost(MAX_ROTATION)
        val hue = dh + if (diff >= 0) rotation else -rotation
        return fromLch(dl, dc, hue)
    }

    fun tone(base: Color, dark: Boolean): StatusTone {
        val (l, c, h) = lch(base)
        return if (dark) {
            StatusTone(
                ink = fromLch(l.coerceAtMost(INK_DARK_MAX_L), c, h),
                container = fromLch(CONTAINER_L_DARK, CONTAINER_C_DARK, h),
                onContainer = fromLch(ON_CONTAINER_L_DARK, ON_CONTAINER_C_DARK, h),
            )
        } else {
            StatusTone(
                ink = base,
                container = fromLch(CONTAINER_L_LIGHT, CONTAINER_C_LIGHT, h),
                onContainer = fromLch(ON_CONTAINER_L_LIGHT, ON_CONTAINER_C_LIGHT, h),
            )
        }
    }

    private fun lch(color: Color): DoubleArray {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(color.toArgb(), lab)
        val chroma = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
        val hue = (Math.toDegrees(atan2(lab[2], lab[1])) + FULL_TURN) % FULL_TURN
        return doubleArrayOf(lab[0], chroma, hue)
    }

    private fun fromLch(l: Double, c: Double, h: Double): Color {
        val rad = Math.toRadians((h + FULL_TURN) % FULL_TURN)
        return Color(ColorUtils.LABToColor(l, c * cos(rad), c * sin(rad)))
    }
}
