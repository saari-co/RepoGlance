package co.saari.repoglance.model

/**
 * [AWAITING_MY_REVIEW] is valid only for PR lists — see [NavigatorList]'s
 * init invariant, which enforces this at construction time.
 */
enum class NavigatorFilter { OPEN, MINE, MENTIONS, RECENTLY_UPDATED, AWAITING_MY_REVIEW }
