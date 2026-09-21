package co.saari.repoglance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import co.saari.repoglance.render.CiColorRole

@Immutable
data class StatusTone(
    val ink: Color,
    val container: Color,
    val onContainer: Color,
)

enum class StatusShape { DOT, PILL }

@Immutable
data class StatusColors(
    val ok: StatusTone,
    val working: StatusTone,
    val failing: StatusTone,
    val neutral: StatusTone,
    val shape: StatusShape = StatusShape.DOT,
) {
    fun tone(role: CiColorRole): StatusTone = when (role) {
        CiColorRole.POSITIVE -> ok
        CiColorRole.IN_PROGRESS -> working
        CiColorRole.NEGATIVE -> failing
        CiColorRole.NEUTRAL -> neutral
    }
}
