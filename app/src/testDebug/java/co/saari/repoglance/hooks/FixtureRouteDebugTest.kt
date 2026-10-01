package co.saari.repoglance.hooks

import org.junit.Assert.assertEquals
import org.junit.Test

class FixtureRouteDebugTest {
    @Test
    fun debugClassesCarryTheFixtureHomeNavigatorRouteAndHelpers() {
        assertEquals(
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES,
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES.filter(FixtureRouteGuardTest::loads),
        )
    }

    @Test
    fun debugClassesDeclareTheFixtureOnlyMembers() {
        for (moved in FixtureRouteGuardTest.DEBUG_ONLY_MEMBERS) {
            assertEquals(
                moved.debugHolder,
                moved.names,
                FixtureRouteGuardTest.declaredMethodNames(moved.debugHolder).filterTo(mutableSetOf()) { it in moved.names },
            )
        }
    }
}
