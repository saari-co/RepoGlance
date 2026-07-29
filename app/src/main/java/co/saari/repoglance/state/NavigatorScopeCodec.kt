package co.saari.repoglance.state

import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.model.RepoRef

/**
 * Encodes/decodes [NavigatorScope] to a (kind, value) pair of primitive
 * `String`s. A sealed interface holding a [RepoRef] is not directly
 * Bundle-saveable (it implements neither `Parcelable` nor `Serializable`),
 * so screens that need [NavigatorScope] to survive `rememberSaveable`
 * (config changes / process death) store these two strings instead and
 * decode back through this object.
 */
object NavigatorScopeCodec {
    fun kindOf(scope: NavigatorScope): String = when (scope) {
        is NavigatorScope.Account -> "ACCOUNT"
        is NavigatorScope.Org -> "ORG"
        is NavigatorScope.Repo -> "REPO"
    }

    fun valueOf(scope: NavigatorScope): String = when (scope) {
        is NavigatorScope.Account -> ""
        is NavigatorScope.Org -> scope.login
        is NavigatorScope.Repo -> scope.ref.full
    }

    /** Falls back to [NavigatorScope.Account] for an unrecognized kind or an
     *  invalid/unparseable repo [value] — never throws. */
    fun decode(kind: String, value: String): NavigatorScope = when (kind) {
        "ORG" -> runCatching { NavigatorScope.Org(value) }.getOrDefault(NavigatorScope.Account)
        "REPO" -> value.split("/", limit = 2).takeIf { it.size == 2 }
            ?.let { (owner, name) -> runCatching { NavigatorScope.Repo(RepoRef(owner, name)) }.getOrNull() }
            ?: NavigatorScope.Account
        else -> NavigatorScope.Account
    }
}
