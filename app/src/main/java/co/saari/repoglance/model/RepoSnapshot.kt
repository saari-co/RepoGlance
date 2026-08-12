package co.saari.repoglance.model

import java.time.Instant

/**
 * A single repo's at-a-glance state.
 *
 * Truth-rule invariants (enforced here, not left to the UI layer):
 * - `valueBasis == UNKNOWN` forces counts and repo observation metadata to
 *   unknown values — we know nothing, so nothing renders as current or as
 *   zero. Rate-limit state remains independent and may still be known.
 * - `valueBasis` in {EXACT, LAST_GOOD} requires all three counts non-null,
 *   non-negative, AND `observedAt` non-null — a value with a basis always
 *   carries the moment it was observed.
 */
data class RepoSnapshot(
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
    val issueLists: Map<NavigatorFilter, NavigatorList> = emptyMap(),
    val prLists: Map<NavigatorFilter, NavigatorList> = emptyMap(),
) {
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
            }
        }
    }
}
