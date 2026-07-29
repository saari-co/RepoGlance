package co.saari.repoglance.model

/**
 * How current a numeric value is. [EXACT] is a fresh fetch. [LAST_GOOD] is
 * an honestly aged value from a previous successful fetch (paired with a
 * non-null `observedAt`). [UNKNOWN] means we have no value at all — never
 * rendered as zero.
 */
enum class ValueBasis { EXACT, LAST_GOOD, UNKNOWN }
