package co.saari.repoglance.model

import java.time.Instant

/**
 * Row payload for a [NavigatorList].
 *
 * Shape decision (Slice 1): a navigator list holds EITHER issue rows OR PR
 * rows, never a mix — every call site is already scoped to one leaf
 * [NavigatorMode] (ISSUES or PRS). [NavigatorMode.BOTH] is expressed by the
 * caller holding two separate [NavigatorList] instances side by side (one
 * `Issues`, one `Prs`), not by a single list carrying both row types. This
 * keeps row rendering monomorphic and makes the "AWAITING_MY_REVIEW is
 * PR-only" rule a type-level invariant enforced in [NavigatorList]'s init,
 * rather than a runtime field check that could silently pass an empty list.
 */
sealed interface NavigatorRows {
    data class Issues(val rows: List<IssueRow>) : NavigatorRows
    data class Prs(val rows: List<PrRow>) : NavigatorRows

    val size: Int
        get() = when (this) {
            is Issues -> rows.size
            is Prs -> rows.size
        }
}

/**
 * One page (or accumulated pages) of navigator rows for a single
 * [NavigatorFilter]. `pageSize` is fixed at [PAGE_SIZE]; pagination beyond
 * that is expressed by `hasMorePages`, not by a variable page size.
 */
data class NavigatorList(
    val filter: NavigatorFilter,
    val rows: NavigatorRows,
    val valueBasis: ValueBasis,
    val observedAt: Instant?,
    val pageSize: Int = PAGE_SIZE,
    val hasMorePages: Boolean = false,
) {
    init {
        require(pageSize == PAGE_SIZE) { "pageSize must be $PAGE_SIZE, got $pageSize" }
        when (valueBasis) {
            ValueBasis.UNKNOWN -> {
                require(rows.size == 0) { "UNKNOWN basis requires an empty navigator list" }
                require(observedAt == null) { "UNKNOWN basis requires a null observedAt" }
                require(!hasMorePages) { "UNKNOWN basis cannot claim more pages" }
            }
            ValueBasis.EXACT, ValueBasis.LAST_GOOD -> {
                require(observedAt != null) {
                    "EXACT/LAST_GOOD basis requires a non-null observedAt"
                }
            }
        }
        if (filter == NavigatorFilter.AWAITING_MY_REVIEW) {
            require(rows is NavigatorRows.Prs) {
                "AWAITING_MY_REVIEW is only valid for PR lists"
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
