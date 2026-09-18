package co.saari.repoglance.render

import java.time.Duration
import java.time.Instant

object Ages {

    fun format(then: Instant, now: Instant): String {
        val seconds = Duration.between(then, now).seconds
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds <= 0 || minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 14 -> "${days}d"
            else -> "${days / 7}w"
        }
    }

    fun updatedLabel(observedAt: Instant?, now: Instant): String {
        if (observedAt == null) return "Updated: unknown"
        val age = format(observedAt, now)
        return if (age == "just now") "Updated just now" else "Updated $age ago"
    }
}
