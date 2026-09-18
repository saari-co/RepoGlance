package co.saari.repoglance.widget

import co.saari.repoglance.model.NavigatorMode

const val EXTRA_REPO_FULL: String = "repo_full"

const val EXTRA_NAVIGATOR_MODE: String = "navigator_mode"

internal fun navigatorModeFromExtra(value: String?): NavigatorMode =
    NavigatorMode.entries.firstOrNull { it.name == value } ?: NavigatorMode.BOTH
