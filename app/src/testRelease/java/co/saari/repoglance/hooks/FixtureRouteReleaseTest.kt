package co.saari.repoglance.hooks

import org.junit.Assert.assertEquals
import org.junit.Test

class FixtureRouteReleaseTest {
    @Test
    fun releaseClassesCarryNoFixtureHomeNavigatorRouteOrHelpers() {
        assertEquals(
            emptyList<String>(),
            FixtureRouteGuardTest.DEBUG_ONLY_CLASSES.filter(FixtureRouteGuardTest::loads),
        )
    }

    @Test
    fun releaseClassesDeclareNoFixtureOnlyMember() {
        val regained = FixtureRouteGuardTest.DEBUG_ONLY_MEMBERS.flatMap { moved ->
            FixtureRouteGuardTest.declaredMethodNames(moved.shippedHost)
                .filter { it in moved.names }
                .map { "${moved.shippedHost}.$it" }
        }
        assertEquals(emptyList<String>(), regained.sorted())
    }
}
