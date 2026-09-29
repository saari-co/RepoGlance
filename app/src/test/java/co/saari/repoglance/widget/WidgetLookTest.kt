package co.saari.repoglance.widget

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.CiColorRole
import co.saari.repoglance.render.ClockLabel
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * widget-look-032 (hybrid H): the compact widget takes the tonal capsule and
 * mono labels, the tall and stack widgets take family ink, bold, on the
 * system face. Freshness roles follow the app's family status meanings.
 */
class WidgetLookTest {

    private val now: Instant = Instant.parse("2026-09-18T03:00:00Z")
    private val fresh = WidgetFreshness(now, ClockLabel(ZoneOffset.UTC, true, Locale.US), rateLimitedUntil = null)

    private fun snapshot(basis: ValueBasis) = RepoSnapshot(
        repo = RepoRef("saari-co", "RepoGlance"),
        openPrs = if (basis == ValueBasis.UNKNOWN) null else 1,
        prsAwaitingMyReview = if (basis == ValueBasis.UNKNOWN) null else 0,
        openIssues = if (basis == ValueBasis.UNKNOWN) null else 2,
        defaultBranchCi = CiState.UNKNOWN,
        latestRelease = null,
        pushedAt = null,
        valueBasis = basis,
        observedAt = if (basis == ValueBasis.UNKNOWN) null else now.minusSeconds(600),
        rateLimit = RateLimitBucket.UNKNOWN,
    )

    @Test
    fun productionDefaultIsTheLockedHybrid() {
        val look = WidgetLook.Default
        assertEquals(WidgetLook.Family, look)
        assertEquals(FreshnessStyle.TONE_CAPSULE, look.freshness)
        assertEquals(true, look.mono)
        assertEquals(WidgetLook(mono = false, freshness = FreshnessStyle.TONE_INK, staleBold = true), look.tall)
        assertEquals(CompactLayout.MERGED_COUNTS, look.compact)
    }

    @Test
    fun capsuleTakesItsOwnRowOnlyWhenStale() {
        assertEquals(CapsulePlace.INLINE, capsulePlace(CompactLayout.MERGED_COUNTS, stale = false, narrow = true))
        assertEquals(CapsulePlace.OWN_ROW, capsulePlace(CompactLayout.MERGED_COUNTS, stale = true, narrow = false))
        assertEquals(CapsulePlace.INLINE, capsulePlace(CompactLayout.INLINE, stale = true, narrow = true))
    }

    @Test
    fun freshnessRolesFollowTheFamilyMeanings() {
        assertNull(freshnessRole(snapshot(ValueBasis.EXACT), fresh))
        assertEquals(CiColorRole.IN_PROGRESS, freshnessRole(snapshot(ValueBasis.LAST_GOOD), fresh))
        assertEquals(CiColorRole.NEUTRAL, freshnessRole(null, fresh))
        assertEquals(CiColorRole.NEUTRAL, freshnessRole(snapshot(ValueBasis.UNKNOWN), fresh))
        val limited = fresh.copy(rateLimitedUntil = now.plusSeconds(60))
        assertEquals(CiColorRole.NEGATIVE, freshnessRole(snapshot(ValueBasis.EXACT), limited))
    }
}
