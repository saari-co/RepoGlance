package co.saari.repoglance.render

import co.saari.repoglance.model.CiState

/**
 * Semantic color role for a [CiState], decoupled from any concrete Color —
 * the Compose and Glance layers each map a role to their own theme color
 * (`MaterialTheme.colorScheme.*` / `GlanceTheme.colors.*`); no hardcoded hex
 * lives in either. [CiState.NO_CI] and [CiState.UNKNOWN] intentionally
 * share [CiColorRole.NEUTRAL] — neither is "passing" nor "failing", so
 * neither earns its own accent color.
 */
enum class CiColorRole { POSITIVE, NEGATIVE, IN_PROGRESS, NEUTRAL }

object CiSemanticRole {
    fun of(ci: CiState): CiColorRole = when (ci) {
        CiState.PASSING -> CiColorRole.POSITIVE
        CiState.FAILING -> CiColorRole.NEGATIVE
        CiState.RUNNING -> CiColorRole.IN_PROGRESS
        CiState.NO_CI, CiState.UNKNOWN -> CiColorRole.NEUTRAL
    }
}
