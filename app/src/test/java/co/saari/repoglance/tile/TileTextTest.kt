package co.saari.repoglance.tile

import co.saari.repoglance.state.LatestPushRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TileTextTest {
    private val now = Instant.parse("2026-09-18T12:00:00Z")

    @Test
    fun noRecordIsInactiveAndInvitesConnecting() {
        val text = TileTexts.of(null, now)
        assertEquals("Open to connect", text.subtitle)
        assertFalse(text.active)
    }

    @Test
    fun freshRecordShowsRepositoryAndPushAge() {
        val record = LatestPushRecord("saari-co/RepoGlance", now.minus(Duration.ofHours(3)), now.minus(Duration.ofMinutes(5)))
        val text = TileTexts.of(record, now)
        assertEquals("saari-co/RepoGlance · 3h", text.subtitle)
        assertTrue(text.active)
    }

    @Test
    fun recordObservedOverADayAgoNeverPosesAsCurrent() {
        val record = LatestPushRecord("saari-co/RepoGlance", now.minus(Duration.ofHours(3)), now.minus(Duration.ofHours(25)))
        val text = TileTexts.of(record, now)
        assertEquals("saari-co/RepoGlance · open to refresh", text.subtitle)
        assertTrue(text.active)
    }

    @Test
    fun lockedShadeNeverCarriesTheRepositoryName() {
        val record = LatestPushRecord("saari-co/private-thing", now.minus(Duration.ofHours(3)), now)
        val text = TileTexts.of(record, now, locked = true)
        assertFalse(text.subtitle.contains("private-thing"))
        assertFalse(text.contentDescription.contains("private-thing"))
        assertEquals("Unlock to see the latest push", text.subtitle)
        assertTrue(text.active)
        assertFalse(TileTexts.of(null, now, locked = true).active)
    }

    @Test
    fun serviceRedactsOnTheLockScreenItselfNotOnlyASecureOne() {
        val source = String(
            java.nio.file.Files.readAllBytes(
                repositoryRoot().resolve("app/src/main/java/co/saari/repoglance/tile/RepoGlanceTileService.kt"),
            ),
            Charsets.UTF_8,
        )
        val listening = source.substringAfter("override fun onStartListening()").substringBefore("override fun onClick()")
        assertTrue("the display guard must be the lock screen showing state", listening.contains("isLocked"))
        assertFalse("isSecure misses a locked phone without a secure method", listening.contains("isSecure"))
    }

    private fun repositoryRoot(): java.nio.file.Path {
        var dir: java.nio.file.Path? = java.nio.file.Paths.get("").toAbsolutePath()
        while (dir != null && !java.nio.file.Files.exists(dir.resolve("settings.gradle.kts"))) dir = dir.parent
        return requireNotNull(dir)
    }

    @Test
    fun repositoryNameIsSanitised() {
        val record = LatestPushRecord("evil/‮name", now, now)
        assertFalse(TileTexts.of(record, now).subtitle.contains('‮'))
    }
}
