package co.saari.repoglance.devlaunch

import android.content.Context
import androidx.core.content.edit
import co.saari.repoglance.state.SampleModeStore
import co.saari.repoglance.widget.WidgetRefresh

// Debug-only entry into the showcase (showcase-048): sample mode under the
// fictional SHOWCASE persona with no SAMPLE marker, so the website can show
// real screens of made-up repositories. This object is the only writer of
// SampleModeStore.KEY_SHOWCASE; a release build has no debug source set, so
// a user can never reach it. Leaving sample mode clears it like every other
// sample key.
internal object ShowcaseLaunch {
    suspend fun enter(context: Context) {
        context.applicationContext
            .getSharedPreferences(SampleModeStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(SampleModeStore.KEY_ACTIVE, true)
                putBoolean(SampleModeStore.KEY_SHOWCASE, true)
            }
        WidgetRefresh.updateAll(context.applicationContext)
    }
}
