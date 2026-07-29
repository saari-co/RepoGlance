package co.saari.repoglance.model

/**
 * A typed, validated `owner/name` GitHub repo reference. Construction
 * throws [IllegalArgumentException] for anything outside GitHub's charset —
 * this is the one place that charset is enforced, so every downstream
 * consumer (deep links included) can trust `owner` and `name` are safe to
 * splice into a URL path segment.
 */
data class RepoRef(val owner: String, val name: String) {
    init {
        require(isValidOwner(owner)) { "invalid GitHub owner: \"$owner\"" }
        require(NAME_PATTERN.matches(name)) { "invalid GitHub repo name: \"$name\"" }
    }

    val full: String get() = "$owner/$name"

    companion object {
        // Simplified from GitHub's real owner rule (no leading/trailing/double
        // hyphen enforcement) per the product spec: charset + length, plus an
        // explicit no-leading-hyphen check.
        private val OWNER_PATTERN = Regex("^[A-Za-z0-9-]{1,39}$")
        private val NAME_PATTERN = Regex("^[A-Za-z0-9._-]{1,100}$")

        fun isValidOwner(candidate: String): Boolean =
            OWNER_PATTERN.matches(candidate) && !candidate.startsWith("-")
    }
}
