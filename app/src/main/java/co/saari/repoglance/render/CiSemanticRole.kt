package co.saari.repoglance.render

import co.saari.repoglance.model.CiState

enum class CiColorRole { POSITIVE, NEGATIVE, IN_PROGRESS, NEUTRAL }

object CiSemanticRole {
    fun of(ci: CiState): CiColorRole = when (ci) {
        CiState.PASSING -> CiColorRole.POSITIVE
        CiState.FAILING -> CiColorRole.NEGATIVE
        CiState.RUNNING -> CiColorRole.IN_PROGRESS
        CiState.NO_CI, CiState.UNKNOWN -> CiColorRole.NEUTRAL
    }
}
