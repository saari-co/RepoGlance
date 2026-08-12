package co.saari.repoglance.render

import co.saari.repoglance.link.Sanitize
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.ReleaseInfo
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import java.time.Instant

/**
 * Display-rule single source of truth for [RepoSnapshot] fields. Slice 2's
 * UI reads only through these functions — no widget or screen re-derives
 * these rules independently.
 */
object SnapshotRendering {

    /** "—" (em dash) for UNKNOWN basis or a null count — never "0" for unknown. */
    fun countText(count: Int?, basis: ValueBasis): String {
        if (basis == ValueBasis.UNKNOWN || count == null) return "—"
        return count.toString()
    }

    fun ciLabel(ci: CiState): String = when (ci) {
        CiState.PASSING -> "Passing"
        CiState.FAILING -> "Failing"
        CiState.RUNNING -> "Running"
        CiState.NO_CI -> "No CI"
        CiState.UNKNOWN -> "Unknown"
    }

    fun isStale(basis: ValueBasis): Boolean = basis == ValueBasis.LAST_GOOD

    /** Non-null exactly when the snapshot's basis is LAST_GOOD. */
    fun ageChip(snapshot: RepoSnapshot, now: Instant): String? {
        if (!isStale(snapshot.valueBasis)) return null
        val observedAt = snapshot.observedAt ?: return null
        return Ages.format(observedAt, now)
    }

    fun rateLimitBanner(bucket: RateLimitBucket): String? = when (bucket) {
        RateLimitBucket.EXHAUSTED -> "Rate-limited — backing off"
        RateLimitBucket.LOW -> "Rate limit low"
        RateLimitBucket.OK, RateLimitBucket.UNKNOWN -> null
    }

    fun releaseLabel(release: ReleaseInfo?, basis: ValueBasis, now: Instant): String {
        if (basis == ValueBasis.UNKNOWN) return "Latest release: unknown"
        if (release == null) return "No releases"
        return "${Sanitize.displayText(release.tag)} · ${Ages.format(release.publishedAt, now)}"
    }

    fun pushedLabel(pushedAt: Instant?, now: Instant): String {
        if (pushedAt == null) return "Last push: unknown"
        val age = Ages.format(pushedAt, now)
        return if (age == "just now") "Pushed just now" else "Pushed $age ago"
    }
}
