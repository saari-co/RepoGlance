package co.saari.repoglance.model

sealed interface NavigatorScope {
    data object Account : NavigatorScope

    data class Org(val login: String) : NavigatorScope {
        init { require(RepoRef.isValidOwner(login)) { "invalid org login: \"$login\"" } }
    }

    data class Repo(val ref: RepoRef) : NavigatorScope
}
