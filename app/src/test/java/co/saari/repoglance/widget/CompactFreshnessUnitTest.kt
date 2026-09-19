package co.saari.repoglance.widget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.render.ClockLabel
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The compact slot must never show a stored count as if it were current.
 * Truth rules 2-3: data age always visible; last-good never reads as fresh.
 * The age is a clock time because a widget picture is frozen between redraws
 * (GrillTrack widget-background-refresh-023).
 */
class CompactFreshnessUnitTest {

    private val now: Instant = Instant.parse("2026-09-18T03:00:00Z")
    private val clock = ClockLabel(ZoneOffset.UTC, is24Hour = true, locale = Locale.US)
    private val fresh = WidgetFreshness(now, clock, rateLimitedUntil = null)
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
    fun exactSnapshotRendersTheClockTimeItWasObserved() {
        assertEquals("02:48", compactFreshnessLabel(snapshot(ValueBasis.EXACT, now.minusSeconds(12 * 60)), fresh))
    }

    @Test
    fun clockTimeStaysTrueHoweverLongThePictureSits() {
        val observed = snapshot(ValueBasis.EXACT, now.minusSeconds(12 * 60))
        val later = fresh.copy(now = now.plusSeconds(55 * 60))
        assertEquals(compactFreshnessLabel(observed, fresh), compactFreshnessLabel(observed, later))
    }

    @Test
    fun lastGoodSnapshotIsPrefixedSoItCannotReadAsCurrent() {
        val threeDays = now.minusSeconds(3 * 24 * 3600)
        assertEquals("last good Tue 03:00", compactFreshnessLabel(snapshot(ValueBasis.LAST_GOOD, threeDays), fresh))
    }

    @Test
    fun rateLimitedSnapshotSaysSoBesideItsAge() {
        val limited = fresh.copy(rateLimitedUntil = now.plusSeconds(1800))
        assertEquals("rate limited · 02:48", compactFreshnessLabel(snapshot(ValueBasis.EXACT, now.minusSeconds(12 * 60)), limited))
    }

    @Test
    fun noObservationSaysSoInsteadOfShowingNothing() {
        assertEquals("no data", compactFreshnessLabel(null, fresh))
        assertEquals("no data", compactFreshnessLabel(snapshot(ValueBasis.UNKNOWN, null), fresh))
    }

    @Test
    fun tallHeaderCarriesBackOffResetTimeAndClockAge() {
        val limited = fresh.copy(rateLimitedUntil = Instant.parse("2026-09-18T03:35:00Z"))
        assertEquals(
            "rate limited · resets 03:35 · last good · ISSUES 7 · as of 02:00",
            tallHeaderLabel(snapshot(ValueBasis.LAST_GOOD, now.minusSeconds(3600)), NavigatorMode.ISSUES, limited),
        )
        assertEquals(
            "ISSUES 7 · PRS 3 · as of 02:00",
            tallHeaderLabel(snapshot(ValueBasis.EXACT, now.minusSeconds(3600)), NavigatorMode.BOTH, fresh),
        )
        assertEquals("no data · open RepoGlance to load", tallHeaderLabel(null, NavigatorMode.BOTH, limited))
    }

    @Test
    fun freshnessIsEmittedAsAnAddressableElementForEveryBasis() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(snapshot(ValueBasis.LAST_GOOD, now.minusSeconds(3 * 24 * 3600)), fresh) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("last good Tue 03:00")
    }

    @Test
    fun freshnessElementRendersExactClockTime() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(snapshot(ValueBasis.EXACT, now.minusSeconds(2 * 3600)), fresh) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("01:00")
    }

    @Test
    fun freshnessElementRendersNoDataForMissingSnapshot() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(null, fresh) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("no data")
    }
}
