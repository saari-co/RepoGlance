package co.saari.repoglance.model

import java.time.Instant

data class PrRow(
    val number: Int,
    val title: String,
    val state: String, // "open" | "closed"
    val labels: List<String>,
    val author: String,
    val assignee: String?,
    val commentCount: Int,
    val updatedAt: Instant,
    val isDraft: Boolean,
    val reviewState: ReviewState,
    val ciRollup: CiState,
) {
    init {
        require(number > 0) { "PR number must be > 0, got $number" }
        require(state == "open" || state == "closed") { "invalid PR state: \"$state\"" }
        require(commentCount >= 0) { "commentCount must be >= 0, got $commentCount" }
    }
}
