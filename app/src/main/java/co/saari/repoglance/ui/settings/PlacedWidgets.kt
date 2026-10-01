package co.saari.repoglance.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.SampleModeStore
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.widget.RepoWidgetConfigStore
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.StackWidgetReceiver

enum class WidgetKind(val title: String, val description: String, val testTag: String) {
    REPOSITORY(
        title = "Repository widget",
        description = "Open issues and PRs for one repository you choose, " +
            "and its latest items when you make it taller.",
        testTag = "repoglance:widgets-add-repository",
    ),
    PINNED_REPOS(
        title = "Pinned repos widget",
        description = "Every repository you pin, newest push first, with its issue and PR counts.",
        testTag = "repoglance:widgets-add-pinned",
    ),
}

sealed interface PlacedWidget {
    val appWidgetId: Int
    val kind: WidgetKind

    data class Repository(override val appWidgetId: Int, val config: RepoWidgetConfig?) : PlacedWidget {
        override val kind: WidgetKind get() = WidgetKind.REPOSITORY
    }

    data class PinnedRepos(override val appWidgetId: Int, val pinCount: Int) : PlacedWidget {
        override val kind: WidgetKind get() = WidgetKind.PINNED_REPOS
    }
}

data class WidgetsUiState(
    val pinSupported: Boolean,
    val placed: List<PlacedWidget>?,
    val sample: Boolean,
)

internal fun placedWidgets(
    repositoryIds: List<Int>,
    pinnedReposIds: List<Int>,
    config: (Int) -> RepoWidgetConfig?,
    pinCount: Int,
): List<PlacedWidget> =
    repositoryIds.distinct().sorted().map { PlacedWidget.Repository(it, config(it)) } +
        pinnedReposIds.distinct().sorted().map { PlacedWidget.PinnedRepos(it, pinCount) }

internal fun loadWidgetsUiState(context: Context): WidgetsUiState {
    val manager = AppWidgetManager.getInstance(context)
    val sample = SampleModeStore.isActive(context)
    val repositoryIds = manager.getAppWidgetIds(ComponentName(context, RepoWidgetReceiver::class.java))
    val pinnedReposIds = manager.getAppWidgetIds(ComponentName(context, StackWidgetReceiver::class.java))
    val pins = if (sample) SampleModeStore.pins(context) else AppPrefs.livePins(context)
    val placed = placedWidgets(
        repositoryIds = repositoryIds.toList(),
        pinnedReposIds = pinnedReposIds.toList(),
        config = { id ->
            if (sample) SampleModeStore.widgetConfig(context, id) else RepoWidgetConfigStore.load(context, id)
        },
        pinCount = pins.size,
    )
    return WidgetsUiState(pinSupported = manager.isRequestPinAppWidgetSupported, placed = placed, sample = sample)
}

internal fun PlacedWidget.headline(): String = when (this) {
    is PlacedWidget.Repository -> config?.repo?.full ?: WidgetKind.REPOSITORY.title
    is PlacedWidget.PinnedRepos -> WidgetKind.PINNED_REPOS.title
}

internal fun PlacedWidget.supporting(): String = when (this) {
    is PlacedWidget.Repository -> config?.let { "${WidgetKind.REPOSITORY.title} · ${feedLabel(it.mode)}" }
        ?: "Not set up · tap to choose a repository"
    is PlacedWidget.PinnedRepos -> when (pinCount) {
        0 -> "No pins yet · pin repositories in the list"
        1 -> "1 pinned repository"
        else -> "$pinCount pinned repositories"
    }
}

internal fun feedLabel(mode: NavigatorMode): String = when (mode) {
    NavigatorMode.ISSUES -> "Issues"
    NavigatorMode.PRS -> "PRs"
    NavigatorMode.BOTH -> "Issues and PRs"
}

internal const val WIDGETS_EMPTY_TEXT = "No RepoGlance widgets on your home screen yet."
internal const val WIDGETS_REMOVE_HINT =
    "To remove a widget, long-press it on your home screen and drag it to Remove."
internal const val WIDGETS_HOW_TO =
    "Your home screen can't add widgets from apps. Long-press an empty spot on your home screen, " +
        "tap Widgets, then find RepoGlance."
internal const val WIDGETS_SAMPLE_NOTE = "Sample mode: widgets you add show the sample repositories."
