@file:OptIn(ExperimentalLayoutApi::class, ExperimentalGlanceRemoteViewsApi::class, ExperimentalComposeUiApi::class)

package co.saari.repoglance.devpicker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.intSetOf
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViews
import co.saari.repoglance.ui.settings.WidgetKind
import co.saari.repoglance.ui.settings.WidgetPinning
import co.saari.repoglance.ui.theme.LabelRole
import co.saari.repoglance.ui.theme.LabelText
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.RepoWidgetReceiver
import co.saari.repoglance.widget.StackWidgetReceiver
import co.saari.repoglance.widget.WidgetPreviewContent
import co.saari.repoglance.widget.WidgetPreviewKind
import co.saari.repoglance.widget.WidgetPreviews
import co.saari.repoglance.widget.WidgetTones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

/**
 * GrillTrack picker for `widget-preview-look-049` after the lock — development
 * tooling, debug source set only.
 *
 * The canvas is the launcher's own `Add to home screen` sheet. Entry A goes
 * through the production WidgetPinning.request, the same call the Widgets
 * screen's Add makes; entry 0 hands the sheet no preview, so the launcher shows
 * the widget's picker preview. `Remove generated previews` drops the Android
 * 15+ generated previews so the previewLayout fallback shows in the sheet and
 * the launcher's widget picker; `Publish previews now` republishes them
 * (rate-limited by the system to about two calls per hour per widget). Below,
 * the production preview content renders inline in light and dark.
 * Gotcha: `Add to home screen` on the sheet places a real widget (A opens the
 * Repository setup); swipe the sheet away instead, or remove the widget after.
 * Pointer/touch: tap a chip or button. Keyboard: `0`/`1` pick, `S` Repository
 * sheet, `P` Pinned repos sheet, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/widget-preview-round-1.json.
 *
 * Launch: bin/verify-repoglance launch MIXED widget-preview-picker "" "" <0|A>
 */
class WidgetPreviewPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = WidgetPreviewCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: WidgetPreviewCandidate.SAMPLE
        setContent { Picker(initial) }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
    }
}

private enum class SheetKind(
    val label: String,
    val tag: String,
    val widget: WidgetKind,
    val preview: WidgetPreviewKind,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
    val size: DpSize,
) {
    REPOSITORY(
        "Repository",
        "repoglance:picker-sheet-repository",
        WidgetKind.REPOSITORY,
        WidgetPreviewKind.REPOSITORY,
        RepoWidgetReceiver::class,
        DpSize(148.dp, 89.dp),
    ),
    PINNED(
        "Pinned repos",
        "repoglance:picker-sheet-pinned",
        WidgetKind.PINNED_REPOS,
        WidgetPreviewKind.PINNED_REPOS,
        StackWidgetReceiver::class,
        DpSize(250.dp, 180.dp),
    ),
}

private suspend fun openSheet(context: Context, candidate: WidgetPreviewCandidate, kind: SheetKind): Boolean =
    when (candidate) {
        WidgetPreviewCandidate.SAMPLE -> WidgetPinning.request(context, kind.widget)
        WidgetPreviewCandidate.LAUNCHER -> AppWidgetManager.getInstance(context).requestPinAppWidget(
            ComponentName(context, kind.receiver.java),
            null,
            null,
        )
    }

private fun previewStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return "generated previews need Android 15"
    val manager = AppWidgetManager.getInstance(context)
    return SheetKind.entries.joinToString(" · ") { kind ->
        val published = manager.getWidgetPreview(
            ComponentName(context, kind.receiver.java),
            Process.myUserHandle(),
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        ) != null
        "${kind.label}: " + if (published) "generated preview published" else "no generated preview (previewLayout)"
    }
}

private fun placedSizes(context: Context): String {
    val manager = AppWidgetManager.getInstance(context)
    val placed = SheetKind.entries.flatMap { kind ->
        manager.getAppWidgetIds(ComponentName(context, kind.receiver.java)).map { id ->
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            "id $id ${kind.label} ${width}x${height}dp"
        }
    }
    return placed.joinToString(" · ").ifEmpty { "none" }
}

private suspend fun removeGenerated(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return "needs Android 15"
    val manager = AppWidgetManager.getInstance(context)
    withContext(Dispatchers.IO) {
        SheetKind.entries.forEach {
            manager.removeWidgetPreview(
                ComponentName(context, it.receiver.java),
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
        }
    }
    return "removed generated previews"
}

private suspend fun publishGenerated(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return "needs Android 15"
    val glance = GlanceAppWidgetManager(context)
    return withContext(Dispatchers.IO) {
        val results = SheetKind.entries.map { kind ->
            kind to glance.setWidgetPreviews(kind.receiver, intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN))
        }
        results.joinToString(" · ") { (kind, result) ->
            "${kind.label} " + if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                "published"
            } else {
                "rate limited"
            }
        }
    }
}

