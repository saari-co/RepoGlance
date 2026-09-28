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
import co.saari.repoglance.render.CiColorRole
import co.saari.repoglance.render.ClockLabel
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.CompactContent
import co.saari.repoglance.widget.LocalWidgetLook
import co.saari.repoglance.widget.LocalWidgetTones
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.widget.StackEntry
import co.saari.repoglance.widget.StackHeader
import co.saari.repoglance.widget.StackRow
import co.saari.repoglance.widget.TallContent
import co.saari.repoglance.widget.TallLook
import co.saari.repoglance.widget.WidgetFixtureData
import co.saari.repoglance.widget.WidgetFreshness
import co.saari.repoglance.widget.WidgetTones
import co.saari.repoglance.widget.stackHeaderLabel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import androidx.glance.layout.Column as GlanceColumn

/**
 * GrillTrack live variant picker for the `family-look` grill, round 4, slot
 * `widget-freshness-and-labels` — development tooling, debug source set only.
 *
 * Renders the production repo widget (CompactContent, TallContent) and stack
 * widget (StackHeader, StackRow) through the real Glance pipeline
 * (GlanceRemoteViews -> RemoteViews) at declared widget sizes, with one
 * [WidgetLookCandidate] provided through LocalWidgetLook and the family tones
 * through LocalWidgetTones. Light/dark re-renders under a night-mode
 * configuration context. Canvas: compact (four data states at floor and wide
 * sizes) or tall + stack.
 * Pointer/touch: tap a chip. Keyboard: `1`-`5` jump, DPAD left/right step,
 * `D` dark, `N` canvas, `H` details, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/family-look-round-4.json.
 *
 * Launch: bin/verify-repoglance launch MIXED widget-look-picker "" "" <A..E>
 */
class WidgetLookVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = WidgetLookCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: WidgetLookCandidate.M3_DEFAULT
        val choices = if (initial == WidgetLookCandidate.HYBRID_C_COMPACT_D_TALL) listOf(initial) else ROUND_FIVE
        setContent { Picker(initial = initial, choices = choices) }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
    }
}

@Composable
private fun Picker(initial: WidgetLookCandidate, choices: List<WidgetLookCandidate>) {
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
    choices: List<WidgetLookCandidate>,
    candidate: WidgetLookCandidate,
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
                label = { Text(if (navigator) "tall + stack" else "compact") },
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
            "On canvas: widgets ${candidate.letter} ${candidate.shortName} · " +
                "${if (navigator) "tall + stack" else "compact"} · " +
                "${if (dark) "dark" else "light"} · app family look locked; widget CI deferred",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private const val QUESTION =
    "deciding: how the repo and stack WIDGETS show stale, last-good, rate-limited and no-data freshness " +
        "(colour and weight) and whether widget labels take the mono face. Widget CI and the tile are not " +
        "being decided."
private const val SHORT_QUESTION = "deciding: WIDGET freshness colour + label face"
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetCanvas(candidate: WidgetLookCandidate, dark: Boolean, tall: Boolean) {
    val context = LocalContext.current
    val themed = remember(dark) {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        context.createConfigurationContext(config)
    }
    var rendered by remember { mutableStateOf<List<Rendered>>(emptyList()) }
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
            Rendered(spec.label, spec.size, result.remoteViews)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        factory = { FlowLayoutHost(it) },
        update = { host -> host.show(themed, rendered) },
    )
}

private class Rendered(val label: String, val size: DpSize, val views: RemoteViews)

private class Spec(val label: String, val size: DpSize, val content: @Composable () -> Unit)

private class FlowLayoutHost(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(40f))
    }

    fun show(themed: Context, rendered: List<Rendered>) {
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

private fun specs(context: Context, tall: Boolean): List<Spec> {
    val now = Instant.now()
    val repo = RepoRef("saari-co", "RepoGlance")
    val config = RepoWidgetConfig(repo, NavigatorMode.BOTH)
    val intent = Intent(context, WidgetLookVariantPickerActivity::class.java)
    val fresh = snapshot(repo, now, ValueBasis.EXACT, observedAgo = 600)
    val lastGood = snapshot(repo, now, ValueBasis.LAST_GOOD, observedAgo = THREE_DAYS)
    val ok = freshness(now, null)
    val limited = freshness(now, now.plusSeconds(1800))
    if (!tall) {
        return listOf(
            "fresh" to Pair(fresh, ok),
            "last good · 3d" to Pair(lastGood, ok),
            "rate limited" to Pair(fresh, limited),
            "no data" to Pair(null, ok),
        ).flatMap { (label, state) ->
            listOf(COMPACT_FLOOR, COMPACT_WIDE).map { size ->
                Spec("compact $label · ${size.width.value.toInt()}x${size.height.value.toInt()}", size) {
                    CompactContent(config, state.first, intent, state.second)
                }
            }
        }
    }
    val rows = WidgetFixtureData.recentRows(repo, NavigatorMode.BOTH, now)
    val entries = listOf(
        StackEntry(repo, fresh),
        StackEntry(
            RepoRef("acme", "rocket"),
            snapshot(RepoRef("acme", "rocket"), now, ValueBasis.LAST_GOOD, THREE_DAYS),
        ),
        StackEntry(RepoRef("octoco", "infra"), null),
    )
    return listOf(
        Spec("tall · last good", TALL) { TallContent(config, lastGood, rows, ok, intent) },
        Spec("tall · rate limited", TALL) { TallContent(config, fresh, rows, limited, intent) },
        Spec("stack · mixed rows", STACK) { Stack(entries, ok, intent) },
        Spec("stack · rate limited", STACK) { Stack(entries, limited, intent) },
    )
}

@Composable
private fun Stack(entries: List<StackEntry>, freshness: WidgetFreshness, intent: Intent) {
    TallLook {
        GlanceColumn(modifier = GlanceModifier.fillMaxSize()) {
            StackHeader(
                stackHeaderLabel(entries.size, freshness),
                intent,
                CiColorRole.NEGATIVE.takeIf { freshness.rateLimitedUntil != null },
            )
            entries.forEach { StackRow(intent, it, freshness) }
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

private fun freshness(now: Instant, limitedUntil: Instant?) = WidgetFreshness(
    now = now,
    clock = ClockLabel(ZoneId.systemDefault(), is24Hour = true, locale = Locale.getDefault()),
    rateLimitedUntil = limitedUntil,
)

private const val THREE_DAYS = 3L * 24 * 3600
private val COMPACT_FLOOR = DpSize(120.dp, 64.dp)
private val COMPACT_WIDE = DpSize(250.dp, 90.dp)
private val TALL = DpSize(250.dp, 140.dp)
private val STACK = DpSize(250.dp, 170.dp)
