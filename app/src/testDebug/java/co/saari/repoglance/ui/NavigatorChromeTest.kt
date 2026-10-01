package co.saari.repoglance.ui

import co.saari.repoglance.fixtures.ListState
import co.saari.repoglance.model.NavigatorFilter
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigatorChromeTest {
    @Test
    fun freshNavigatorSessionStartsInBothMode() {
        assertEquals(NavigatorMode.BOTH, DefaultNavigatorMode)
    }

    @Test
    fun defaultSecondaryControlsDoNotClaimAnActiveFilter() {
        assertEquals(0, activeSecondaryFilterCount(NavigatorFilter.OPEN, ListState.LOADED))
    }

    @Test
    fun filterLabelCountIncludesFilterAndFixtureStateOverrides() {
        assertEquals(1, activeSecondaryFilterCount(NavigatorFilter.MENTIONS, ListState.LOADED))
        assertEquals(1, activeSecondaryFilterCount(NavigatorFilter.OPEN, ListState.LAST_GOOD))
        assertEquals(2, activeSecondaryFilterCount(NavigatorFilter.MINE, ListState.PAGED))
    }

    @Test
    fun selectionKeyDistinguishesSameNumberAcrossRepositoriesAndKinds() {
        val rocket = RepoRef("acme", "rocket")
        val apiServer = RepoRef("acme", "api-server")
        assertNotEquals(rowKey("issue", rocket, 100), rowKey("issue", apiServer, 100))
        assertNotEquals(rowKey("issue", rocket, 100), rowKey("pr", rocket, 100))
        assertEquals(rowKey("issue", rocket, 100), rowKey("issue", RepoRef("acme", "rocket"), 100))
    }
}
