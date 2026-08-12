package co.saari.repoglance.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class RepoSnapshotInvariantsTest {

    private val repo = RepoRef("saari-co", "RepoGlance")
    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun snapshot(
        valueBasis: ValueBasis,
        openPrs: Int?,
        prsAwaitingMyReview: Int?,
        openIssues: Int?,
        observedAt: Instant?,
        defaultBranchCi: CiState = CiState.UNKNOWN,
        latestRelease: ReleaseInfo? = null,
        pushedAt: Instant? = null,
        rateLimit: RateLimitBucket = RateLimitBucket.UNKNOWN,
    ) = RepoSnapshot(
        repo = repo,
        openPrs = openPrs,
        prsAwaitingMyReview = prsAwaitingMyReview,
        openIssues = openIssues,
        defaultBranchCi = defaultBranchCi,
        latestRelease = latestRelease,
        pushedAt = pushedAt,
        valueBasis = valueBasis,
        observedAt = observedAt,
        rateLimit = rateLimit,
    )

    @Test
    fun unknownBasisAllowsNullCounts() {
        val result = snapshot(ValueBasis.UNKNOWN, null, null, null, null)
        assertEquals(null, result.openPrs)
        assertEquals(null, result.prsAwaitingMyReview)
        assertEquals(null, result.openIssues)
    }

    @Test
    fun unknownBasisRejectsNonNullCount() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.UNKNOWN, 1, null, null, null)
        }
    }

    @Test
    fun unknownBasisRejectsKnownObservationMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.UNKNOWN, null, null, null, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.UNKNOWN, null, null, null, null, pushedAt = now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.UNKNOWN, null, null, null, null, defaultBranchCi = CiState.PASSING)
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(
                ValueBasis.UNKNOWN,
                null,
                null,
                null,
                null,
                latestRelease = ReleaseInfo("v1.0.0", now),
            )
        }
    }

    @Test
    fun unknownBasisAllowsKnownRateLimitState() {
        val result = snapshot(
            ValueBasis.UNKNOWN,
            null,
            null,
            null,
            null,
            rateLimit = RateLimitBucket.EXHAUSTED,
        )
        assertEquals(RateLimitBucket.EXHAUSTED, result.rateLimit)
    }

    @Test
    fun exactBasisRequiresAllCountsNonNull() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.EXACT, 1, null, 2, now)
        }
    }

    @Test
    fun exactBasisRequiresObservedAt() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.EXACT, 1, 0, 2, null)
        }
    }

    @Test
    fun lastGoodBasisRequiresAllCountsNonNull() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.LAST_GOOD, null, 0, 2, now)
        }
    }

    @Test
    fun lastGoodBasisRequiresObservedAt() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.LAST_GOOD, 1, 0, 2, null)
        }
    }

    @Test
    fun exactBasisAcceptsValidSnapshot() {
        val result = snapshot(ValueBasis.EXACT, 1, 0, 2, now)
        assertEquals(1, result.openPrs)
    }

    @Test
    fun negativeCountsThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.EXACT, -1, 0, 2, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.EXACT, 1, -1, 2, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot(ValueBasis.EXACT, 1, 0, -2, now)
        }
    }
}
