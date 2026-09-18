package co.saari.repoglance.widget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The compact slot must never show a stored count as if it were current.
 * Truth rules 2-3: data age always visible; last-good never reads as fresh.
 */
class CompactFreshnessUnitTest {

    private val now: Instant = Instant.parse("2026-09-18T03:00:00Z")
    private val repo = RepoRef("saari-co", "RepoGlance")

    private fun snapshot(basis: ValueBasis, observedAt: Instant?): RepoSnapshot = RepoSnapshot(
        repo = repo,
        openPrs = if (basis == ValueBasis.UNKNOWN) null else 3,
        prsAwaitingMyReview = if (basis == ValueBasis.UNKNOWN) null else 1,
        openIssues = if (basis == ValueBasis.UNKNOWN) null else 7,
        defaultBranchCi = CiState.UNKNOWN,
        latestRelease = null,
        pushedAt = null,
        valueBasis = basis,
        observedAt = observedAt,
        rateLimit = RateLimitBucket.UNKNOWN,
    )

    @Test
    fun exactSnapshotRendersItsBareAge() {
        assertEquals("12m", compactFreshnessLabel(snapshot(ValueBasis.EXACT, now.minusSeconds(12 * 60)), now))
    }

    @Test
    fun lastGoodSnapshotIsPrefixedSoItCannotReadAsCurrent() {
        val threeDays = now.minusSeconds(3 * 24 * 3600)
        assertEquals("last good 3d", compactFreshnessLabel(snapshot(ValueBasis.LAST_GOOD, threeDays), now))
    }

    @Test
    fun noObservationSaysSoInsteadOfShowingNothing() {
        assertEquals("no data", compactFreshnessLabel(null, now))
        assertEquals("no data", compactFreshnessLabel(snapshot(ValueBasis.UNKNOWN, null), now))
    }

    @Test
    fun freshnessIsEmittedAsAnAddressableElementForEveryBasis() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(snapshot(ValueBasis.LAST_GOOD, now.minusSeconds(3 * 24 * 3600)), now) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("last good 3d")
    }

    @Test
    fun freshnessElementRendersExactAge() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(snapshot(ValueBasis.EXACT, now.minusSeconds(2 * 3600)), now) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("2h")
    }

    @Test
    fun freshnessElementRendersNoDataForMissingSnapshot() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(null, now) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("no data")
    }
}
