package co.saari.repoglance.render

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AgesTest {

    private val now: Instant = Instant.parse("2026-01-01T12:00:00Z")

    @Test
    fun justNowUnder60Seconds() {
        assertEquals("just now", Ages.format(now.minusSeconds(30), now))
    }

    @Test
    fun minutesUnder60Minutes() {
        assertEquals("59m", Ages.format(now.minus(Duration.ofMinutes(59)), now))
    }

    @Test
    fun hoursRoundsDownFromMinutes() {
        assertEquals("1h", Ages.format(now.minus(Duration.ofMinutes(90)), now))
    }

    @Test
    fun daysUnder14Days() {
        assertEquals("1d", Ages.format(now.minus(Duration.ofHours(26)), now))
    }

    @Test
    fun weeksAt14DaysAndBeyond() {
        assertEquals("2w", Ages.format(now.minus(Duration.ofDays(14)), now))
    }

    @Test
    fun futureClampsToJustNow() {
        assertEquals("just now", Ages.format(now.plusSeconds(600), now))
    }

    @Test
    fun updatedLabelUnknownWhenObservedAtNull() {
        assertEquals("Updated: unknown", Ages.updatedLabel(null, now))
    }

    @Test
    fun updatedLabelJustNowHasNoTrailingAgo() {
        assertEquals("Updated just now", Ages.updatedLabel(now.minusSeconds(5), now))
    }

    @Test
    fun updatedLabelAppendsAgoOtherwise() {
        assertEquals("Updated 59m ago", Ages.updatedLabel(now.minus(Duration.ofMinutes(59)), now))
    }
}
