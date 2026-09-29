@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)

package co.saari.repoglance.devpicker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import co.saari.repoglance.model.CiState
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RateLimitBucket
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.model.RepoSnapshot
import co.saari.repoglance.model.ValueBasis
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.CompactContent
import co.saari.repoglance.widget.LocalWidgetLook
import co.saari.repoglance.widget.LocalWidgetTones
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.widget.WidgetFreshness
import co.saari.repoglance.widget.WidgetTones
import co.saari.repoglance.widget.widgetClock
import java.time.Instant

/**
 * GrillTrack live variant picker for the `family-look` grill, round 5, slot
 * `compact-widget-crowding` — development tooling, debug source set only.
 *
 * Renders the production compact repo widget (CompactContent) through the
 * real Glance pipeline (GlanceRemoteViews -> RemoteViews) in four data
 * states with one [CompactLayoutCandidate] provided through LocalWidgetLook
 * and the family tones through LocalWidgetTones. Canvas: floor (140x64, the
 * widget minimum since compact-crowding-034) and the Fold's placed 2x1 size
 * (148x89), or mid (180x64) and wide (250x90).
 * Pointer/touch: tap a chip. Keyboard: `1`-`5` jump, DPAD left/right step,
 * `D` dark, `N` canvas, `H` details, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/family-look-round-5.json.
 *
 * Launch: bin/verify-repoglance launch MIXED compact-picker "" "" <A..E>
 */
class CompactVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = CompactLayoutCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: CompactLayoutCandidate.INLINE
        val choices = CompactLayoutCandidate.entries
        setContent { Picker(initial = initial, choices = choices) }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
    }
}

@Composable
private fun Picker(initial: CompactLayoutCandidate, choices: List<CompactLayoutCandidate>) {
    val systemDark = isSystemInDarkTheme()
    var candidate by rememberSaveable { mutableStateOf(initial) }
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var navigator by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun pick(index: Int) {
        choices.getOrNull(index)?.let { candidate = it }
    }

    fun step(delta: Int) = pick((choices.indexOf(candidate) + delta + choices.size) % choices.size)

    RepoGlanceTheme(darkTheme = dark) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val digit = DIGIT_KEYS.indexOf(event.key)
                    when {
                        digit >= 0 -> pick(digit)
                        event.key == Key.DirectionLeft -> step(-1)
                        event.key == Key.DirectionRight -> step(+1)
                        event.key == Key.D -> dark = !dark
                        event.key == Key.H -> collapsed = !collapsed
                        event.key == Key.N -> navigator = !navigator
                        event.key == Key.R -> {
                            candidate = initial
                            dark = systemDark
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PickerChrome(
                    choices = choices,
                    candidate = candidate,
                    dark = dark,
                    navigator = navigator,
                    onToggleCanvas = { navigator = !navigator },
                    collapsed = collapsed,
                    onPick = ::pick,
                    onToggleDark = { dark = !dark },
                    onToggleDetails = { collapsed = !collapsed },
                    onReset = {
                        candidate = initial
                        dark = systemDark
                    },
                )
                WidgetCanvas(candidate = candidate, dark = dark, tall = navigator)
            }
        }
    }
}

