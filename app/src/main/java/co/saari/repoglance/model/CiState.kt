package co.saari.repoglance.model

/**
 * Default-branch CI state for a repo. [NO_CI] is distinct from [PASSING]:
 * a repo with no workflows at all is never rendered as "passing" — that
 * would be unknown-as-zero in disguise. [UNKNOWN] is for "we could not
 * determine CI state", distinct from both.
 */
enum class CiState { PASSING, FAILING, RUNNING, NO_CI, UNKNOWN }
