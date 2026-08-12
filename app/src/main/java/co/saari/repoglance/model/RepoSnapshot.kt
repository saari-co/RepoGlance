package co.saari.repoglance.model

import java.time.Instant
import java.util.Collections

/**
 * A single repo's at-a-glance state.
 *
 * Truth-rule invariants (enforced here, not left to the UI layer):
 * - `valueBasis == UNKNOWN` forces counts and repo observation metadata to
 *   unknown values — we know nothing, so nothing renders as current or as
 *   zero. Rate-limit state remains independent and may still be known.
 * - `valueBasis` in {EXACT, LAST_GOOD} requires all three counts non-null,
 *   non-negative, AND `observedAt` non-null — a value with a basis always
 *   carries the moment it was observed. Awaiting-review PRs cannot exceed
 *   the open-PR total.
 * - Navigator maps and row collections are defensively snapshotted before
 *   validation, so caller-owned mutable aliases cannot invalidate truth.
 */
class RepoSnapshot(
    val repo: RepoRef,
    val openPrs: Int?,
    val prsAwaitingMyReview: Int?,
    val openIssues: Int?,
    val defaultBranchCi: CiState,
    val latestRelease: ReleaseInfo?,
    val pushedAt: Instant?,
    val valueBasis: ValueBasis,
    val observedAt: Instant?,
    val rateLimit: RateLimitBucket,
    issueLists: Map<NavigatorFilter, NavigatorList> = emptyMap(),
    prLists: Map<NavigatorFilter, NavigatorList> = emptyMap(),
) {
    val issueLists: Map<NavigatorFilter, NavigatorList> = immutableMap(issueLists)
    val prLists: Map<NavigatorFilter, NavigatorList> = immutableMap(prLists)

    init {
        require(issueLists.all { (filter, list) ->
            filter == list.filter && list.rows is NavigatorRows.Issues
        }) {
            "issueLists keys must match their list filters and contain only issue rows"
        }
        require(prLists.all { (filter, list) ->
            filter == list.filter && list.rows is NavigatorRows.Prs
        }) {
            "prLists keys must match their list filters and contain only PR rows"
        }
        when (valueBasis) {
            ValueBasis.UNKNOWN -> {
                require(openPrs == null && prsAwaitingMyReview == null && openIssues == null) {
                    "UNKNOWN basis forces null counts"
                }
                require(observedAt == null && pushedAt == null && latestRelease == null) {
                    "UNKNOWN basis forces null observation metadata"
                }
                require(defaultBranchCi == CiState.UNKNOWN) {
                    "UNKNOWN basis requires UNKNOWN default-branch CI"
                }
            }
            ValueBasis.EXACT, ValueBasis.LAST_GOOD -> {
                require(openPrs != null && prsAwaitingMyReview != null && openIssues != null) {
                    "EXACT/LAST_GOOD basis requires non-null counts"
                }
                require(observedAt != null) {
                    "EXACT/LAST_GOOD basis requires a non-null observedAt"
                }
                require(openPrs >= 0 && prsAwaitingMyReview >= 0 && openIssues >= 0) {
                    "counts must never be negative"
                }
                require(prsAwaitingMyReview <= openPrs) {
                    "PRs awaiting my review cannot exceed open PRs"
                }
            }
        }
    }

    fun copy(
        repo: RepoRef = this.repo,
        openPrs: Int? = this.openPrs,
        prsAwaitingMyReview: Int? = this.prsAwaitingMyReview,
        openIssues: Int? = this.openIssues,
        defaultBranchCi: CiState = this.defaultBranchCi,
        latestRelease: ReleaseInfo? = this.latestRelease,
        pushedAt: Instant? = this.pushedAt,
        valueBasis: ValueBasis = this.valueBasis,
        observedAt: Instant? = this.observedAt,
        rateLimit: RateLimitBucket = this.rateLimit,
        issueLists: Map<NavigatorFilter, NavigatorList> = this.issueLists,
        prLists: Map<NavigatorFilter, NavigatorList> = this.prLists,
    ): RepoSnapshot = RepoSnapshot(
        repo = repo,
        openPrs = openPrs,
        prsAwaitingMyReview = prsAwaitingMyReview,
        openIssues = openIssues,
        defaultBranchCi = defaultBranchCi,
        latestRelease = latestRelease,
        pushedAt = pushedAt,
        valueBasis = valueBasis,
        observedAt = observedAt,
        rateLimit = rateLimit,
        issueLists = issueLists,
        prLists = prLists,
    )

    override fun equals(other: Any?): Boolean = this === other || (
        other is RepoSnapshot &&
            repo == other.repo &&
            openPrs == other.openPrs &&
            prsAwaitingMyReview == other.prsAwaitingMyReview &&
            openIssues == other.openIssues &&
            defaultBranchCi == other.defaultBranchCi &&
            latestRelease == other.latestRelease &&
            pushedAt == other.pushedAt &&
            valueBasis == other.valueBasis &&
            observedAt == other.observedAt &&
            rateLimit == other.rateLimit &&
            issueLists == other.issueLists &&
            prLists == other.prLists
        )

    override fun hashCode(): Int {
        var result = repo.hashCode()
        result = 31 * result + (openPrs ?: 0)
        result = 31 * result + (prsAwaitingMyReview ?: 0)
        result = 31 * result + (openIssues ?: 0)
        result = 31 * result + defaultBranchCi.hashCode()
        result = 31 * result + (latestRelease?.hashCode() ?: 0)
        result = 31 * result + (pushedAt?.hashCode() ?: 0)
        result = 31 * result + valueBasis.hashCode()
        result = 31 * result + (observedAt?.hashCode() ?: 0)
        result = 31 * result + rateLimit.hashCode()
        result = 31 * result + issueLists.hashCode()
        result = 31 * result + prLists.hashCode()
        return result
    }

    override fun toString(): String =
        "RepoSnapshot(repo=$repo, openPrs=$openPrs, prsAwaitingMyReview=$prsAwaitingMyReview, " +
            "openIssues=$openIssues, defaultBranchCi=$defaultBranchCi, latestRelease=$latestRelease, " +
            "pushedAt=$pushedAt, valueBasis=$valueBasis, observedAt=$observedAt, " +
            "rateLimit=$rateLimit, issueLists=$issueLists, prLists=$prLists)"

    private companion object {
        fun <K, V> immutableMap(source: Map<K, V>): Map<K, V> =
            Collections.unmodifiableMap(LinkedHashMap(source))
    }
}
