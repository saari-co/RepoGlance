package co.saari.repoglance.model

import java.time.Instant
import java.util.Collections

class PrRow(
    val repo: RepoRef,
    val number: Int,
    val title: String,
    val state: String, // "open" | "closed"
    labels: List<String>,
    val author: String,
    val assignee: String?,
    val commentCount: Int,
    val updatedAt: Instant,
    val isDraft: Boolean,
    val reviewState: ReviewState,
    val ciRollup: CiState,
) {
    val labels: List<String> = Collections.unmodifiableList(ArrayList(labels))

    init {
        require(number > 0) { "PR number must be > 0, got $number" }
        require(state == "open" || state == "closed") { "invalid PR state: \"$state\"" }
        require(commentCount >= 0) { "commentCount must be >= 0, got $commentCount" }
    }

    override fun equals(other: Any?): Boolean = this === other || (
        other is PrRow &&
            repo == other.repo &&
            number == other.number &&
            title == other.title &&
            state == other.state &&
            labels == other.labels &&
            author == other.author &&
            assignee == other.assignee &&
            commentCount == other.commentCount &&
            updatedAt == other.updatedAt &&
            isDraft == other.isDraft &&
            reviewState == other.reviewState &&
            ciRollup == other.ciRollup
        )

    override fun hashCode(): Int {
        var result = repo.hashCode()
        result = 31 * result + number
        result = 31 * result + title.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + (assignee?.hashCode() ?: 0)
        result = 31 * result + commentCount
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + isDraft.hashCode()
        result = 31 * result + reviewState.hashCode()
        result = 31 * result + ciRollup.hashCode()
        return result
    }

    override fun toString(): String =
        "PrRow(repo=$repo, number=$number, title=$title, state=$state, labels=$labels, " +
            "author=$author, assignee=$assignee, commentCount=$commentCount, updatedAt=$updatedAt, " +
            "isDraft=$isDraft, reviewState=$reviewState, ciRollup=$ciRollup)"
}
