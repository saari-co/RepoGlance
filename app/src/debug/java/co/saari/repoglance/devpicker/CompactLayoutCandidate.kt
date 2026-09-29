package co.saari.repoglance.devpicker

import co.saari.repoglance.widget.CompactLayout
import co.saari.repoglance.widget.WidgetLook

/**
 * Five candidates for the `family-look` grill, round 5, slot
 * `compact-widget-crowding`: how the compact repo widget keeps its repo name
 * when a stale capsule (last good, rate limited, no data) needs the top row.
 * Each keeps the locked widget-look-032 colours and swaps only
 * [CompactLayout] on WidgetLook.Family; A is the pre-lock inline layout.
 * The maintainer locked D on 2026-09-29 (with the widget floor raised to
 * 140 dp); it is now part of WidgetLook.Family, the production default.
 */
internal enum class CompactLayoutCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val look: WidgetLook,
) {
    INLINE(
        "A",
        "inline (shipped)",
        "Today: name and capsule share the top row; the name takes the leftover width, so a stale capsule " +
            "squeezes it to nothing at the floor.",
        WidgetLook.Family.copy(compact = CompactLayout.INLINE),
    ),
    OWN_ROW(
        "B",
        "capsule own row",
        "When stale, the capsule drops to its own row under the repo name; tighter vertical padding keeps " +
            "issues and PRs. Fresh widgets are unchanged.",
        WidgetLook.Family.copy(compact = CompactLayout.OWN_ROW),
    ),
    SHORT_NAME_FIRST(
        "C",
        "short words, name first",
        "Stays inline, but the name keeps its full width and the capsule takes what is left, with shorter " +
            "words: 'cached' for last good, 'limited' for rate limited.",
        WidgetLook.Family.copy(compact = CompactLayout.SHORT_NAME_FIRST),
    ),
    MERGED_COUNTS(
        "D",
        "merged counts",
        "When stale, the capsule gets its own row and the counts merge onto one bold 9 sp line " +
            "('12 issues · 4 PRs'). Fresh widgets keep today's rows.",
        WidgetLook.Family,
    ),
    RESPONSIVE(
        "E",
        "responsive bottom band",
        "Size-responsive: 180 dp wide and up stays inline; narrower, the capsule moves below the counts as a " +
            "bottom band and the name keeps the top row.",
        WidgetLook.Family.copy(compact = CompactLayout.RESPONSIVE),
    ),
}
