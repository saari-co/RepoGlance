package co.saari.repoglance.render

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ClockLabel(
    val zone: ZoneId,
    val is24Hour: Boolean,
    val locale: Locale,
) {
    fun time(instant: Instant): String =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale).format(instant.atZone(zone))

    fun format(observedAt: Instant, now: Instant): String {
        val then = observedAt.atZone(zone)
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), now.atZone(zone).toLocalDate())
        return when {
            days <= 0 -> time(observedAt)
            days < WEEK_DAYS -> DateTimeFormatter.ofPattern("EEE", locale).format(then) + " " + time(observedAt)
            else -> DateTimeFormatter.ofPattern("d MMM", locale).format(then)
        }
    }

    private companion object {
        const val WEEK_DAYS = 7
    }
}
