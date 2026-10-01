package co.saari.repoglance

import android.app.Application
import co.saari.repoglance.hooks.DebugHooks
import co.saari.repoglance.widget.WidgetPreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RepoGlanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugHooks.install()
        val app = this
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { WidgetPreviews.publishIfNeeded(app) }
    }
}
