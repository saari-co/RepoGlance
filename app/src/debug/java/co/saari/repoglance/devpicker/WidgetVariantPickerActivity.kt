package co.saari.repoglance.devpicker

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import android.content.Intent
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.widget.CompactContent
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.model.ValueBasis
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * GrillTrack live variant picker — development tooling, debug source set only.
 *
 * Renders the five compact-widget candidates through the real Glance
 * composition pipeline (GlanceRemoteViews -> RemoteViews) at the exact
 * COMPACT_SIZE the widget declares, so text fit and truncation are faithful
 * rather than approximated by a Compose mock.
 *
 * Manifest: .grilltrack/work/picker/compact-widget-round-1.json
 *
 * Launch:
 *   adb shell am start -n co.saari.repoglance.debug/co.saari.repoglance.devpicker.WidgetVariantPickerActivity
 */
class WidgetVariantPickerActivity : ComponentActivity() {

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(24f), dp(20f), dp(40f))
            setBackgroundColor(BACKDROP)
        }
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(BACKDROP)
                addView(column)
            },
        )

        val now = Instant.parse("2026-09-17T23:00:00Z")
        val remoteViews = GlanceRemoteViews()

        // Verification mode: the real production CompactContent, not a
        // candidate copy. Two data states prove the truth-rule rendering.
        val config = RepoWidgetConfig(RepoRef("saariuslystoned", "x-api"), NavigatorMode.BOTH)
        val intent = Intent(this, WidgetVariantPickerActivity::class.java)
        val candidates: List<Pair<String, @Composable (RepoSnapshot?, Instant) -> Unit>> = listOf(
            "PRODUCTION — exact counts" to { s, n -> CompactContent(config, s, intent, n) },
            "PRODUCTION — last good, 3 days old" to { s, n ->
                CompactContent(config, s?.copy(valueBasis = ValueBasis.LAST_GOOD, observedAt = n.minusSeconds(3 * 24 * 3600)), intent, n)
            },
            "PRODUCTION — no observation yet" to { _, n -> CompactContent(config, null, intent, n) },
        )

        lifecycleScope.launch {
            for ((label, content) in candidates) {
                column.addView(heading(label))
                val row = LinearLayout(this@WidgetVariantPickerActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                column.addView(row)
                for ((sizeLabel, size) in SIZE_SWEEP) {
                    val snapshot = busyRepo(now)
                    val cell = LinearLayout(this@WidgetVariantPickerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, 0, dp(16f), 0)
                    }
                    row.addView(cell)
                    cell.addView(caption(sizeLabel))
                    val result = remoteViews.compose(
                        context = this@WidgetVariantPickerActivity,
                        size = size,
                        content = { content(snapshot, now) },
                    )
                    val host = FrameLayout(this@WidgetVariantPickerActivity).apply {
                        setBackgroundColor(0xFF1B1B1F.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            dp(size.width.value),
                            dp(size.height.value),
                        ).apply { gravity = Gravity.START; bottomMargin = dp(10f) }
                        addView(
                            result.remoteViews.apply(this@WidgetVariantPickerActivity, this),
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                    cell.addView(host)
                }
            }
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun heading(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(0xFFFFFFFF.toInt())
        setPadding(0, dp(18f), 0, dp(6f))
    }

    private fun caption(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 10f
        setTextColor(0xFF8A8A8E.toInt())
        setPadding(0, 0, 0, dp(4f))
    }

    private companion object {
        /**
         * The widget declares COMPACT_SIZE = 120x64dp as its floor, and treats
         * anything under TALL_BREAKPOINT = 100dp tall as compact. Width is what
         * actually varies across launcher grids, so sweep it at compact height.
         */
        const val BACKDROP = 0xFF101014.toInt()

        val SIZE_SWEEP = listOf(
            "120x64dp  (floor)" to DpSize(120.dp, 64.dp),
            "180x64dp  (mid)" to DpSize(180.dp, 64.dp),
            "250x90dp  (wide)" to DpSize(250.dp, 90.dp),
        )

        /** Busy repository: worst case for text fit. */
        fun busyRepo(now: Instant): RepoSnapshot = snapshot(
            RepoRef("saariuslystoned", "x-api"), openIssues = 128, openPrs = 23, awaiting = 7, now = now,
        )

        fun snapshot(
            repo: RepoRef,
            openIssues: Int,
            openPrs: Int,
            awaiting: Int,
            now: Instant,
        ): RepoSnapshot = RepoSnapshot(
            repo = repo,
            openPrs = openPrs,
            prsAwaitingMyReview = awaiting,
            openIssues = openIssues,
            defaultBranchCi = CiState.PASSING,
            latestRelease = null,
            pushedAt = now.minusSeconds(1200),
            valueBasis = ValueBasis.EXACT,
            observedAt = now.minusSeconds(7200),
            rateLimit = RateLimitBucket.OK,
        )
    }
}
