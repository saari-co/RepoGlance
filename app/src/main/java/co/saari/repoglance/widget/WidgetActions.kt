package co.saari.repoglance.widget

import androidx.glance.action.ActionParameters

/**
 * Shared repo Intent-extra contract between the widgets' tap actions and
 * [co.saari.repoglance.MainActivity], which reads this extra to pre-scope
 * its navigator to the tapped repo (chosen over a raw ACTION_VIEW Intent
 * from Glance because building an arbitrary Intent's extras inside a Glance
 * action is awkward; opening the app pre-scoped is the documented Slice 2
 * choice — see widget/RepoWidget.kt and widget/StackWidget.kt KDoc).
 */
const val EXTRA_REPO_FULL: String = "repo_full"

val ActionKeyRepoFull: ActionParameters.Key<String> = ActionParameters.Key(EXTRA_REPO_FULL)
