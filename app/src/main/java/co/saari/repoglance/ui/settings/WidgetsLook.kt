package co.saari.repoglance.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

enum class WidgetsLook {
    CARDS,
    LIST,
    PREVIEWS,
    PLACED_FIRST,
    GROUPED,
    ;

    companion object {
        val Default = CARDS
    }
}

val LocalWidgetsLook = staticCompositionLocalOf { WidgetsLook.Default }
