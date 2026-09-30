package co.saari.repoglance.devpicker

import co.saari.repoglance.ui.theme.SampleMarker

/**
 * GrillTrack `sample-marker-040`: how sample mode is marked across the app
 * screens, the repo and stack widgets and the Quick Settings tile, fed through
 * the production seam (ui/theme/SampleMarker.kt, LocalSampleMarker).
 *
 * Round 1 (sample-marker-round-1, 2026-09-30) showed five candidates on the
 * Fold: A chip row, B tonal banner, C top strip, D header badge, E bottom bar.
 * The maintainer locked B, now SampleMarker.Default. C, D and E were rejected
 * and their production code removed; their captures and hashes are in
 * .grilltrack/proof/sample-marker-040-verify-20260930.md. A stays as the
 * pre-lock reference (sample-app-038 and sample-widgets-039).
 */
internal enum class SampleMarkerCandidate(
    val letter: String,
    val shortName: String,
    val description: String,
    val marker: SampleMarker,
) {
    CHIP_ROW(
        "A",
        "chip row (pre-lock)",
        "Pre-lock: a row under the header with a SAMPLE chip, one sentence and a text 'Sign in with GitHub'. " +
            "Widgets: the plain word 'sample' where a live widget shows its time. Tile: 'Sample · repo · age'.",
        SampleMarker.CHIP_ROW,
    ),
    BANNER(
        "B",
        "tonal banner (locked)",
        "Locked: a tertiary tonal banner card under the header with SAMPLE, a sentence and a filled " +
            "'Sign in with GitHub' button. Widgets: 'sample' in a tertiary capsule (compact) and a tertiary " +
            "header band (tall, stack). Tile: 'Sample data · repo · age'.",
        SampleMarker.BANNER,
    ),
}
