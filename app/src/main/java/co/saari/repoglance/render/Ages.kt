package co.saari.repoglance.render

import java.time.Duration
import java.time.Instant

/**
 * Single source of truth for rendering an age (an [Instant] relative to
 * "now") as a short human label. Never renders a negative age — a
 * then-instant in the future clamps to "just now" rather than showing
 * something nonsensical like "-3m".
 */
object Ages {

    /** "just now" (<60s), "Nm" (<60m), "Nh" (<24h), "Nd" (<14d), otherwise "Nw". */
    fun format(then: Instant, now: Instant): String {
        val seconds = Duration.between(then, now).seconds
        if (seconds <= 0) return "just now"

        val minutes = seconds / 60
        if (minutes < 1) return "just now"
        if (minutes < 60) return "${minutes}m"

        val hours = minutes / 60
        if (hours < 24) return "${hours}h"

        val days = hours / 24
        if (days < 14) return "${days}d"

        val weeks = days / 7
        return "${weeks}w"
    }

    /** "Updated: unknown" when [observedAt] is null; otherwise "Updated <age> ago",
     *  special-cased to "Updated just now" (no trailing "ago") for the freshest bucket. */
    fun updatedLabel(observedAt: Instant?, now: Instant): String {
        if (observedAt == null) return "Updated: unknown"
        val age = format(observedAt, now)
        return if (age == "just now") "Updated just now" else "Updated $age ago"
    }
}
