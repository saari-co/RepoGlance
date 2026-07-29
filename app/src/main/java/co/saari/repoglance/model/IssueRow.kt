package co.saari.repoglance.model

import java.time.Instant

data class IssueRow(
    val number: Int,
    val title: String,
    val state: String, // "open" | "closed"
    val labels: List<String>,
    val author: String,
    val assignee: String?,
    val commentCount: Int,
    val updatedAt: Instant,
) {
    init {
        require(number > 0) { "issue number must be > 0, got $number" }
        require(state == "open" || state == "closed") { "invalid issue state: \"$state\"" }
        require(commentCount >= 0) { "commentCount must be >= 0, got $commentCount" }
    }
}
