package co.saari.repoglance.render

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.fixtures.Fixtures
import co.saari.repoglance.model.RateLimitBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RateLimitTest {

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun exhaustedBannerIsNonNull() {
        assertNotNull(SnapshotRendering.rateLimitBanner(RateLimitBucket.EXHAUSTED))
    }

    @Test
    fun okBannerIsNull() {
        assertNull(SnapshotRendering.rateLimitBanner(RateLimitBucket.OK))
    }

    @Test
    fun rateLimitedScenarioIsAllExhaustedWithBannerAndAgeChip() {
        val snapshots = Fixtures.snapshots(FixtureScenario.RATE_LIMITED, now)
        assertTrue(snapshots.isNotEmpty())
        for (snapshot in snapshots) {
            assertEquals(RateLimitBucket.EXHAUSTED, snapshot.rateLimit)
            assertNotNull(SnapshotRendering.rateLimitBanner(snapshot.rateLimit))
            assertNotNull(SnapshotRendering.ageChip(snapshot, now))
        }
    }
}
