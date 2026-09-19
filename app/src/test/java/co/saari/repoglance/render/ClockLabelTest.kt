package co.saari.repoglance.render

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockLabelTest {

    private val zone = ZoneId.of("America/Los_Angeles")
    private val now = Instant.parse("2026-09-19T21:30:00Z")
    private val clock24 = ClockLabel(zone, is24Hour = true, locale = Locale.US)
    private val clock12 = ClockLabel(zone, is24Hour = false, locale = Locale.US)

    @Test
    fun sameLocalDayShowsTimeOnlyInTheDeviceHourFormat() {
        val observed = Instant.parse("2026-09-19T21:05:00Z")
        assertEquals("14:05", clock24.format(observed, now))
        assertEquals("2:05 PM", clock12.format(observed, now))
    }

    @Test
    fun earlierDayWithinAWeekCarriesTheWeekday() {
        assertEquals("Thu 14:05", clock24.format(Instant.parse("2026-09-17T21:05:00Z"), now))
    }

    @Test
    fun theLocalZoneDecidesWhatTodayIs() {
        assertEquals("Fri 23:50", clock24.format(Instant.parse("2026-09-19T06:50:00Z"), now))
    }

    @Test
    fun aWeekOrOlderShowsTheDate() {
        assertEquals("12 Sep", clock24.format(Instant.parse("2026-09-12T18:00:00Z"), now))
    }

    @Test
    fun aClockAheadOfTheDeviceStillShowsTimeRatherThanAFutureDay() {
        assertEquals("14:40", clock24.format(Instant.parse("2026-09-19T21:40:00Z"), now))
    }
}
