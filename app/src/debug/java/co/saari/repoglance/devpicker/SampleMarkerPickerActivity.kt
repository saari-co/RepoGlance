@file:OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import androidx.glance.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import co.saari.repoglance.ContentUiState
import co.saari.repoglance.LiveUiState
import co.saari.repoglance.R
import co.saari.repoglance.data.LiveRepository
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.render.ClockLabel
import co.saari.repoglance.sample.SampleAccount
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.SampleModeStore
import co.saari.repoglance.state.latestPushRecordFor
import co.saari.repoglance.tile.TileTexts
import co.saari.repoglance.ui.LiveRepoGlanceScreen
import co.saari.repoglance.ui.theme.LocalSampleMarker
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.ui.theme.SampleMarker
import co.saari.repoglance.widget.CompactContent
import co.saari.repoglance.widget.LocalWidgetTones
import co.saari.repoglance.widget.RepoWidgetConfig
import co.saari.repoglance.widget.SampleWidgetData
import co.saari.repoglance.widget.StackEntry
import co.saari.repoglance.widget.StackHeader
import co.saari.repoglance.widget.StackRow
import co.saari.repoglance.widget.TallContent
import co.saari.repoglance.widget.TallLook
import co.saari.repoglance.widget.WidgetFreshness
import co.saari.repoglance.widget.WidgetTones
import co.saari.repoglance.widget.rowsForMode
import co.saari.repoglance.widget.stackHeaderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Column as GlanceColumn

/**
 * GrillTrack live variant picker for `sample-marker-040` — development tooling,
 * debug source set only.
 *
 * Left: the production [LiveRepoGlanceScreen] fed the sample account (catalog or
 * the saari-co/rocket repository view) at phone width. Right: the production
 * repo widget (CompactContent at the Fold's 2x1 cell, TallContent) and stack
 * widget pieces through GlanceRemoteViews, plus the tile subtitle from
 * TileTexts.sample. One [SampleMarkerCandidate] is provided through
 * LocalSampleMarker to both. Known gap: Glance LazyColumn rows render blank
 * outside a launcher, so the tall widget shows its header (and footer) only;
 * the stack rows are composed directly.
 * Gotcha: the app canvas is the production screen, so a thumbtack tap there
 * writes the phone's real sample pins (SampleModeStore); do not pin in the
 * picker, or leave sample mode afterwards to clear them.
 * Pointer/touch: tap a chip. Keyboard: `1`-`2` jump, DPAD left/right step,
 * `D` dark, `N` catalog/repository, `H` details, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/sample-marker-round-1.json.
 *
 * Launch: bin/verify-repoglance launch MIXED sample-marker-picker "" "" <A|B>
 */
class SampleMarkerPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = SampleMarkerCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: SampleMarkerCandidate.BANNER
        val context = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.preload(context)
                SampleModeStore.isActive(context)
            }
            setContent { Picker(initial) }
        }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
    }
}

