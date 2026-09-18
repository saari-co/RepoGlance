package co.saari.repoglance.model

import java.time.Instant
import java.util.Collections

sealed interface NavigatorRows {
    class Issues(rows: List<IssueRow>) : NavigatorRows {
        val rows: List<IssueRow> = Collections.unmodifiableList(ArrayList(rows))

        override fun equals(other: Any?): Boolean = other is Issues && rows == other.rows
        override fun hashCode(): Int = rows.hashCode()
        override fun toString(): String = "Issues(rows=$rows)"
    }

    class Prs(rows: List<PrRow>) : NavigatorRows {
        val rows: List<PrRow> = Collections.unmodifiableList(ArrayList(rows))

        override fun equals(other: Any?): Boolean = other is Prs && rows == other.rows
        override fun hashCode(): Int = rows.hashCode()
        override fun toString(): String = "Prs(rows=$rows)"
    }

    val size: Int
        get() = when (this) {
            is Issues -> rows.size
            is Prs -> rows.size
        }
}

data class NavigatorList(
    val filter: NavigatorFilter,
    val rows: NavigatorRows,
    val valueBasis: ValueBasis,
    val observedAt: Instant?,
    val rateLimit: RateLimitBucket,
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
                requireNotNull(observedAt) {
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
