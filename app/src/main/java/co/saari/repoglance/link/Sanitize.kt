package co.saari.repoglance.link

/**
 * Text-safety layer for anything sourced from GitHub (titles, labels, repo
 * names, tags, ...) before it is ever shown on screen.
 *
 * Pipeline: strip ASCII/ISO control characters -> collapse whitespace runs
 * to a single space (and trim) -> redact any token-shaped or bearer-shaped
 * substring with "•••". The redaction patterns below are regex *shapes*,
 * not secret literals — no token-shaped string literal appears anywhere in
 * this file or its tests; hostile inputs are built at test time by runtime
 * concatenation only.
 */
object Sanitize {

    private val CONTROL_CHARS = Regex("\\p{Cntrl}")
    private val WHITESPACE_RUN = Regex("\\s+")

    private val REDACTION_PATTERNS = listOf(
        // Classic GitHub PAT prefixes: ghp_/gho_/ghu_/ghs_/ghr_ + 20+ alnum.
        Regex("gh[pousr]_[A-Za-z0-9]{20,}"),
        // Fine-grained PAT.
        Regex("github_pat_[A-Za-z0-9_]{20,}"),
        // "Authorization: <anything non-whitespace>", case-insensitive.
        Regex("(?i)authorization\\s*:\\s*\\S+"),
        // "Bearer <token>", case-insensitive.
        Regex("(?i)bearer\\s+[A-Za-z0-9._~+/=-]{8,}"),
    )

    fun displayText(raw: String): String {
        var text = CONTROL_CHARS.replace(raw, " ")
        text = WHITESPACE_RUN.replace(text, " ").trim()
        for (pattern in REDACTION_PATTERNS) {
            text = pattern.replace(text, "•••")
        }
        return text
    }

    fun isClean(raw: String): Boolean = displayText(raw) == raw
}