@Composable
private fun Picker(initial: SampleMarkerCandidate) {
    val systemDark = isSystemInDarkTheme()
    var candidate by rememberSaveable { mutableStateOf(initial) }
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var repoView by rememberSaveable { mutableStateOf(false) }
    var collapsed by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val choices = SampleMarkerCandidate.entries

    fun pick(index: Int) {
        choices.getOrNull(index)?.let { candidate = it }
    }

    fun step(delta: Int) = pick((choices.indexOf(candidate) + delta + choices.size) % choices.size)

    fun reset() {
        candidate = initial
        dark = systemDark
        repoView = false
    }

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
                        event.key == Key.N -> repoView = !repoView
                        event.key == Key.H -> collapsed = !collapsed
                        event.key == Key.R -> reset()
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Chrome(
                    candidate = candidate,
                    dark = dark,
                    repoView = repoView,
                    collapsed = collapsed,
                    onPick = ::pick,
                    onToggleDark = { dark = !dark },
                    onToggleCanvas = { repoView = !repoView },
                    onToggleDetails = { collapsed = !collapsed },
                    onReset = ::reset,
                )
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    AppCanvas(candidate.marker, repoView)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                        TileCanvas(candidate.marker)
                        WidgetCanvas(candidate.marker, dark)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chrome(
    candidate: SampleMarkerCandidate,
    dark: Boolean,
    repoView: Boolean,
    collapsed: Boolean,
    onPick: (Int) -> Unit,
    onToggleDark: () -> Unit,
    onToggleCanvas: () -> Unit,
    onToggleDetails: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(
            "GrillTrack sample-marker-040 · " + if (collapsed) SHORT_QUESTION else QUESTION,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("repoglance:picker-question"),
        )
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            SampleMarkerCandidate.entries.forEachIndexed { index, entry ->
                FilterChip(
                    selected = entry == candidate,
                    onClick = { onPick(index) },
                    label = { Text("${entry.letter} ${entry.shortName}") },
                    modifier = Modifier.testTag("repoglance:picker-candidate-${entry.letter}"),
                )
                Spacer(Modifier.width(6.dp))
            }
            FilterChip(
                selected = dark,
                onClick = onToggleDark,
                label = { Text(if (dark) "dark preview" else "light preview") },
                modifier = Modifier.testTag("repoglance:picker-dark"),
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = repoView,
                onClick = onToggleCanvas,
                label = { Text(if (repoView) "repository view" else "catalog") },
                modifier = Modifier.testTag("repoglance:picker-canvas"),
            )
            Spacer(Modifier.width(6.dp))
            AssistChip(
                onClick = onToggleDetails,
                label = { Text(if (collapsed) "Details" else "Hide details") },
                modifier = Modifier.testTag("repoglance:picker-collapse"),
            )
            Spacer(Modifier.width(6.dp))
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
            "On canvas: sample marker ${candidate.letter} ${candidate.shortName} · " +
                "${if (repoView) "repository view" else "catalog"} · ${if (dark) "dark" else "light"} · " +
                "app at Pixel 10 Pro XL width (443 dp); widgets + tile beside; family look, mono labels " +
                "and tonal shapes locked",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private const val QUESTION =
    "decided (round 1 of five): B tonal banner marks SAMPLE MODE on the app screens, the repo and stack " +
        "widgets and the Quick Settings tile. A is the pre-lock look for comparison; C, D and E were rejected."
private const val SHORT_QUESTION = "decided: B tonal banner marks SAMPLE MODE across app, widgets and tile"
private val DIGIT_KEYS = listOf(Key.One, Key.Two)

@Composable
private fun AppCanvas(marker: SampleMarker, repoView: Boolean) {
    val now = remember { Instant.now() }
    val catalog = remember(now) { SampleAccount.catalog(now) }
    val rocket: LiveRepository = catalog.repositories.first { it.ref == ROCKET }
    Box(
        modifier = Modifier
            .width(APP_WIDTH)
            .fillMaxHeight()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .testTag("repoglance:picker-app"),
    ) {
        CompositionLocalProvider(LocalSampleMarker provides marker) {
            LiveRepoGlanceScreen(
                state = LiveUiState.Ready(catalog, now, SampleAccount.RATE_LIMIT),
                selectedRepository = if (repoView) rocket else null,
                contentState = if (repoView) {
                    ContentUiState.Ready(SampleAccount.content(rocket, now))
                } else {
                    ContentUiState.Idle
                },
                connectionReady = true,
                sampleMode = true,
                onConnectGitHub = {},
                onExploreSampleData = {},
                onLeaveSampleData = {},
                onCopyCodeAndOpenGitHub = { _, _ -> },
                onCancelGitHubAuthorization = {},
                onRetry = {},
                onSelectRepository = {},
                onBackToRepositories = {},
                onRefreshRepository = {},
                onManageGitHubAccess = {},
                onSignOut = {},
            )
        }
    }
}

@Composable
private fun TileCanvas(marker: SampleMarker) {
    val now = remember { Instant.now() }
    val latest = latestPushRecordFor(SampleAccount.catalog(now).repositories, now)
    val text = TileTexts.sample(latest, now, marker = marker)
    Column {
        Text(
            "Quick Settings tile (system-drawn: only this wording and the active state change)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .width(TILE_WIDTH)
                .testTag("repoglance:picker-tile"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_repoglance_glyph),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(TileTexts.LABEL, style = MaterialTheme.typography.titleSmall)
                    Text(text.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetCanvas(marker: SampleMarker, dark: Boolean) {
    val context = LocalContext.current
    val themed = remember(dark) {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        context.createConfigurationContext(config)
    }
    var rendered by remember { mutableStateOf<List<MarkerRendered>>(emptyList()) }
    LaunchedEffect(marker, dark) {
        val tones = WidgetTones.of(themed)
        val glance = GlanceRemoteViews()
        rendered = specs(themed).map { spec ->
            val result = glance.compose(context = themed, size = spec.size) {
                CompositionLocalProvider(LocalWidgetTones provides tones, LocalSampleMarker provides marker) {
                    GlanceTheme {
                        GlanceBox(modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)) {
                            spec.content()
                        }
                    }
                }
            }
            MarkerRendered(spec.label, spec.size, result.remoteViews)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().testTag("repoglance:picker-widgets"),
        factory = { MarkerWidgetHost(it) },
        update = { host -> host.show(themed, rendered) },
    )
}

private class MarkerRendered(val label: String, val size: DpSize, val views: RemoteViews)

private class MarkerSpec(val label: String, val size: DpSize, val content: @Composable () -> Unit)

private class MarkerWidgetHost(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, dp(40f))
    }

    fun show(themed: Context, rendered: List<MarkerRendered>) {
        removeAllViews()
        rendered.forEach { item ->
            addView(
                TextView(context).apply {
                    text = item.label
                    textSize = 11f
                    setTextColor(0xFF8A8A8E.toInt())
                },
            )
            val frame = FrameLayout(context).apply {
                layoutParams = LayoutParams(dp(item.size.width.value), dp(item.size.height.value)).apply {
                    bottomMargin = dp(12f)
                }
                clipToOutline = true
            }
            frame.addView(
                item.views.apply(themed, frame),
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            addView(frame)
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

private fun specs(context: Context): List<MarkerSpec> {
    val now = Instant.now()
    val config = RepoWidgetConfig(ROCKET, NavigatorMode.BOTH)
    val intent = Intent(context, SampleMarkerPickerActivity::class.java)
    val snapshot = SampleWidgetData.snapshot(ROCKET, now)
    val rows = rowsForMode(SampleWidgetData.rows(ROCKET, now), NavigatorMode.BOTH)
    val freshness = WidgetFreshness(
        now = now,
        clock = ClockLabel(ZoneId.systemDefault(), is24Hour = true, locale = Locale.getDefault()),
        rateLimitedUntil = null,
        sample = true,
    )
    val entries = STACK_PINS.map { StackEntry(it, SampleWidgetData.snapshot(it, now)) }
    return listOf(
        MarkerSpec("repo widget · compact 148x89 (Fold 2x1 cell)", COMPACT) {
            CompactContent(config, snapshot, intent, freshness)
        },
        MarkerSpec("repo widget · tall 250x150 (header; rows need a launcher)", TALL) {
            TallContent(config, snapshot, rows, freshness, intent)
        },
        MarkerSpec("stack widget · 250x190, three sample pins", STACK) { Stack(entries, freshness, intent) },
    )
}

@Composable
private fun Stack(entries: List<StackEntry>, freshness: WidgetFreshness, intent: Intent) {
    TallLook {
        GlanceColumn(modifier = GlanceModifier.fillMaxSize()) {
            StackHeader(stackHeaderLabel(entries.size, freshness), intent, null, LocalSampleMarker.current)
            entries.forEach { StackRow(intent, it, freshness) }
        }
    }
}

private val ROCKET = RepoRef("saari-co", "rocket")
private val STACK_PINS = listOf(
    RepoRef("saari-co", "rocket"),
    RepoRef("saari-co", "api-server"),
    RepoRef("dinkuskit", "infra"),
)
private val APP_WIDTH = 443.dp
private val TILE_WIDTH = 330.dp
private val COMPACT = DpSize(148.dp, 89.dp)
private val TALL = DpSize(250.dp, 150.dp)
private val STACK = DpSize(250.dp, 190.dp)
