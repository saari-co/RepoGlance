package co.saari.repoglance.render

import co.saari.repoglance.model.CiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CiRenderingTest {

    @Test
    fun noCiLabelIsDistinctFromPassing() {
        assertEquals("No CI", SnapshotRendering.ciLabel(CiState.NO_CI))
        assertNotEquals(
            SnapshotRendering.ciLabel(CiState.NO_CI),
            SnapshotRendering.ciLabel(CiState.PASSING),
        )
    }

    @Test
    fun unknownLabelIsDistinctFromFailing() {
        assertNotEquals(
            SnapshotRendering.ciLabel(CiState.UNKNOWN),
            SnapshotRendering.ciLabel(CiState.FAILING),
        )
    }

    @Test
    fun allFiveCiLabelsAreUnique() {
        val labels = CiState.entries.map { SnapshotRendering.ciLabel(it) }
        assertEquals(labels.size, labels.toSet().size)
    }
}
