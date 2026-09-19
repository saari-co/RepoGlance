package co.saari.repoglance.widget

import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.ClockLabel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackWidgetTest {

    private val now = Instant.parse("2026-09-19T06:31:30Z")
    private val clock = ClockLabel(ZoneOffset.UTC, is24Hour = true, locale = Locale.US)
    private val fresh = WidgetFreshness(now, clock, rateLimitedUntil = null)

    private fun snapshot(
        full: String,
        basis: ValueBasis = ValueBasis.EXACT,
        pushedAt: Instant? = null,
        observedAt: Instant? = now,
    ): RepoSnapshot {
        val (owner, name) = full.split('/')
        val known = basis != ValueBasis.UNKNOWN
        return RepoSnapshot(
            repo = RepoRef(owner, name),
            openPrs = if (known) 1 else null,
            prsAwaitingMyReview = if (known) 0 else null,
            openIssues = if (known) 2 else null,
            defaultBranchCi = CiState.UNKNOWN,
            latestRelease = null,
            pushedAt = pushedAt,
            valueBasis = basis,
            observedAt = if (known) observedAt else null,
            rateLimit = RateLimitBucket.UNKNOWN,
        )
    }

    @Test
    fun pinsAreOrderedByMostRecentPushWithUnknownPushLast() {
        val snapshots = mapOf(
            "a/old" to snapshot("a/old", pushedAt = now.minusSeconds(86_400)),
            "a/new" to snapshot("a/new", pushedAt = now.minusSeconds(60)),
            "a/nopush" to snapshot("a/nopush", pushedAt = null),
        )
        val entries = StackRows.order(listOf("a/old", "b/unloaded", "a/nopush", "a/new", "not a repo")) { snapshots[it.full] }
        assertEquals(listOf("a/new", "a/old", "a/nopush", "b/unloaded"), entries.map { it.repo.full })
        assertNull(entries.last().snapshot)
    }

    @Test
    fun noPinsMeansNoRowsAndTheEmptyStateLabel() {
        assertTrue(StackRows.order(emptySet()) { null }.isEmpty())
        assertEquals("Pin repositories in RepoGlance", STACK_EMPTY_LABEL)
    }

    @Test
    fun rowsCarryCountsAndTheirOwnClockTime() {
        val exact = snapshot("saari-co/RepoGlance", observedAt = Instant.parse("2026-09-19T06:31:00Z"))
        assertEquals("issues 2 · PRs 1 · review 0", stackRowCounts(exact))
        assertEquals("06:31", stackRowAge(exact, fresh))
        val lastGood = snapshot("saari-co/RepoGlance", ValueBasis.LAST_GOOD, observedAt = Instant.parse("2026-09-16T18:00:00Z"))
        assertEquals("last good Wed 18:00", stackRowAge(lastGood, fresh))
    }

    @Test
    fun aPinWithoutDataSaysSoAndNeverShowsZero() {
        assertEquals("no data", stackRowAge(null, fresh))
        assertNull(stackRowCounts(null))
        val unknown = snapshot("saari-co/RepoGlance", ValueBasis.UNKNOWN)
        assertEquals("no data", stackRowAge(unknown, fresh))
        assertNull(stackRowCounts(unknown))
    }

    @Test
    fun headerCountsPinsAndShowsBackOff() {
        assertEquals("Pinned · 3", stackHeaderLabel(3, fresh))
        val limited = fresh.copy(rateLimitedUntil = Instant.parse("2026-09-19T07:05:00Z"))
        assertEquals("Pinned · 3 · rate limited · resets 07:05", stackHeaderLabel(3, limited))
    }

    @Test
    fun theStackReadsOnlyLivePinsAndLiveSnapshots() {
        val source = String(Files.readAllBytes(root().resolve("app/src/main/java/co/saari/repoglance/widget/StackWidget.kt")), Charsets.UTF_8)
        assertTrue(source.contains("AppPrefs.livePins("))
        assertTrue(source.contains("LiveSnapshotStore.load("))
        for (fixture in listOf("AppPrefs.pinnedRepos(", "Fixtures.", "SnapshotStore.", "selectedScenario(", "WIDGET_PREVIEW_LABEL")) {
            val used = Regex("(?<![A-Za-z])" + Regex.escape(fixture)).containsMatchIn(source)
            assertFalse("the placed stack must not read fixture data: $fixture", used)
        }
        assertTrue("the stack redraws on the shared redraw counter", source.contains("currentState(WidgetRefresh.REDRAW_KEY)"))
    }

    @Test
    fun theHeaderOpensTheCatalogEvenFromAnOpenRepository() {
        val widget = String(Files.readAllBytes(root().resolve("app/src/main/java/co/saari/repoglance/widget/StackWidget.kt")), Charsets.UTF_8)
        val activity = String(Files.readAllBytes(root().resolve("app/src/main/java/co/saari/repoglance/MainActivity.kt")), Charsets.UTF_8)
        val catalogIntent = widget.substringAfter("private fun liveCatalogIntent(").substringBefore("\n}\n")
        assertTrue(catalogIntent.contains("putExtra(EXTRA_LIVE_CATALOG, true)"))
        val handler = activity.substringAfter("private fun handleLiveIntent(").substringBefore("\n    }\n")
        assertTrue(handler.contains("EXTRA_LIVE_CATALOG"))
        assertTrue(handler.indexOf("liveModel.backToRepositories()") < handler.indexOf("EXTRA_LIVE_REPO_FULL"))
    }

    private fun root(): Path {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }
}
