package co.saari.repoglance.ui

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FreshnessClockTest {
    @Test
    fun nextTickAlignsToMinuteBoundary() {
        assertEquals(
            29_750L,
            millisUntilNextMinute(Instant.parse("2026-08-12T17:00:30.250Z")),
        )
    }

    @Test
    fun exactBoundaryWaitsOneMinuteInsteadOfSpinning() {
        assertEquals(
            60_000L,
            millisUntilNextMinute(Instant.parse("2026-08-12T17:00:00Z")),
        )
    }
}
