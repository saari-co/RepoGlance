@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)

package co.saari.repoglance.devpicker

import android.os.Bundle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import co.saari.repoglance.fixtures.FixtureScenario
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.NavigatorScreen
import co.saari.repoglance.ui.theme.LocalLabelType
import co.saari.repoglance.ui.theme.RepoGlanceTheme

/**
 * GrillTrack live variant picker for the `family-look` grill, round 2, slot
 * `label-typography` — development tooling, debug source set only.
 *
 * Renders the production fixture catalog (HomeScreen) or the production
 * navigator in the launched scenario with one [LabelTypeCandidate] provided
 * through LocalLabelType. Everything else is production: dynamic colour, the
 * locked E family-tonal status pills, the mark, the cards, the copy, and M3
 * body text. Chrome states the question, names the five, describes the
 * active one, and offers canvas, light/dark, collapse and Reset.
 * Pointer/touch: tap a chip. Keyboard: `1`-`5` jump, DPAD left/right step,
 * `D` dark, `N` catalog/navigator, `H` details, `R` reset.
 *
 * Manifest: .grilltrack/work/picker/family-look-round-2.json.
 *
 * Launch: bin/verify-repoglance launch EXACT type-picker "" "" <A..E>
 */
class LabelTypeVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = LabelTypeCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: LabelTypeCandidate.M3_DEFAULT
        val scenario = intent.getStringExtra(EXTRA_SCENARIO)
            ?.let { name -> FixtureScenario.entries.firstOrNull { it.name == name } }
            ?: FixtureScenario.EXACT
        setContent { Picker(initial = initial, initialScenario = scenario) }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
        const val EXTRA_SCENARIO = "scenario"
    }
}

@Composable
private fun Picker(initial: LabelTypeCandidate, initialScenario: FixtureScenario) {
    val systemDark = isSystemInDarkTheme()
    var candidate by rememberSaveable { mutableStateOf(initial) }
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var scenario by rememberSaveable { mutableStateOf(initialScenario) }
    var navigator by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun pick(index: Int) {
        candidate = LabelTypeCandidate.entries[index]
    }

    fun step(delta: Int) = pick((candidate.ordinal + delta + CANDIDATES) % CANDIDATES)

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
                CompositionLocalProvider(LocalLabelType provides candidate.type) {
                    if (navigator) {
                        NavigatorScreen(
                            scenario = scenario,
                            initialScope = NavigatorScope.Account,
                            onBackToHome = { navigator = false },
                        )
                    } else {
                        HomeScreen(
                            scenario = scenario,
                            onScenarioChange = { scenario = it },
                            pinnedRepos = emptySet(),
                            onTogglePin = {},
                            onOpenNavigator = { navigator = true },
                            onOpenRepo = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerChrome(
    candidate: LabelTypeCandidate,
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
            LabelTypeCandidate.entries.forEachIndexed { index, entry ->
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
                label = { Text(if (navigator) "navigator" else "catalog") },
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
            "On canvas: labels ${candidate.letter} ${candidate.shortName} · " +
                "${if (navigator) "navigator" else "catalog"} · " +
                "${if (dark) "dark" else "light"} · status E tonal, mark, dynamic colour, M3 body locked",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private const val QUESTION =
    "deciding: how RepoGlance sets its LABELS (section heads, chips and status pills, age and rate-limit " +
        "lines, button labels) so it reads as the same crew as Swarm Intercom. Body text, colour, status " +
        "pills' colour and the mark are not being decided."
private const val SHORT_QUESTION = "deciding: LABEL TYPOGRAPHY (heads / chips / ages / buttons)"
private const val CANDIDATES = 5
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)
