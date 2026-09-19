package co.saari.repoglance.hooks

import android.content.Context
import java.time.Instant

// Debug-only probe for the stale-session proof (GrillTrack
// widget-background-refresh-023, ClawSweeper round 2 on PR #22). Armed once
// through ScenarioLaunchActivity (--el probeCommitDelaySeconds N), it holds
// the next LiveRefresh.persist for N seconds after its network work and
// before the session commit, so a sign-out can land inside that window. It
// records only its own state and timestamps, never repository data. The
// release flavour is a no-op.
object RefreshProbe {
    private const val PREFS_NAME = "repoglance_debug_probe"

    @Volatile
    private var delaySeconds = 0L

    fun arm(context: Context, seconds: Long) {
        delaySeconds = seconds
        record(context, "armed")
    }

    fun beforeCommit(context: Context) {
        val seconds = delaySeconds
        if (seconds <= 0L) return
        delaySeconds = 0L
        record(context, "holding")
        Thread.sleep(seconds * 1000L)
        record(context, "released")
    }

    private fun record(context: Context, state: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(state, Instant.now().toString())
            .apply()
    }
}