@Composable
private fun Picker(initial: WidgetPreviewCandidate) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    var candidate by rememberSaveable { mutableStateOf(initial) }
    var trace by rememberSaveable { mutableStateOf(READY) }
    var status by remember { mutableStateOf(previewStatus(app)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val choices = WidgetPreviewCandidate.entries

    fun stamp() = " at " + LocalTime.now().format(STAMP)

    fun pick(entry: WidgetPreviewCandidate) {
        candidate = entry
        trace = "picked ${entry.letter} ${entry.shortName}; open a sheet to see it on the launcher"
    }

    fun reset() {
        candidate = initial
        trace = READY
        status = previewStatus(app)
    }

    fun sheet(kind: SheetKind) {
        val active = candidate
        trace = "${active.letter} ${active.shortName} → composing the ${kind.label} sheet…"
        scope.launch {
            val accepted = runCatching { openSheet(app, active, kind) }
            trace = "${active.letter} ${active.shortName} → ${kind.label} sheet " +
                accepted.fold(
                    onSuccess = { if (it) "requested; the launcher accepted" else "refused: launcher cannot pin" },
                    onFailure = { "failed: ${it.javaClass.simpleName}" },
                ) + stamp()
        }
    }

    fun generated(action: suspend (Context) -> String) {
        scope.launch {
            trace = runCatching { action(app) }.getOrElse { "failed: ${it.javaClass.simpleName}" } + stamp()
            status = previewStatus(app)
        }
    }

    RepoGlanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    event.type == KeyEventType.KeyDown && onKey(event.key, ::pick, ::sheet, ::reset)
                },
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    "GrillTrack widget-preview-look-049 · $QUESTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("repoglance:picker-question"),
                )
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    choices.forEach { entry ->
                        FilterChip(
                            selected = entry == candidate,
                            onClick = { pick(entry) },
                            label = { Text("${entry.letter} ${entry.shortName}") },
                            modifier = Modifier.testTag("repoglance:picker-candidate-${entry.letter}"),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Text(
                    "${candidate.letter}: ${candidate.sheet}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp).testTag("repoglance:picker-active"),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    SheetKind.entries.forEach { kind ->
                        FilledTonalButton(
                            onClick = { sheet(kind) },
                            modifier = Modifier.weight(1f).testTag(kind.tag),
                        ) {
                            LabelText("Open ${kind.label} sheet", LabelRole.BUTTON)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                GeneratedControls(onAction = ::generated, onReset = ::reset)
                Text(
                    "Last: $trace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("repoglance:picker-trace"),
                )
                Text(
                    "Picker previews: $status (stamp ${WidgetPreviews.stamp()}). Placed: ${placedSizes(app)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("repoglance:picker-generated"),
                )
                InlineRenders()
            }
        }
    }
}

private fun onKey(
    key: Key,
    pick: (WidgetPreviewCandidate) -> Unit,
    sheet: (SheetKind) -> Unit,
    reset: () -> Unit,
): Boolean {
    when (key) {
        Key.Zero -> pick(WidgetPreviewCandidate.LAUNCHER)
        Key.One -> pick(WidgetPreviewCandidate.SAMPLE)
        Key.S -> sheet(SheetKind.REPOSITORY)
        Key.P -> sheet(SheetKind.PINNED)
        Key.R -> reset()
        else -> return false
    }
    return true
}

@Composable
private fun GeneratedControls(
    onAction: (suspend (Context) -> String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier.fillMaxWidth()) {
        AssistChip(
            onClick = { onAction(::removeGenerated) },
            label = { Text("Remove generated previews") },
            modifier = Modifier.testTag("repoglance:picker-remove-generated"),
        )
        Spacer(Modifier.width(6.dp))
        AssistChip(
            onClick = { onAction(::publishGenerated) },
            label = { Text("Publish previews now") },
            modifier = Modifier.testTag("repoglance:picker-publish-generated"),
        )
        Spacer(Modifier.width(6.dp))
        AssistChip(
            onClick = onReset,
            label = { Text("Reset") },
            modifier = Modifier.testTag("repoglance:picker-reset"),
        )
    }
}

@Composable
private fun InlineRenders() {
    val context = LocalContext.current
    var rendered by remember { mutableStateOf<List<InlineRendered>>(emptyList()) }
    LaunchedEffect(Unit) {
        val glance = GlanceRemoteViews()
        val intent = Intent(context, WidgetPreviewPickerActivity::class.java)
        val data = WidgetPreviews.data(context)
        rendered = listOf(false, true).flatMap { dark ->
            val themed = themedContext(context, dark)
            val tones = WidgetTones.of(themed)
            SheetKind.entries.map { kind ->
                val views = glance.compose(context = themed, size = kind.size) {
                    WidgetPreviewContent(kind.preview, data, tones, intent)
                }.remoteViews
                InlineRendered("${kind.label} · ${if (dark) "dark" else "light"}", kind.size, views, themed)
            }
        }
    }
    Column {
        Text(
            "Inline render of the production preview (not the launcher sheet):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).testTag("repoglance:picker-inline"),
            factory = { InlineHost(it) },
            update = { host -> host.show(rendered) },
        )
    }
}

private fun themedContext(context: Context, dark: Boolean): Context {
    val config = Configuration(context.resources.configuration)
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return context.createConfigurationContext(config)
}

private class InlineRendered(val label: String, val size: DpSize, val views: RemoteViews, val themed: Context)

private class InlineHost(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
    }

    fun show(rendered: List<InlineRendered>) {
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
                    bottomMargin = dp(10f)
                }
                clipToOutline = true
            }
            frame.addView(
                item.views.apply(item.themed, frame),
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            addView(frame)
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

private const val QUESTION =
    "decided (round 1 of five): A sample widget is the preview on the launcher's 'Add to home screen' " +
        "sheet and its widget picker. B skeleton, C your data, D annotated and E poster were rejected."
private const val READY = "nothing opened yet; pick 0 or A, then open a sheet"
private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
