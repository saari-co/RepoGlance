@file:OptIn(ExperimentalLayoutApi::class)

package co.saari.repoglance.devpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.RepoRef
import co.saari.repoglance.ui.settings.LocalWidgetsLook
import co.saari.repoglance.ui.settings.PlacedWidget
import co.saari.repoglance.ui.settings.WidgetKind
import co.saari.repoglance.ui.settings.WidgetsContent
import co.saari.repoglance.ui.settings.WidgetsUiState
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.RepoWidgetConfig

/**
 * GrillTrack live variant picker for `widgets-look-047` — development tooling,
 * debug source set only.
 *
 * Canvas: the production [WidgetsContent] (the screen behind the menu's
 * "Widgets" item) at the phone's own width, with one [WidgetsLookCandidate]
 * provided through LocalWidgetsLook. The placed-widget rows are example rows
 * (a set-up repository widget, one not set up, a Pinned repos widget with
 * three pins) so every row state is on the canvas; "nothing placed" and
 * "launcher can't pin" toggle the other two states.
 * The picker never pins or opens a setup: taps are written to the trace line.
 * Pointer/touch: tap a chip. Keyboard: `1`-`5` jump, DPAD left/right step,
 * `D` dark, `S` sample, `P` pin support, `E` empty, `H` details, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/widgets-look-round-1.json.
 *
 * Launch: bin/verify-repoglance launch MIXED widgets-look-picker "" "" <A..E>
 */
class WidgetsLookPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = WidgetsLookCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: WidgetsLookCandidate.CARDS
        setContent { Picker(initial) }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
    }
}

private class PickerState(private val initial: WidgetsLookCandidate, private val systemDark: Boolean) {
    var candidate by mutableStateOf(initial)
    var dark by mutableStateOf(systemDark)
    var sample by mutableStateOf(false)
    var pinSupported by mutableStateOf(true)
    var empty by mutableStateOf(false)
    var collapsed by mutableStateOf(false)
    var trace by mutableStateOf(START_TRACE)

    fun pick(index: Int) {
        WidgetsLookCandidate.entries.getOrNull(index)?.let {
            candidate = it
            trace = "showing ${it.letter} ${it.shortName}"
        }
    }

    fun step(delta: Int) {
        val size = WidgetsLookCandidate.entries.size
        pick((WidgetsLookCandidate.entries.indexOf(candidate) + delta + size) % size)
    }

    fun reset() {
        candidate = initial
        dark = systemDark
        sample = false
        pinSupported = true
        empty = false
        trace = START_TRACE
    }

    fun onKey(key: Key): Boolean {
        val digit = DIGIT_KEYS.indexOf(key)
        when {
            digit >= 0 -> pick(digit)
            key == Key.DirectionLeft -> step(-1)
            key == Key.DirectionRight -> step(+1)
            else -> return onToggleKey(key)
        }
        return true
    }

    private fun onToggleKey(key: Key): Boolean {
        when (key) {
            Key.D -> dark = !dark
            Key.S -> sample = !sample
            Key.P -> pinSupported = !pinSupported
            Key.E -> empty = !empty
            Key.H -> collapsed = !collapsed
            Key.R -> reset()
            else -> return false
        }
        return true
    }

    fun uiState() = WidgetsUiState(
        pinSupported = pinSupported,
        placed = if (empty) emptyList() else examplePlaced(sample),
        sample = sample,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Picker(initial: WidgetsLookCandidate) {
    val systemDark = isSystemInDarkTheme()
    val picker = remember { PickerState(initial, systemDark) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    RepoGlanceTheme(darkTheme = picker.dark) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event -> event.type == KeyEventType.KeyDown && picker.onKey(event.key) },
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PickerChrome(picker)
                HorizontalDivider()
                Box(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWidgetsLook provides picker.candidate.look) {
                        WidgetsContent(
                            state = picker.uiState(),
                            onBack = { picker.trace = BACK_TRACE },
                            onAdd = { kind -> picker.trace = addTrace(kind) },
                            onOpenSetup = { id -> picker.trace = "placed widget #$id → $SETUP_TRACE" },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerChrome(picker: PickerState) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(
            "GrillTrack widgets-look-047 · " + if (picker.collapsed) SHORT_QUESTION else QUESTION,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("repoglance:picker-question"),
        )
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WidgetsLookCandidate.entries.forEachIndexed { index, entry ->
                FilterChip(
                    selected = entry == picker.candidate,
                    onClick = { picker.pick(index) },
                    label = { Text("${entry.letter} ${entry.shortName}") },
                    modifier = Modifier.testTag("repoglance:picker-candidate-${entry.letter}"),
                )
            }
            if (!picker.collapsed) {
                Toggle("dark", picker.dark, "repoglance:picker-dark") { picker.dark = !picker.dark }
                Toggle("sample mode", picker.sample, "repoglance:picker-sample") { picker.sample = !picker.sample }
                Toggle("launcher can't pin", !picker.pinSupported, "repoglance:picker-no-pin") {
                    picker.pinSupported = !picker.pinSupported
                }
                Toggle("nothing placed", picker.empty, "repoglance:picker-empty") { picker.empty = !picker.empty }
            }
            AssistChip(
                onClick = { picker.collapsed = !picker.collapsed },
                label = { Text(if (picker.collapsed) "Details" else "Hide details") },
                modifier = Modifier.testTag("repoglance:picker-collapse"),
            )
            AssistChip(
                onClick = picker::reset,
                label = { Text("Reset") },
                modifier = Modifier.testTag("repoglance:picker-reset"),
            )
        }
        if (!picker.collapsed) {
            Text(
                "${picker.candidate.letter}: ${picker.candidate.description}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp).testTag("repoglance:picker-active"),
            )
        }
        Text(
            "Last tap: ${picker.trace}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp).testTag("repoglance:picker-trace"),
        )
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.testTag(tag),
    )
}

private fun addTrace(kind: WidgetKind): String = when (kind) {
    WidgetKind.REPOSITORY -> "Add ${kind.title} → $ADD_TRACE, then its setup (not run here)"
    WidgetKind.PINNED_REPOS -> "Add ${kind.title} → $ADD_TRACE (not run here)"
}

private fun examplePlaced(sample: Boolean): List<PlacedWidget> {
    val repo = if (sample) RepoRef("saari-co", "rocket") else RepoRef("saari-co", "RepoGlance")
    return listOf(
        PlacedWidget.Repository(EXAMPLE_REPO_ID, RepoWidgetConfig(repo, NavigatorMode.BOTH)),
        PlacedWidget.Repository(EXAMPLE_UNCONFIGURED_ID, null),
        PlacedWidget.PinnedRepos(EXAMPLE_PINNED_ID, if (sample) 1 else 3),
    )
}

private const val EXAMPLE_REPO_ID = 12
private const val EXAMPLE_UNCONFIGURED_ID = 14
private const val EXAMPLE_PINNED_ID = 13
private const val START_TRACE = "none yet · tap a chip, then Add or a placed widget"
private const val ADD_TRACE = "the launcher's 'Add to home screen' dialog"
private const val BACK_TRACE = "back → returns to the catalog or Settings, wherever Widgets was opened"
private const val SETUP_TRACE = "its setup screen, to change the repository (not run here)"
private const val QUESTION =
    "deciding: the LOOK of the Widgets screen (menu → Widgets) — how the two Add entries and the " +
        "'On your home screen' list are laid out. Behaviour is fixed: Add asks the launcher, a repository " +
        "widget row opens its setup."
private const val SHORT_QUESTION = "deciding: the look of the Widgets screen"
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)
