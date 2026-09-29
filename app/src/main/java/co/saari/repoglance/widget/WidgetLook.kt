package co.saari.repoglance.widget

import android.content.Context
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.CiColorRole
import co.saari.repoglance.ui.theme.FamilyStatus
import co.saari.repoglance.ui.theme.StatusColors
import co.saari.repoglance.ui.theme.StatusTone
import androidx.glance.color.ColorProvider as dayNight

enum class FreshnessStyle { MATERIAL_ERROR, TONE_INK, TONE_CAPSULE, TONE_HEADER }

@Immutable
data class WidgetLook(
    val mono: Boolean,
    val freshness: FreshnessStyle,
    val staleBold: Boolean,
    val tall: WidgetLook? = null,
) {
    companion object {
        val Material = WidgetLook(mono = false, freshness = FreshnessStyle.MATERIAL_ERROR, staleBold = true)

        val Family = WidgetLook(
            mono = true,
            freshness = FreshnessStyle.TONE_CAPSULE,
            staleBold = false,
            tall = WidgetLook(mono = false, freshness = FreshnessStyle.TONE_INK, staleBold = true),
        )

        val Default = Family
    }
}

@Immutable
data class WidgetTone(val ink: ColorProvider, val container: ColorProvider, val onContainer: ColorProvider)

@Immutable
data class WidgetTones(val working: WidgetTone, val failing: WidgetTone, val neutral: WidgetTone) {
    fun of(role: CiColorRole): WidgetTone = when (role) {
        CiColorRole.IN_PROGRESS -> working
        CiColorRole.NEGATIVE -> failing
        else -> neutral
    }

    companion object {
        fun of(context: Context): WidgetTones {
            val day = FamilyStatus.colors(dynamicLightColorScheme(context))
            val night = FamilyStatus.colors(dynamicDarkColorScheme(context))
            fun pair(pick: (StatusColors) -> StatusTone) =
                WidgetTone(
                    ink = dayNight(pick(day).ink, pick(night).ink),
                    container = dayNight(pick(day).container, pick(night).container),
                    onContainer = dayNight(pick(day).onContainer, pick(night).onContainer),
                )
            return WidgetTones(
                working = pair { it.working },
                failing = pair { it.failing },
                neutral = pair { it.neutral },
            )
        }
    }
}

val LocalWidgetLook = staticCompositionLocalOf { WidgetLook.Default }

val LocalWidgetTones = staticCompositionLocalOf<WidgetTones?> { null }

@Composable
fun TallLook(content: @Composable () -> Unit) {
    val look = LocalWidgetLook.current
    CompositionLocalProvider(LocalWidgetLook provides (look.tall ?: look), content = content)
}

internal fun freshnessRole(snapshot: RepoSnapshot?, freshness: WidgetFreshness): CiColorRole? = when {
    freshness.rateLimitedUntil != null -> CiColorRole.NEGATIVE
    snapshot == null || snapshot.observedAt == null || snapshot.valueBasis == ValueBasis.UNKNOWN ->
        CiColorRole.NEUTRAL
    snapshot.valueBasis == ValueBasis.LAST_GOOD -> CiColorRole.IN_PROGRESS
    else -> null
}

@Composable
internal fun labelFamily(): FontFamily? = if (LocalWidgetLook.current.mono) FontFamily.Monospace else null

@Composable
internal fun FreshnessText(
    text: String,
    role: CiColorRole?,
    tag: String,
    modifier: GlanceModifier = GlanceModifier,
    fontSize: TextUnit? = null,
) {
    val look = LocalWidgetLook.current
    val tone = role?.let { LocalWidgetTones.current?.of(it) }
    val stale = role != null
    val color = when {
        !stale -> GlanceTheme.colors.onSurfaceVariant
        look.freshness == FreshnessStyle.MATERIAL_ERROR || tone == null -> GlanceTheme.colors.error
        look.freshness == FreshnessStyle.TONE_CAPSULE -> tone.onContainer
        else -> tone.ink
    }
    val capsule = tone?.takeIf { look.freshness == FreshnessStyle.TONE_CAPSULE }
    Text(
        text,
        maxLines = 1,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = if (stale && look.staleBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = labelFamily(),
        ),
        modifier = if (capsule != null) {
            modifier
                .background(capsule.container)
                .cornerRadius(CAPSULE_RADIUS.dp)
                .padding(horizontal = CAPSULE_PAD.dp)
                .semantics { testTag = tag }
        } else {
            modifier.semantics { testTag = tag }
        },
    )
}

@Composable
internal fun headerBackground(role: CiColorRole?): ColorProvider {
    val tone = role?.let { LocalWidgetTones.current?.of(it) }
    return if (tone != null && LocalWidgetLook.current.freshness == FreshnessStyle.TONE_HEADER) {
        tone.container
    } else {
        GlanceTheme.colors.surfaceVariant
    }
}

@Composable
internal fun headerInk(role: CiColorRole?): ColorProvider {
    val tone = role?.let { LocalWidgetTones.current?.of(it) }
    return if (tone != null && LocalWidgetLook.current.freshness == FreshnessStyle.TONE_HEADER) {
        tone.onContainer
    } else {
        GlanceTheme.colors.onSurfaceVariant
    }
}

private const val CAPSULE_RADIUS = 8
private const val CAPSULE_PAD = 4
