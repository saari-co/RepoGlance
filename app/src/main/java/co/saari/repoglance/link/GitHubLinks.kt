package co.saari.repoglance.link

import co.saari.repoglance.model.RepoRef
import java.net.URLEncoder

/**
 * The ONLY place app deep links are built. Every function builds strictly
 * from typed parts ([RepoRef], validated numbers, tags) and every output
 * starts with "https://github.com/" — no function here accepts a raw URL
 * string, so there is no way to produce a non-github.com URL from this
 * object.
 */
object GitHubLinks {
    private const val BASE = "https://github.com/"

    fun repo(ref: RepoRef): String = base(ref)

    fun issues(ref: RepoRef): String = "${base(ref)}/issues"

    fun pulls(ref: RepoRef): String = "${base(ref)}/pulls"

    fun issue(ref: RepoRef, number: Int): String {
        require(number > 0) { "issue number must be > 0, got $number" }
        return "${base(ref)}/issues/$number"
    }

    fun pull(ref: RepoRef, number: Int): String {
        require(number > 0) { "pull number must be > 0, got $number" }
        return "${base(ref)}/pull/$number"
    }

    fun release(ref: RepoRef, tag: String): String {
        return "${base(ref)}/releases/tag/${encodeTag(tag)}"
    }

    private fun base(ref: RepoRef): String {
        // RepoRef's own charset already rules out "/", but its name pattern
        // still permits a bare ".." (e.g. name = ".."). Belt-and-braces: never
        // let ".." reach a URL we build, even though RepoRef makes it hard to
        // get here in the first place.
        require(!ref.full.contains("..")) { "repo ref must not contain \"..\": ${ref.full}" }
        return "$BASE${ref.owner}/${ref.name}"
    }

    private fun encodeTag(tag: String): String =
        URLEncoder.encode(tag, "UTF-8").replace("+", "%20")
}
