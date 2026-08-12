package co.saari.repoglance.widget

import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RepoWidgetConfigurationTest {
    private val now = Instant.parse("2026-08-12T12:00:00Z")

    @Test
    fun missingOrMalformedModeDefaultsToBoth() {
        assertEquals(NavigatorMode.BOTH, navigatorModeFromExtra(null))
        assertEquals(NavigatorMode.BOTH, navigatorModeFromExtra("not-a-mode"))
        assertEquals(NavigatorMode.PRS, navigatorModeFromExtra("PRS"))
    }

    @Test
    fun persistedConfigDecodeValidatesRepoAndDefaultsModeToBoth() {
        assertEquals(
            RepoWidgetConfig(RepoRef("saari-co", "RepoGlance"), NavigatorMode.BOTH),
            RepoWidgetConfigStore.decode("saari-co/RepoGlance", null),
        )
        assertNull(RepoWidgetConfigStore.decode("missing-slash", "ISSUES"))
        assertNull(RepoWidgetConfigStore.decode("bad owner!/repo", "ISSUES"))
    }

    @Test
    fun fixtureCatalogIsUniqueAndSurvivesScenarioWithoutConfiguredRepo() {
        val catalog = WidgetFixtureData.availableSnapshots(now)
        assertEquals(catalog.size, catalog.map { it.repo.full }.distinct().size)

        val configured = RepoRef("dinkuskit", "blocks")
        val fallback = WidgetFixtureData.snapshotFor(configured, FixtureScenario.EMPTY, now)
        assertEquals(configured, fallback?.repo)
    }

    @Test
    fun bothIsOneRecentlyUpdatedMixedFeedWithExactLinks() {
        val repo = RepoRef("saari-co", "RepoGlance")
        val rows = WidgetFixtureData.recentRows(repo, NavigatorMode.BOTH, now)

        assertTrue(rows.zipWithNext().all { (first, second) -> first.updatedAt >= second.updatedAt })
        assertEquals(listOf(WidgetRowKind.ISSUE, WidgetRowKind.PR), rows.take(2).map { it.kind })
        assertEquals("https://github.com/saari-co/RepoGlance/issues/100", rows.first().url)
        assertEquals("https://github.com/saari-co/RepoGlance/pull/200", rows[1].url)
    }

    @Test
    fun feedModeExcludesTheOtherRowType() {
        val repo = RepoRef("saari-co", "RepoGlance")
        val issues = WidgetFixtureData.recentRows(repo, NavigatorMode.ISSUES, now)
        val prs = WidgetFixtureData.recentRows(repo, NavigatorMode.PRS, now)

        assertTrue(issues.isNotEmpty() && issues.all { it.kind == WidgetRowKind.ISSUE })
        assertTrue(prs.isNotEmpty() && prs.all { it.kind == WidgetRowKind.PR })
    }

    @Test
    fun compactBothSummaryShowsBothCounts() {
        val snapshot = WidgetFixtureData.snapshotFor(
            RepoRef("saari-co", "RepoGlance"),
            FixtureScenario.EXACT,
            now,
        )!!
        assertEquals("ISSUES 5 · PRS 2", widgetCountSummary(snapshot, NavigatorMode.BOTH))
        assertEquals("ISSUES 5", widgetCountSummary(snapshot, NavigatorMode.ISSUES))
        assertEquals("PRS 2", widgetCountSummary(snapshot, NavigatorMode.PRS))
    }

    @Test
    fun persistentFixtureWidgetsCarryAnUnambiguousPreviewLabel() {
        assertEquals("FIXTURE PREVIEW", WIDGET_PREVIEW_LABEL)
    }
}
