package co.saari.repoglance.hooks

import org.junit.Assert.assertEquals
import org.junit.Test

class FixtureRouteDebugTest {
    @Test
    fun debugClassesCarryTheFixtureHomeNavigatorAndRoute() {
        assertEquals(
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES,
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES.filter(FixtureRouteGuardTest::loads),
        )
    }
}
