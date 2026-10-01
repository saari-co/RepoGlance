package co.saari.repoglance.devpicker

import co.saari.repoglance.ui.settings.WidgetsLook

/**
 * GrillTrack `widgets-look-047`: how the in-app Widgets screen looks (the Add
 * entries for the Repository and Pinned repos widgets and the "On your home
 * screen" list), fed through the production seam
 * (ui/settings/WidgetsLook.kt, LocalWidgetsLook).
 *
 * Round 1 (widgets-look-round-1): five candidates on the real Widgets screen,
 * under label-typography-030, shape-control-031 and status-colour-029.
 */
internal enum class WidgetsLookCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val look: WidgetsLook,
) {
    CARDS(
        "A",
        "add cards, then list",
        "Two tonal 24 dp cards, one per widget, each with its name, a sentence and a tonal 'Add' capsule. " +
            "Below them, 'On your home screen' lists the placed widgets as plain rows.",
        WidgetsLook.CARDS,
    ),
    LIST(
        "B",
        "settings list",
        "No cards: everything is a Settings-style list row. 'Add a widget' rows carry an icon, the name, a " +
            "sentence and a + button (tap the row or +). Placed widgets follow as rows with icons.",
        WidgetsLook.LIST,
    ),
    PREVIEWS(
        "C",
        "preview tiles",
        "Two side-by-side tiles with a sketch of each widget's shape, its name, a sentence and a full-width " +
            "'Add'. Placed widgets follow as rows with icons.",
        WidgetsLook.PREVIEWS,
    ),
    PLACED_FIRST(
        "D",
        "placed first + Add",
        "What is on your home screen comes first. One full-width 'Add a widget' button below opens a dialog " +
            "to choose Repository or Pinned repos.",
        WidgetsLook.PLACED_FIRST,
    ),
    GROUPED(
        "E",
        "grouped by widget",
        "One tonal card per widget kind: its name, a sentence and 'Add' on top, then the widgets of that kind " +
            "already on your home screen inside the same card.",
        WidgetsLook.GROUPED,
    ),
}
