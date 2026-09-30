package co.saari.repoglance.widget

import android.content.Intent
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.render.ClockLabel
import co.saari.repoglance.ui.theme.SampleMarker
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleMarkerTest {
    private val now = Instant.parse("2026-09-30T14:05:00Z")
    private val clock = ClockLabel(ZoneOffset.UTC, is24Hour = true, locale = Locale.US)
    private val sample = WidgetFreshness(now, clock, rateLimitedUntil = null, sample = true)
    private val rocket = RepoRef("saari-co", "rocket")
    private val config = RepoWidgetConfig(rocket, NavigatorMode.BOTH)

    @Test
    fun theLockedTonalBannerIsTheProductionMarker() {
        assertEquals(SampleMarker.BANNER, SampleMarker.Default)
        assertEquals(listOf(SampleMarker.CHIP_ROW, SampleMarker.BANNER), SampleMarker.entries.toList())
    }

    @Test
    fun theBannerKeepsTheVerifyTagsAndASignInButton() {
        val marks = source("app/src/main/java/co/saari/repoglance/ui/SampleMarks.kt")
        val banner = marks.substringAfter("internal fun SampleBanner(").substringBefore("\n}\n")
        for (tag in listOf("SAMPLE_BAR_TEST_TAG", "SAMPLE_CHIP_TEST_TAG", "SAMPLE_SIGN_IN_TEST_TAG")) {
            assertTrue("the banner keeps $tag", banner.contains(tag))
        }
        assertTrue(banner.contains("PrimaryButton(onClick = onSignIn"))
        assertTrue("emphasis comes from the sample tone", banner.contains("tone = sampleTone()"))
        val screen = source("app/src/main/java/co/saari/repoglance/ui/LiveRepoGlanceScreen.kt")
        val bar = screen.substringAfter("private fun SampleModeBar(").substringBefore("\n}\n")
        assertTrue(bar.contains("SampleMarker.BANNER -> SampleBanner(onSignIn, modifier)"))
    }

    @Test
    fun theSampleToneIsTheDynamicTertiaryRoleNotAStatusHue() {
        val tone = source("app/src/main/java/co/saari/repoglance/ui/theme/SampleMarker.kt")
            .substringAfter("fun sampleTone()")
        assertTrue(tone.contains("colorScheme.tertiaryContainer"))
        assertFalse(tone.contains("FamilyStatus"))
        val marks = source("app/src/main/java/co/saari/repoglance/widget/SampleWidgetMarks.kt")
        assertTrue(marks.contains("GlanceTheme.colors.tertiaryContainer"))
    }

    @Test
    fun aSampleCompactWidgetPutsSampleInATertiaryCapsule() = runGlanceAppWidgetUnitTest {
        provideComposable { CompactContent(config, SampleWidgetData.snapshot(rocket, now), Intent(), sample) }
        onNode(hasTestTag(SAMPLE_CAPSULE_TAG)).assertHasTextEqualTo(SAMPLE_TIME_LABEL)
    }

    @Test
    fun aLiveCompactWidgetHasNoSampleCapsule() = runGlanceAppWidgetUnitTest {
        provideComposable {
            CompactContent(config, SampleWidgetData.snapshot(rocket, now), Intent(), sample.copy(sample = false))
        }
        onNode(hasTestTag(LEDGER_FRESHNESS_TAG)).assertHasTextEqualTo("14:05")
    }

    private fun source(relative: String): String =
        Files.readAllBytes(repositoryRoot().resolve(relative)).toString(Charsets.UTF_8)

    private fun repositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: error("Could not find repository root")
}
