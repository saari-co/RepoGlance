package co.saari.repoglance.devlaunch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import co.saari.repoglance.MainActivity
import co.saari.repoglance.devpicker.NavigatorVariantPickerActivity
import co.saari.repoglance.devpicker.SplashVariantPickerActivity
import co.saari.repoglance.devpicker.WidgetVariantPickerActivity
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.hooks.RefreshProbe
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.EXTRA_REPO_FULL

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
//         | checking (the production Checking screen held open)
// probeCommitDelaySeconds (long, optional): arms hooks.RefreshProbe once.
@SuppressLint("CustomSplashScreen")
class ScenarioLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_SCENARIO)?.let { name ->
            val scenario = runCatching { FixtureScenario.valueOf(name) }.getOrNull()
            if (scenario != null) {
                AppPrefs.setSelectedScenario(this, scenario)
            }
        }
        intent.getLongExtra(EXTRA_PROBE_COMMIT_DELAY, 0L).takeIf { it > 0L }?.let { seconds ->
            RefreshProbe.arm(this, seconds)
        }
        val next = nextIntent(intent.getStringExtra(EXTRA_SCREEN) ?: SCREEN_LIVE)
        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(next)
        finish()
    }

    private fun nextIntent(screen: String): Intent = when (screen) {
        SCREEN_PICKER -> Intent(this, WidgetVariantPickerActivity::class.java)
        SCREEN_NAVIGATOR_PICKER -> Intent(this, NavigatorVariantPickerActivity::class.java).apply {
            putExtra(EXTRA_REPO_FULL, intent.getStringExtra(EXTRA_REPO) ?: DEFAULT_REPO)
            putExtra(EXTRA_NAVIGATOR_MODE, intent.getStringExtra(EXTRA_MODE) ?: DEFAULT_MODE)
            val candidate = intent.getStringExtra(EXTRA_CANDIDATE)
            putExtra(EXTRA_CANDIDATE, candidate)
            putExtra(EXTRA_HYBRID, candidate?.endsWith(HYBRID_SUFFIX) == true)
        }
        SCREEN_CHECKING -> Intent(this, CheckingPreviewActivity::class.java)
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
        const val EXTRA_REPO = "repo"
        const val EXTRA_MODE = "mode"
        const val SCREEN_LIVE = "live"
        const val SCREEN_NAVIGATOR = "navigator"
        const val SCREEN_PICKER = "picker"
        const val SCREEN_NAVIGATOR_PICKER = "navigator-picker"
        const val SCREEN_SPLASH_PICKER = "splash-picker"
        const val SCREEN_CHECKING = "checking"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_CANDIDATE = "candidate"
        const val EXTRA_HYBRID = "hybrid"
        const val HYBRID_SUFFIX = "+sheet"
        const val DEFAULT_REPO = "acme/rocket"
        const val DEFAULT_MODE = "BOTH"
    }
}
