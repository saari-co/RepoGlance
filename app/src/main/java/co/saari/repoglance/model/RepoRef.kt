package co.saari.repoglance.model

data class RepoRef(val owner: String, val name: String) {
    init {
        require(isValidOwner(owner)) { "invalid GitHub owner: \"$owner\"" }
        require(NAME_PATTERN.matches(name)) { "invalid GitHub repo name: \"$name\"" }
        require(name != "." && name != "..") { "repo name must not be a dot segment" }
    }

    val full: String get() = "$owner/$name"

    companion object {

        private val OWNER_PATTERN = Regex("^[A-Za-z0-9-]{1,39}$")
        private val NAME_PATTERN = Regex("^[A-Za-z0-9._-]{1,100}$")

        fun isValidOwner(candidate: String): Boolean =
            OWNER_PATTERN.matches(candidate) && !candidate.startsWith("-")
    }
}
