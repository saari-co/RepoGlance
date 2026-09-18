package co.saari.repoglance

import android.app.Application
import co.saari.repoglance.hooks.DebugHooks

class RepoGlanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugHooks.install()
    }
}
