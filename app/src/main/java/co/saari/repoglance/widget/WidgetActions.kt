package co.saari.repoglance.widget

import co.saari.repoglance.model.NavigatorMode

/**
 * Shared repo Intent-extra contract between the widgets' tap actions and
 * [co.saari.repoglance.MainActivity], which reads this extra to pre-scope
 * its navigator to the tapped repo. Each widget builds its own explicit
 * `Intent(context, MainActivity::class.java).putExtra(EXTRA_REPO_FULL, ...)`
 * and passes it to `actionStartActivity(intent)` — chosen over a raw
 * ACTION_VIEW Intent from Glance because opening the app pre-scoped is the
 * documented Slice 2 choice (see widget/RepoWidget.kt and
 * widget/StackWidget.kt KDoc).
 */
const val EXTRA_REPO_FULL: String = "repo_full"

/** Optional companion to [EXTRA_REPO_FULL] for opening the navigator in the
 * mode selected for one widget instance. Missing or malformed values stay
 * safe and predictable by falling back to [NavigatorMode.BOTH]. */
const val EXTRA_NAVIGATOR_MODE: String = "navigator_mode"

internal fun navigatorModeFromExtra(value: String?): NavigatorMode =
    NavigatorMode.entries.firstOrNull { it.name == value } ?: NavigatorMode.BOTH
