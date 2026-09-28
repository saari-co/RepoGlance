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
import co.saari.repoglance.ui.HomeScreen
import co.saari.repoglance.ui.theme.RepoGlanceTheme

/**
 * GrillTrack live variant picker for the `family-look` grill, slot
 * `status-colour` — development tooling, debug source set only.
 *
 * Renders the production fixture catalog (HomeScreen) in the launched fixture
 * scenario with one status palette applied. EXACT shows passing, failing and
 * running in its first three cards; MIXED adds last-good, unknown, an
 * exhausted rate-limit banner and No CI. The chrome collapses to one line so the cards
 * stay in view. Everything else on the canvas is production: dynamic colour, the
 * locked mark, the cards, the copy. The chrome states the question, names each
 * candidate, describes the active one, and offers a light/dark preview toggle
 * so both modes can be judged without touching the phone's settings.
 * Pointer/touch: tap a chip. Keyboard: `1`–`5` jump, DPAD left/right step,
 * `D` toggles dark, `H` hides or shows the chrome details, `R` resets.
 *
 * Manifest: .grilltrack/work/picker/family-look-round-1.json.
 *
 * Launch (via the scenario launcher):
 *   bin/verify-repoglance launch EXACT status-picker "" "" <A..E>
 *   adb shell am start -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity \
 *     --es scenario EXACT --es screen status-picker --es candidate C
 */
class StatusColourVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val letter = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val initial = StatusColourCandidate.entries.firstOrNull { it.letter.equals(letter, true) }
            ?: StatusColourCandidate.DYNAMIC_ROLES
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
private fun Picker(initial: StatusColourCandidate, initialScenario: FixtureScenario) {
    val systemDark = isSystemInDarkTheme()
    var candidate by rememberSaveable { mutableStateOf(initial) }
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var scenario by rememberSaveable { mutableStateOf(initialScenario) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun pick(index: Int) {
        candidate = StatusColourCandidate.entries[index]
    }

    fun step(delta: Int) = pick((candidate.ordinal + delta + CANDIDATES) % CANDIDATES)

    RepoGlanceTheme(darkTheme = dark) {
        val palette = candidate.palette(MaterialTheme.colorScheme, dark)
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
                    collapsed = collapsed,
                    onPick = ::pick,
                    onToggleDark = { dark = !dark },
                    onToggleDetails = { collapsed = !collapsed },
                    onReset = {
                        candidate = initial
                        dark = systemDark
                    },
                )
                HomeScreen(
                    scenario = scenario,
                    onScenarioChange = { scenario = it },
                    pinnedRepos = emptySet(),
                    onTogglePin = {},
                    onOpenNavigator = {},
                    onOpenRepo = {},
                    statusColors = palette,
                )
            }
        }
    }
}

@Composable
private fun PickerChrome(
    candidate: StatusColourCandidate,
    dark: Boolean,
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
            StatusColourCandidate.entries.forEachIndexed { index, entry ->
                FilterChip(
                    selected = entry == candidate,
                    onClick = { onPick(index) },
                    label = { Text(entry.label) },
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
            "On canvas: status ${candidate.letter} ${candidate.shortName} · ${if (dark) "dark" else "light"} · " +
                "dynamic colour and the locked mark unchanged",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private const val QUESTION =
    "deciding: how RepoGlance COLOURS STATUS meanings (ok / working / failing / neutral) across the app, " +
        "so it shares a family with Swarm Intercom on a dynamic-colour base. Cards, copy and the mark are not " +
        "being decided."
private const val SHORT_QUESTION = "deciding: STATUS COLOUR (ok / working / failing / neutral)"
private const val CANDIDATES = 5
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)
