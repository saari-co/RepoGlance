package co.saari.repoglance.render

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ValueBasisRenderingTest {

    private val repo = RepoRef("acme", "rocket")
    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun countTextUnknownBasisIsEmDash() {
        assertEquals("—", SnapshotRendering.countText(null, ValueBasis.UNKNOWN))
        assertEquals("—", SnapshotRendering.countText(0, ValueBasis.UNKNOWN))
        assertEquals("—", SnapshotRendering.countText(100, ValueBasis.UNKNOWN))
    }

    @Test
    fun countTextExactBasisRendersNumberIncludingLegitimateZero() {
        assertEquals("5", SnapshotRendering.countText(5, ValueBasis.EXACT))
        assertEquals("0", SnapshotRendering.countText(0, ValueBasis.EXACT))
    }

    @Test
    fun countTextLastGoodBasisRendersNumber() {
        assertEquals("7", SnapshotRendering.countText(7, ValueBasis.LAST_GOOD))
    }

    @Test
    fun countTextNullCountAlwaysEmDashRegardlessOfBasis() {
        assertEquals("—", SnapshotRendering.countText(null, ValueBasis.EXACT))
        assertEquals("—", SnapshotRendering.countText(null, ValueBasis.LAST_GOOD))
    }

    @Test
    fun ageChipNonNullOnlyForLastGoodBasis() {
        val lastGood = RepoSnapshot(
            repo = repo,
            openPrs = 1,
            prsAwaitingMyReview = 0,
            openIssues = 2,
            defaultBranchCi = CiState.PASSING,
            latestRelease = null,
            pushedAt = null,
            valueBasis = ValueBasis.LAST_GOOD,
            observedAt = now.minusSeconds(3600),
            rateLimit = RateLimitBucket.OK,
        )
        val exact = lastGood.copy(valueBasis = ValueBasis.EXACT, observedAt = now)

        assertNotNull(SnapshotRendering.ageChip(lastGood, now))
        assertNull(SnapshotRendering.ageChip(exact, now))
    }

    @Test
    fun countTextNeverZeroForUnknownAcrossRange() {
        for (count in 0..100) {
            assertEquals("—", SnapshotRendering.countText(count, ValueBasis.UNKNOWN))
        }
    }

    @Test
    fun pushedLabelDoesNotAppendAgoToJustNow() {
        assertEquals("Pushed just now", SnapshotRendering.pushedLabel(now.minusSeconds(5), now))
        assertEquals(
            "Pushed 2m ago",
            SnapshotRendering.pushedLabel(now.minus(Duration.ofMinutes(2)), now),
        )
    }

    @Test
    fun releaseLabelPreservesUnknownVersusKnownEmpty() {
        assertEquals(
            "Latest release: unknown",
            SnapshotRendering.releaseLabel(null, ValueBasis.UNKNOWN, now),
        )
        assertEquals(
            "No releases",
            SnapshotRendering.releaseLabel(null, ValueBasis.EXACT, now),
        )
    }
}
