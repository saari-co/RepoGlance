package co.saari.repoglance.devlaunch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.MainActivity
import co.saari.repoglance.devpicker.NavigatorVariantPickerActivity
import co.saari.repoglance.devpicker.SplashVariantPickerActivity
import co.saari.repoglance.devpicker.StatusColourVariantPickerActivity
import co.saari.repoglance.devpicker.WidgetVariantPickerActivity
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.hooks.RefreshProbe
import co.saari.repoglance.hooks.TransportFault
import co.saari.repoglance.refresh.BackgroundRefresh
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.EXTRA_REPO_FULL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Debug-only entry point for the verify-repoglance skill: one adb command
// puts the app into a named fixture scenario and opens a named screen, so an
// agent can reach every truth state without an account. Never ships: debug
// source set only.
//
//   adb shell am start -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity \
//     --es scenario RATE_LIMITED --es screen navigator --es repo acme/rocket --es mode BOTH
//
// screen: live (default) | navigator | picker | navigator-picker (extra candidate A..E)
//         | splash-picker (extra candidate <mark A..E>/<motion A..E>, extra slot mark|motion)
//         | status-picker (extra candidate A..E)
//         | checking (the production Checking screen held open)
//         | signin-finishing (the production post-token sign-in screens held open)
//         | none (apply the extras below and stay on the current screen)
// probeCommitDelaySeconds (long, optional): arms hooks.RefreshProbe once.
// rateFault (LOW | EXHAUSTED | OFF, optional) with rateFaultResetSeconds (long,
//   default 180): arms or clears hooks.TransportFault until that reset time.
// refreshNow (boolean, optional): enqueues the production one-time pinned
//   refresh (BackgroundRefresh.refreshNow), as a widget save does.
//
// The preference writes run on Dispatchers.IO before the next screen starts,
// so a cold launch logs no StrictMode disk read from this launcher and the
// next screen still sees the scenario (the loaded SharedPreferences instance
// is process-wide and apply() updates it in memory at once).
@SuppressLint("CustomSplashScreen")
class ScenarioLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra(EXTRA_SCENARIO)
            ?.let { name -> runCatching { FixtureScenario.valueOf(name) }.getOrNull() }
        val probeSeconds = intent.getLongExtra(EXTRA_PROBE_COMMIT_DELAY, 0L).takeIf { it > 0L }
        val rateFault = intent.getStringExtra(EXTRA_RATE_FAULT)?.takeIf { name ->
            name == RATE_FAULT_OFF || TransportFault.Kind.entries.any { it.name == name }
        }
        val rateFaultResetSeconds =
            intent.getLongExtra(EXTRA_RATE_FAULT_RESET_SECONDS, DEFAULT_RATE_FAULT_RESET_SECONDS)
        val refreshNow = intent.getBooleanExtra(EXTRA_REFRESH_NOW, false)
        val screen = intent.getStringExtra(EXTRA_SCREEN) ?: SCREEN_LIVE
        val next = if (screen == SCREEN_NONE) {
            null
        } else {
            nextIntent(screen).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                scenario?.let { AppPrefs.setSelectedScenario(applicationContext, it) }
                probeSeconds?.let { RefreshProbe.arm(applicationContext, it) }
                rateFault?.let { name ->
                    val kind = TransportFault.Kind.entries.firstOrNull { it.name == name }
                    TransportFault.arm(applicationContext, kind, rateFaultResetSeconds)
                }
                if (refreshNow) BackgroundRefresh.refreshNow(applicationContext)
            }
            if (next != null) startActivity(next)
            finish()
        }
    }

    private fun nextIntent(screen: String): Intent {
        HOLDER_SCREENS[screen]?.let { return Intent(this, it) }
        return extrasIntent(screen)
    }

    private fun extrasIntent(screen: String): Intent = when (screen) {
        SCREEN_NAVIGATOR_PICKER -> Intent(this, NavigatorVariantPickerActivity::class.java).apply {
            putExtra(EXTRA_REPO_FULL, intent.getStringExtra(EXTRA_REPO) ?: DEFAULT_REPO)
            putExtra(EXTRA_NAVIGATOR_MODE, intent.getStringExtra(EXTRA_MODE) ?: DEFAULT_MODE)
            val candidate = intent.getStringExtra(EXTRA_CANDIDATE)
            putExtra(EXTRA_CANDIDATE, candidate)
            putExtra(EXTRA_HYBRID, candidate?.endsWith(HYBRID_SUFFIX) == true)
        }
        SCREEN_STATUS_PICKER -> Intent(this, StatusColourVariantPickerActivity::class.java).apply {
            putExtra(EXTRA_CANDIDATE, intent.getStringExtra(EXTRA_CANDIDATE))
        }
        SCREEN_SPLASH_PICKER -> Intent(this, SplashVariantPickerActivity::class.java).apply {
            putExtra(EXTRA_CANDIDATE, intent.getStringExtra(EXTRA_CANDIDATE))
            putExtra(EXTRA_SLOT, intent.getStringExtra(EXTRA_SLOT))
        }
        SCREEN_NAVIGATOR -> mainIntent(Intent.ACTION_VIEW).apply {
            putExtra(EXTRA_REPO_FULL, intent.getStringExtra(EXTRA_REPO) ?: DEFAULT_REPO)
            putExtra(EXTRA_NAVIGATOR_MODE, intent.getStringExtra(EXTRA_MODE) ?: DEFAULT_MODE)
        }
        else -> mainIntent(Intent.ACTION_MAIN)
    }

    private fun mainIntent(intentAction: String): Intent =
        Intent(this, MainActivity::class.java).apply { action = intentAction }

    private companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_PROBE_COMMIT_DELAY = "probeCommitDelaySeconds"
        const val EXTRA_RATE_FAULT = "rateFault"
        const val EXTRA_RATE_FAULT_RESET_SECONDS = "rateFaultResetSeconds"
        const val EXTRA_REFRESH_NOW = "refreshNow"
        const val RATE_FAULT_OFF = "OFF"
        const val DEFAULT_RATE_FAULT_RESET_SECONDS = 180L
        const val EXTRA_REPO = "repo"
        const val EXTRA_MODE = "mode"
        const val SCREEN_LIVE = "live"
        const val SCREEN_NAVIGATOR = "navigator"
        const val SCREEN_PICKER = "picker"
        const val SCREEN_NAVIGATOR_PICKER = "navigator-picker"
        const val SCREEN_SPLASH_PICKER = "splash-picker"
        const val SCREEN_STATUS_PICKER = "status-picker"
        const val SCREEN_CHECKING = "checking"
        const val SCREEN_SIGNIN_FINISHING = "signin-finishing"
        const val SCREEN_NONE = "none"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_CANDIDATE = "candidate"
        const val EXTRA_HYBRID = "hybrid"
        const val HYBRID_SUFFIX = "+sheet"
        const val DEFAULT_REPO = "acme/rocket"
        const val DEFAULT_MODE = "BOTH"
        val HOLDER_SCREENS: Map<String, Class<out Activity>> = mapOf(
            SCREEN_PICKER to WidgetVariantPickerActivity::class.java,
            SCREEN_CHECKING to CheckingPreviewActivity::class.java,
            SCREEN_SIGNIN_FINISHING to SignInFinishingPreviewActivity::class.java,
        )
    }
}
