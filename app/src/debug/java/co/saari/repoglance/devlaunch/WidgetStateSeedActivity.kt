package co.saari.repoglance.devlaunch

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.data.RateLimitSnapshot
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.state.LiveSnapshotStore
import co.saari.repoglance.state.RateLimitStore
import co.saari.repoglance.widget.WidgetRefresh
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

// Debug-only: puts the placed widgets into a named freshness state so
// verify-repoglance can capture launcher-hosted widgets in states live data
// rarely reaches. Never ships: debug source set only.
//
// last_good    relabels the stored snapshot for the repo LAST_GOOD, keeping
//              its real counts and real observedAt.
// rate_limited records an exhausted rate limit resetting in 30 minutes;
//              stashes a real stored rate-limit state first and refuses
//              if a real exhausted limit is active.
// no_data      removes the stored snapshot for the repo.
// restore      puts back the snapshot stashed before the first seed and
//              replaces a seeded rate limit with the stashed real one.
//
// The first seed stashes the real record; later seeds keep that stash.
// Every action writes its result line to files/widget-seed.txt and
// redraws all widgets.
//
//   bin/verify-repoglance widget-state last_good [owner/name]
class WidgetStateSeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = intent.getStringExtra("state").orEmpty()
        val full = intent.getStringExtra("repo") ?: "saari-co/RepoGlance"
        val repo = full.split('/').takeIf { it.size == 2 }?.let { RepoRef(it[0], it[1]) }
        val message = if (repo == null) "bad repo '$full'" else seed(this, state, repo)
        File(filesDir, RESULT_FILE).writeText("${Instant.now()} $message\n")
        lifecycleScope.launch {
            WidgetRefresh.updateAll(applicationContext)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun seed(context: Context, state: String, repo: RepoRef): String {
        val stash = context.getSharedPreferences(STASH, Context.MODE_PRIVATE)
        val current = LiveSnapshotStore.load(context, repo)
        if (state != "restore" && !stash.contains(repo.full)) {
            stash.edit()
                .putString(repo.full, current?.let { LiveSnapshotStore.encode(it) } ?: NONE)
                .putBoolean(RATE_SEEDED, false)
                .apply()
        }
        val now = Instant.now()
        return when (state) {
            "last_good" -> {
                val base = current ?: return "last_good: no stored snapshot for ${repo.full}"
                LiveSnapshotStore.save(context, base.copy(valueBasis = ValueBasis.LAST_GOOD))
                "last_good: ${repo.full} relabelled LAST_GOOD, observedAt ${base.observedAt}"
            }
            "rate_limited" -> {
                val real = RateLimitStore.load(context)
                if (real != null && !stash.getBoolean(RATE_SEEDED, false)) {
                    if (RateLimitStore.exhaustedUntil(real, now) != null) {
                        return "rate_limited: refused, a real exhausted rate limit is active"
                    }
                    stash.edit()
                        .putString(RATE_BUCKET, real.bucket.name)
                        .putString(RATE_RESETS, real.resetsAt?.toString())
                        .apply()
                }
                RateLimitStore.record(
                    context,
                    RateLimitSnapshot(RateLimitBucket.EXHAUSTED, 0, null, now.plusSeconds(RESET_SECONDS)),
                    now,
                )
                stash.edit().putBoolean(RATE_SEEDED, true).apply()
                "rate_limited: exhausted until ${now.plusSeconds(RESET_SECONDS)}"
            }
            "no_data" -> {
                context.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE).edit().remove(repo.full).apply()
                "no_data: removed stored snapshot for ${repo.full}"
            }
            "restore" -> restore(context, repo)
            else -> "unknown state '$state' (last_good|rate_limited|no_data|restore)"
        }
    }

    private fun restore(context: Context, repo: RepoRef): String {
        val stash = context.getSharedPreferences(STASH, Context.MODE_PRIVATE)
        val raw = stash.getString(repo.full, null) ?: return "restore: nothing stashed for ${repo.full}"
        val snapshots = context.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE).edit()
        if (raw == NONE) snapshots.remove(repo.full) else snapshots.putString(repo.full, raw)
        snapshots.apply()
        if (stash.getBoolean(RATE_SEEDED, false)) {
            RateLimitStore.clear(context)
            stash.getString(RATE_BUCKET, null)?.let { bucket ->
                val resets = stash.getString(RATE_RESETS, null)?.let(Instant::parse)
                RateLimitStore.record(
                    context,
                    RateLimitSnapshot(RateLimitBucket.valueOf(bucket), null, null, resets),
                    Instant.now(),
                )
            }
        }
        stash.edit().remove(repo.full).remove(RATE_SEEDED).remove(RATE_BUCKET).remove(RATE_RESETS).apply()
        return "restore: ${repo.full} back to the stashed record"
    }

    private companion object {
        const val RESULT_FILE = "widget-seed.txt"
        const val STASH = "repoglance_debug_widget_seed"
        const val RATE_SEEDED = "rateSeeded"
        const val RATE_BUCKET = "realRateBucket"
        const val RATE_RESETS = "realRateResetsAt"
        const val NONE = "<none>"
        const val RESET_SECONDS = 1800L
        const val SNAPSHOT_PREFS = "repoglance_live_snapshots"
    }
}