@Composable
private fun PickerChrome(
    choices: List<CompactLayoutCandidate>,
    candidate: CompactLayoutCandidate,
    dark: Boolean,
    navigator: Boolean,
    onToggleCanvas: () -> Unit,
    collapsed: Boolean,
    onPick: (Int) -> Unit,
    onToggleDark: () -> Unit,
    onToggleDetails: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(
            "GrillTrack family-look · " + if (collapsed) SHORT_QUESTION else QUESTION,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("repoglance:picker-question"),
        )
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, entry ->
                FilterChip(
                    selected = entry == candidate,
                    onClick = { onPick(index) },
                    label = { Text("${entry.letter} ${entry.shortName}") },
                    modifier = Modifier.testTag("repoglance:picker-candidate-${entry.letter}"),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            FilterChip(
                selected = dark,
                onClick = onToggleDark,
                label = { Text(if (dark) "dark preview" else "light preview") },
                modifier = Modifier.testTag("repoglance:picker-dark"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = navigator,
                onClick = onToggleCanvas,
                label = { Text(if (navigator) "mid + wide" else "floor + placed") },
                modifier = Modifier.testTag("repoglance:picker-canvas"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            AssistChip(
                onClick = onToggleDetails,
                label = { Text(if (collapsed) "Details" else "Hide details") },
                modifier = Modifier.testTag("repoglance:picker-collapse"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            AssistChip(
                onClick = onReset,
                label = { Text("Reset") },
                modifier = Modifier.testTag("repoglance:picker-reset"),
            )
        }
        if (!collapsed) {
            Text(
                "${candidate.letter}: ${candidate.description}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp).testTag("repoglance:picker-active"),
            )
        }
        Text(
            "On canvas: compact ${candidate.letter} ${candidate.shortName} · " +
                "${if (navigator) "mid + wide" else "floor + placed"} · " +
                "${if (dark) "dark" else "light"} · widget colours locked (widget-look-032); tall and stack untouched",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private const val QUESTION =
    "deciding: how the COMPACT repo widget keeps its repo name when a stale capsule (last good, rate " +
        "limited, no data) needs the top row. Capsule colours, the tall and stack widgets, and the tile " +
        "are not being decided."
private const val SHORT_QUESTION = "deciding: COMPACT widget crowding"
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetCanvas(candidate: CompactLayoutCandidate, dark: Boolean, tall: Boolean) {
    val context = LocalContext.current
    val themed = remember(dark) {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        context.createConfigurationContext(config)
    }
    var rendered by remember { mutableStateOf<List<CompactRendered>>(emptyList()) }
    LaunchedEffect(candidate, dark, tall) {
        val tones = WidgetTones.of(themed)
        val glance = GlanceRemoteViews()
        rendered = specs(themed, tall).map { spec ->
            val result = glance.compose(context = themed, size = spec.size) {
                CompositionLocalProvider(
                    LocalWidgetLook provides candidate.look,
                    LocalWidgetTones provides tones,
                ) {
                    GlanceTheme {
                        Box(modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)) {
                            spec.content()
                        }
                    }
                }
            }
            CompactRendered(spec.label, spec.size, result.remoteViews)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        factory = { CompactHost(it) },
        update = { host -> host.show(themed, rendered) },
    )
}

private class CompactRendered(val label: String, val size: DpSize, val views: RemoteViews)

private class CompactSpec(val label: String, val size: DpSize, val content: @Composable () -> Unit)

private class CompactHost(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(40f))
    }

    fun show(themed: Context, rendered: List<CompactRendered>) {
        removeAllViews()
        rendered.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            addView(row)
            pair.forEach { item ->
                val cell = LinearLayout(context).apply {
                    orientation = VERTICAL
                    setPadding(0, 0, dp(24f), dp(12f))
                }
                row.addView(cell)
                cell.addView(
                    TextView(context).apply {
                        text = item.label
                        textSize = 11f
                        setTextColor(0xFF8A8A8E.toInt())
                    },
                )
                val frame = FrameLayout(context).apply {
                    layoutParams = LayoutParams(dp(item.size.width.value), dp(item.size.height.value))
                    clipToOutline = true
                }
                frame.addView(
                    item.views.apply(themed, frame),
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                cell.addView(frame)
            }
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

private fun specs(context: Context, tall: Boolean): List<CompactSpec> {
    val now = Instant.now()
    val repo = RepoRef("saari-co", "RepoGlance")
    val config = RepoWidgetConfig(repo, NavigatorMode.BOTH)
    val intent = Intent(context, CompactVariantPickerActivity::class.java)
    val fresh = snapshot(repo, now, ValueBasis.EXACT, observedAgo = 600)
    val lastGood = snapshot(repo, now, ValueBasis.LAST_GOOD, observedAgo = THREE_DAYS)
    val ok = freshness(context, now, null)
    val limited = freshness(context, now, now.plusSeconds(1800))
    val sizes = if (tall) listOf(COMPACT_MID, COMPACT_WIDE) else listOf(COMPACT_FLOOR, COMPACT_PLACED)
    return listOf(
        "fresh" to Pair(fresh, ok),
        "last good · 3d" to Pair(lastGood, ok),
        "rate limited" to Pair(fresh, limited),
        "no data" to Pair(null, ok),
    ).flatMap { (label, state) ->
        sizes.map { size ->
            CompactSpec("$label · ${size.width.value.toInt()}x${size.height.value.toInt()}", size) {
                CompactContent(config, state.first, intent, state.second)
            }
        }
    }
}

private fun snapshot(repo: RepoRef, now: Instant, basis: ValueBasis, observedAgo: Long) = RepoSnapshot(
    repo = repo,
    openPrs = 4,
    prsAwaitingMyReview = 1,
    openIssues = 12,
    defaultBranchCi = CiState.UNKNOWN,
    latestRelease = null,
    pushedAt = now.minusSeconds(observedAgo + 1200),
    valueBasis = basis,
    observedAt = now.minusSeconds(observedAgo),
    rateLimit = RateLimitBucket.OK,
)

private fun freshness(context: Context, now: Instant, limitedUntil: Instant?) = WidgetFreshness(
    now = now,
    clock = widgetClock(context),
    rateLimitedUntil = limitedUntil,
)

private const val THREE_DAYS = 3L * 24 * 3600
private val COMPACT_FLOOR = DpSize(140.dp, 64.dp)
private val COMPACT_WIDE = DpSize(250.dp, 90.dp)
private val COMPACT_PLACED = DpSize(148.dp, 89.dp)
private val COMPACT_MID = DpSize(180.dp, 64.dp)
