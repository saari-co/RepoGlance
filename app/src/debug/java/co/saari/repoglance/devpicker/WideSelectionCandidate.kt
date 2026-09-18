package co.saari.repoglance.devpicker

/**
 * GrillTrack round `navigator-detail-round-1`, active slot `wide-selection`:
 * what a row tap does on the 600dp+ navigator layout. Debug source set only.
 *
 * [tapEffect] and [gitHubPath] are shown in the picker chrome so the person
 * deciding can read the sequence, not just the frame: the candidates differ
 * in behaviour, and stills of behaviours look alike.
 */
enum class WideSelectionCandidate(
    val letter: String,
    val shortName: String,
    val tapEffect: String,
    val gitHubPath: String,
) {
    PANE_FILL(
        "A",
        "Pane",
        "fills the detail pane on the right; the list stays",
        "the Open on GitHub button in the pane",
    ),
    LIST_ONLY(
        "B",
        "GitHub only",
        "opens the GitHub app beside the list; there is no in-app detail",
        "the tap itself",
    ),
    SECOND_TAP(
        "C",
        "Pane + 2nd tap",
        "fills the detail pane on the right; the list stays",
        "a second tap on the highlighted row, or the pane button",
    ),
    SHEET(
        "D",
        "Sheet",
        "raises a detail sheet over the full-width list",
        "the Open on GitHub button in the sheet",
    ),
    SINGLE_PANE(
        "E",
        "Replace",
        "replaces the list with the detail; the list is hidden until Back",
        "the Open on GitHub button on the detail",
    ),
    ;

    val label: String get() = "$letter $shortName"
}
