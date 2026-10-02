package co.saari.repoglance.devlaunch

import android.content.Context
import androidx.core.content.edit
import co.saari.repoglance.sample.SamplePersona
import co.saari.repoglance.state.SampleModeStore
import co.saari.repoglance.widget.WidgetRefresh

// Debug-only entry into the showcase (showcase-048): sample mode under the
// fictional SHOWCASE persona with no SAMPLE marker, so the website can show
// real screens of made-up repositories. This object is the only writer of
// SampleModeStore.KEY_SHOWCASE; a release build has no debug source set, so
// a user can never reach it. Leaving sample mode clears it like every other
// sample key. Pins and widget configurations left behind by an earlier
// sample-mode run (the maintainer's owners) are dropped on entry so the
// showcase only ever shows the showcase persona; showcase pins survive a
// relaunch.
internal object ShowcaseLaunch {
    suspend fun enter(context: Context) {
        val prefs = context.applicationContext
            .getSharedPreferences(SampleModeStore.PREFS_NAME, Context.MODE_PRIVATE)
        val stalePins = stalePins(SampleModeStore.pins(context))
        val staleWidgetKeys = staleWidgetKeys(prefs.all)
        prefs.edit {
            putBoolean(SampleModeStore.KEY_ACTIVE, true)
            putBoolean(SampleModeStore.KEY_SHOWCASE, true)
            if (stalePins.isNotEmpty()) putStringSet(KEY_PINS, SampleModeStore.pins(context) - stalePins)
            staleWidgetKeys.forEach { remove(it) }
        }
        WidgetRefresh.updateAll(context.applicationContext)
    }

    internal fun stalePins(pins: Set<String>): Set<String> =
        pins.filterNot { belongsToShowcase(it) }.toSet()

    internal fun staleWidgetKeys(all: Map<String, *>): Set<String> {
        val staleIds = all.entries
            .filter { (key, value) ->
                key.startsWith(WIDGET_PREFIX) && key.endsWith(REPO_SUFFIX) && value is String
            }
            .filterNot { (_, value) -> belongsToShowcase(value as String) }
            .map { (key, _) -> key.removePrefix(WIDGET_PREFIX).removeSuffix(REPO_SUFFIX) }
        return staleIds
            .flatMap { id -> listOf("$WIDGET_PREFIX$id$REPO_SUFFIX", "$WIDGET_PREFIX$id$MODE_SUFFIX") }
            .toSet()
    }

    private fun belongsToShowcase(repoFull: String): Boolean =
        SamplePersona.of(repoFull.substringBefore('/')) == SamplePersona.SHOWCASE

    // Mirrors SampleModeStore's private key layout; ShowcaseLaunchTest pins it.
    private const val KEY_PINS = "pins"
    private const val WIDGET_PREFIX = "widget."
    private const val REPO_SUFFIX = ".repo"
    private const val MODE_SUFFIX = ".mode"
}
