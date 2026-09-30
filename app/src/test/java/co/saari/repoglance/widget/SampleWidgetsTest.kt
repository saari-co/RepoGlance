package co.saari.repoglance.widget

import android.content.Intent
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.ClockLabel
import co.saari.repoglance.sample.SampleAccount
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleWidgetsTest {
    private val now = Instant.parse("2026-09-30T14:05:00Z")
    private val clock = ClockLabel(ZoneOffset.UTC, is24Hour = true, locale = Locale.US)
    private val sample = WidgetFreshness(now, clock, rateLimitedUntil = null, sample = true)
    private val rocket = RepoRef("saari-co", "rocket")

    @Test
    fun setupListsEverySampleRepositoryMostRecentPushFirst() {
        assertEquals(
            listOf(
                "saari-co/rocket",
                "saari-co/api-server",
                "saari-co/mobile-app",
                "dinkuskit/infra",
                "dinkuskit/design-system",
                "saariuslystoned/dotfiles",
                "saari-co/legacy-site",
            ),
            SampleWidgetData.names(now),
        )
        assertEquals(
            SampleAccount.catalog(now).repositories.map { it.ref.full }.toSet(),
            SampleWidgetData.names(now).toSet(),
        )
    }

    @Test
    fun setupPutsSamplePinsFirstAndIgnoresNamesOutsideTheSample() {
        val list = SampleWidgetData.configurationList(setOf("dinkuskit/infra", "saari-co/RepoGlance"), now)
        assertEquals("dinkuskit/infra", list.first().full)
        assertEquals(7, list.size)
        assertFalse(list.any { it.full == "saari-co/RepoGlance" })
    }

    @Test
    fun sampleSnapshotCarriesTheSameCountsAsTheSampleRepositoryView() {
        val snapshot = SampleWidgetData.snapshot(rocket, now)!!
        val content = SampleAccount.content(SampleAccount.catalog(now).repositories.first { it.ref == rocket }, now)
        assertEquals(ValueBasis.EXACT, snapshot.valueBasis)
        assertEquals(SampleWidgetData.rows(rocket, now).count { it.kind == WidgetRowKind.ISSUE }, snapshot.openIssues)
        assertEquals(5, snapshot.openIssues)
        assertEquals(3, snapshot.openPrs)
        assertEquals(0, snapshot.prsAwaitingMyReview)
        assertEquals(rocket, content.repository.ref)
        assertNull(SampleWidgetData.snapshot(RepoRef("saari-co", "RepoGlance"), now))
    }

    @Test
    fun sampleRowsAreOneRecentlyUpdatedFeed() {
        val rows = SampleWidgetData.rows(rocket, now)
        assertEquals(8, rows.size)
        assertTrue(rows.zipWithNext().all { (first, second) -> first.updatedAt >= second.updatedAt })
        assertEquals(5, rowsForMode(rows, NavigatorMode.ISSUES).size)
        assertTrue(SampleWidgetData.rows(RepoRef("saari-co", "RepoGlance"), now).isEmpty())
    }

    @Test
    fun theTimeSlotSaysSampleInsteadOfAClockOnEverySurface() {
        val snapshot = SampleWidgetData.snapshot(rocket, now)
        assertEquals(SAMPLE_TIME_LABEL, compactFreshnessLabel(snapshot, sample))
        assertEquals(SAMPLE_TIME_LABEL, compactFreshnessLabel(snapshot, sample, short = true))
        assertEquals("ISSUES 5 · PRS 3 · sample", tallHeaderLabel(snapshot, NavigatorMode.BOTH, sample))
        assertEquals("sample", stackRowAge(snapshot, sample))
        assertEquals("issues 5 · PRs 3 · review 0", stackRowCounts(snapshot))
        for (label in listOf(compactFreshnessLabel(snapshot, sample), tallHeaderLabel(snapshot, NavigatorMode.BOTH, sample))) {
            assertFalse("no clock on a sample widget: $label", label.contains("14:05") || label.contains("as of"))
        }
        assertEquals("no data", compactFreshnessLabel(null, sample))
    }

    @Test
    fun liveWidgetsKeepTheirClock() {
        val snapshot = SampleWidgetData.snapshot(rocket, now)
        val live = sample.copy(sample = false)
        assertEquals("14:05", compactFreshnessLabel(snapshot, live))
        assertEquals("14:05", stackRowAge(snapshot, live))
    }

    @Test
    fun aSampleRowTapOpensTheAppAndALiveRowTapOpensGitHub() {
        val row = SampleWidgetData.rows(rocket, now).first()
        val appIntent = Intent()
        assertSame(appIntent, rowIntent(row, sample, appIntent))
        assertNotSame(appIntent, rowIntent(row, sample.copy(sample = false), appIntent))
    }

    @Test
    fun theCompactSlotRendersTheSampleWord() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactFreshness(SampleWidgetData.snapshot(rocket, now), sample) }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo(SAMPLE_TIME_LABEL)
    }

    @Test
    fun aSampleSnapshotHasNoStaleRole() {
        assertNull(freshnessRole(SampleWidgetData.snapshot(rocket, now), sample))
    }
}
