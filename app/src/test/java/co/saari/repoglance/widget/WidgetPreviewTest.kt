package co.saari.repoglance.widget

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.color.ColorProvider
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.render.ClockLabel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreviewTest {
    private val now = Instant.parse("2026-09-30T14:05:00Z")
    private val clock = ClockLabel(ZoneOffset.UTC, is24Hour = true, locale = Locale.US)
    private val data = WidgetPreviews.data(now, clock)
    private val tone = WidgetTone(
        ink = ColorProvider(Color.Red, Color.Red),
        container = ColorProvider(Color.Red, Color.Red),
        onContainer = ColorProvider(Color.Red, Color.Red),
    )
    private val tones = WidgetTones(working = tone, failing = tone, neutral = tone)

    @Test
    fun thePreviewIsAlwaysTheMarkedSampleNeverLiveData() {
        assertTrue("widget-preview-look-049 A: previews are sample data", data.freshness.sample)
        assertEquals(null, data.freshness.rateLimitedUntil)
        assertEquals("saari-co/rocket", data.config.repo.full)
        assertEquals(ValueBasis.EXACT, data.snapshot?.valueBasis)
        assertEquals(
            listOf("saari-co/rocket", "saari-co/api-server", "dinkuskit/infra"),
            data.entries.map { it.repo.full },
        )
        assertTrue(data.entries.all { it.snapshot?.valueBasis == ValueBasis.EXACT })
    }

    @Test
    fun theRepositoryPreviewIsTheCompactSampleWidgetWithoutAClock() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetPreviewContent(WidgetPreviewKind.REPOSITORY, data, tones, Intent()) }
        onNode(hasTestTag(SAMPLE_CAPSULE_TAG)).assertHasTextEqualTo(SAMPLE_TIME_LABEL)
        onNode(hasTextEqualTo("rocket")).assertExists()
        onNode(hasTextEqualTo("5")).assertExists()
        onNode(hasTextEqualTo("3")).assertExists()
        onAllNodes(hasText("14:05")).assertCountEquals(0)
        onAllNodes(hasTestTag(LEDGER_FRESHNESS_TAG)).assertCountEquals(0)
    }

    @Test
    fun thePinnedPreviewIsTheSampleStackWithSampleInEveryRow() = runGlanceAppWidgetUnitTest {
        provideComposable { WidgetPreviewContent(WidgetPreviewKind.PINNED_REPOS, data, tones, Intent()) }
        onNode(hasTestTag(STACK_HEADER_TAG)).assertHasTextEqualTo("Pinned · 3")
        onAllNodes(hasTestTag(STACK_AGE_TAG)).assertCountEquals(3)
        onAllNodes(hasTextEqualTo(SAMPLE_TIME_LABEL)).assertCountEquals(3)
        onAllNodes(hasTestTag(STACK_COUNTS_TAG)).assertCountEquals(3)
        onAllNodes(hasText("14:05")).assertCountEquals(0)
    }

    @Test
    fun aTwelveHourClockOrALaterDayStillShowsNoTime() = runGlanceAppWidgetUnitTest {
        val later = WidgetPreviews.data(
            Instant.parse("2026-10-09T21:40:00Z"),
            ClockLabel(ZoneOffset.UTC, is24Hour = false, locale = Locale.US),
        )
        provideComposable { WidgetPreviewContent(WidgetPreviewKind.PINNED_REPOS, later, tones, Intent()) }
        onAllNodes(hasText("PM")).assertCountEquals(0)
        onAllNodes(hasText("as of")).assertCountEquals(0)
        onAllNodes(hasTextEqualTo(SAMPLE_TIME_LABEL)).assertCountEquals(3)
    }

    @Test
    fun theStaticFallbackSaysWhatTheGeneratedPreviewSays() {
        val strings = Regex("<string name=\"(widget_preview_[a-z0-9_]+)\"[^>]*>([^<]+)</string>")
            .findAll(source("app/src/main/res/values/widget_preview.xml"))
            .associate { it.groupValues[1] to it.groupValues[2] }
        val snapshot = data.snapshot!!
        assertEquals(data.config.repo.name, strings["widget_preview_repo_name"])
        assertEquals(SAMPLE_TIME_LABEL, strings["widget_preview_sample"])
        assertEquals(snapshot.openIssues.toString(), strings["widget_preview_repo_issues"])
        assertEquals(snapshot.openPrs.toString(), strings["widget_preview_repo_prs"])
        assertEquals(stackHeaderLabel(data.entries.size, data.freshness), strings["widget_preview_pinned_header"])
        data.entries.forEachIndexed { index, entry ->
            assertEquals(entry.repo.full, strings["widget_preview_pin_${index + 1}"])
            assertEquals(stackRowCounts(entry.snapshot), strings["widget_preview_pin_${index + 1}_counts"])
            assertEquals(SAMPLE_TIME_LABEL, stackRowAge(entry.snapshot, data.freshness))
        }
        assertEquals(data.entries.size, strings.keys.count { Regex("widget_preview_pin_[0-9]+").matches(it) })
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
