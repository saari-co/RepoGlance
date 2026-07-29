package co.saari.repoglance.model

/**
 * GitHub API rate-limit headroom, as a first-class state rather than a
 * silent failure mode. [EXHAUSTED] must always surface visibly; back off,
 * never silently keep serving old numbers as if they were fresh.
 */
enum class RateLimitBucket { OK, LOW, EXHAUSTED, UNKNOWN }
