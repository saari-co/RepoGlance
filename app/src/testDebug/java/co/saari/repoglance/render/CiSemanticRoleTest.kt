package co.saari.repoglance.render

import co.saari.repoglance.model.CiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CiSemanticRoleTest {

    @Test
    fun everyCiStateMapsToARole() {
        // Totality: the mapping is a `when` over all CiState entries with no
        // else branch, so this only compiles/passes if every entry is
        // covered — this test also pins the resulting set size.
        val roles = CiState.entries.map { CiSemanticRole.of(it) }
        assertEquals(CiState.entries.size, roles.size)
    }

    @Test
    fun passingFailingRunningMapToDistinctRoles() {
        val passing = CiSemanticRole.of(CiState.PASSING)
        val failing = CiSemanticRole.of(CiState.FAILING)
        val running = CiSemanticRole.of(CiState.RUNNING)
        assertNotEquals(passing, failing)
        assertNotEquals(passing, running)
        assertNotEquals(failing, running)
    }

    @Test
    fun noCiAndUnknownShareNeutralRole() {
        assertEquals(CiColorRole.NEUTRAL, CiSemanticRole.of(CiState.NO_CI))
        assertEquals(CiColorRole.NEUTRAL, CiSemanticRole.of(CiState.UNKNOWN))
    }

    @Test
    fun neutralIsDistinctFromThePositiveNegativeInProgressRoles() {
        val neutral = CiSemanticRole.of(CiState.NO_CI)
        assertNotEquals(neutral, CiSemanticRole.of(CiState.PASSING))
        assertNotEquals(neutral, CiSemanticRole.of(CiState.FAILING))
        assertNotEquals(neutral, CiSemanticRole.of(CiState.RUNNING))
    }

    @Test
    fun exactlyFourDistinctRolesExistAcrossAllCiStates() {
        val roles = CiState.entries.map { CiSemanticRole.of(it) }.toSet()
        assertEquals(4, roles.size)
    }
}
