package co.saari.repoglance.devpicker

/**
 * GrillTrack `widget-preview-look-049`: what the launcher's `Add to home screen`
 * sheet and its widget picker show for the Repository and Pinned repos widgets.
 *
 * Round 1 (widget-preview-round-1, 2026-09-30) put five candidates on the real
 * launcher sheet on the Pixel 10 Pro XL and the Fold emulator: A sample widget,
 * B skeleton, C your data, D annotated, E poster. The maintainer picked B, then
 * changed the answer to A before confirmation; A is now the production preview
 * (widget/WidgetPreview.kt). B-E were rejected and their picker code removed;
 * their captures and hashes are in
 * .grilltrack/proof/widget-preview-look-049-verify-20260930.md. The pre-lock
 * sheet showed the app icon; with previewLayout declared it can no longer be
 * reproduced, so entry 0 hands the sheet no preview and shows what the launcher
 * picks itself (the generated preview, or previewLayout once removed).
 */
internal enum class WidgetPreviewCandidate(
    val letter: String,
    val shortName: String,
    val sheet: String,
) {
    LAUNCHER(
        "0",
        "launcher's own preview",
        "No preview is handed to the sheet, so the launcher shows the widget's picker preview: the generated " +
            "preview on Android 15+, or previewLayout before Android 15 and after 'Remove generated previews'.",
    ),
    SAMPLE(
        "A",
        "sample widget (locked)",
        "Locked: the production path (WidgetPinning.request). The real widget filled with the sample " +
            "repositories and marked 'sample': the tertiary capsule on Repository, the tertiary header band on " +
            "Pinned repos. The launcher's widget picker shows the same preview (generated on Android 15+, " +
            "previewLayout before that).",
    ),
}
