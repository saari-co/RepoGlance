package co.saari.repoglance.hooks

import org.junit.Assert.assertEquals
import org.junit.Test

class FixtureRouteReleaseTest {
    @Test
    fun releaseClassesCarryNoFixtureHomeNavigatorOrRoute() {
        assertEquals(
            emptyList<String>(),
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES.filter(FixtureRouteGuardTest::loads),
        )
    }
}
