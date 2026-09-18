package co.saari.repoglance.link

object Sanitize {

    private val CONTROL_CHARS = Regex("\\p{Cc}")
    private val FORMAT_CONTROLS = Regex("\\p{Cf}")
    private val WHITESPACE_RUN = Regex("[\\s\\p{Z}]+")

    private val REDACTION_PATTERNS = listOf(

        Regex("gh[pousr]_[A-Za-z0-9]{20,}"),

        Regex("github_pat_[A-Za-z0-9_]{20,}"),

        Regex("(?i)authorization\\s*:\\s*\\S+\\s+\\S+"),

        Regex("(?i)authorization\\s*:\\s*\\S+"),

        Regex("(?i)bearer\\s+[A-Za-z0-9._~+/=-]{8,}"),
    )

    fun displayText(raw: String): String {
        var text = CONTROL_CHARS.replace(raw, " ")
        text = FORMAT_CONTROLS.replace(text, "")
        text = WHITESPACE_RUN.replace(text, " ").trim()
        for (pattern in REDACTION_PATTERNS) {
            text = pattern.replace(text, "•••")
        }
        return text
    }

    fun isClean(raw: String): Boolean = displayText(raw) == raw
}
