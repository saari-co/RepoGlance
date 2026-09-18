@file:OptIn(ExperimentalComposeUiApi::class)

package co.saari.repoglance.devpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import co.saari.repoglance.model.NavigatorMode
import co.saari.repoglance.model.NavigatorScope
import co.saari.repoglance.state.AppPrefs
import co.saari.repoglance.state.NavigatorScopeCodec
import co.saari.repoglance.ui.theme.RepoGlanceTheme
import co.saari.repoglance.widget.EXTRA_NAVIGATOR_MODE
import co.saari.repoglance.widget.EXTRA_REPO_FULL
import co.saari.repoglance.widget.navigatorModeFromExtra

/**
 * GrillTrack live variant picker for decision `navigator-detail-017` —
 * development tooling, debug source set only.
 *
 * Hosts the real navigator canvas with one of five wide-layout selection
 * behaviours ([WideSelectionCandidate]) active. The chrome states the question
 * being decided, what a tap will do under the active candidate, and a live
 * trace of the last action, because the candidates differ in behaviour and
 * stills of behaviours look alike. Pointer/touch: tap a chip; Reset clears
 * the selection. Keyboard: `1`–`5` jump to a candidate, DPAD left/right step.
 *
 * Manifest: .grilltrack/work/picker/navigator-detail-round-1.json. Candidate
 * `C+sheet` is the hybrid preview that replaced the five after round 1 and
 * became the production behaviour (proof/navigator-detail-20260918).
 *
 * Launch (via the scenario launcher):
 *   bin/verify-repoglance launch MIXED navigator-picker acme/rocket BOTH
 *   adb shell am start -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity \
 *     --es screen navigator-picker --es repo acme/rocket --es mode BOTH --es candidate C
 */
class NavigatorVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scope = intent.getStringExtra(EXTRA_REPO_FULL)
            ?.let { NavigatorScopeCodec.decode("REPO", it) }
            ?: NavigatorScope.Account
        val mode = navigatorModeFromExtra(intent.getStringExtra(EXTRA_NAVIGATOR_MODE))
        val hybrid = intent.getBooleanExtra(EXTRA_HYBRID, false)
        val letter = intent.getStringExtra(EXTRA_CANDIDATE)?.substringBefore("+")
        val initial = WideSelectionCandidate.entries.firstOrNull { it.letter.equals(letter, ignoreCase = true) }
            ?: WideSelectionCandidate.PANE_FILL
        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Picker(initial = initial, scope = scope, mode = mode, hybrid = hybrid, onFinish = ::finish)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
        const val EXTRA_HYBRID = "hybrid"
    }
}

@Composable
private fun Picker(
    initial: WideSelectionCandidate,
    scope: NavigatorScope,
    mode: NavigatorMode,
    hybrid: Boolean,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scenario = AppPrefs.rememberScenario(context)
    var active by rememberSaveable { mutableStateOf(initial) }
    var resetToken by rememberSaveable { mutableIntStateOf(0) }
    var trace by rememberSaveable { mutableStateOf("nothing yet") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun activate(candidate: WideSelectionCandidate) {
        active = candidate
        trace = "nothing yet"
    }

    fun step(delta: Int) {
        val all = WideSelectionCandidate.entries
        activate(all[(active.ordinal + delta + all.size) % all.size])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = DIGIT_KEYS.indexOf(event.key)
                when {
                    digit >= 0 -> activate(WideSelectionCandidate.entries[digit])
                    event.key == Key.DirectionLeft -> step(-1)
                    event.key == Key.DirectionRight -> step(+1)
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        PickerChrome(
            active = active,
            hybrid = hybrid,
            trace = trace,
            onActivate = ::activate,
            onReset = {
                resetToken += 1
                trace = "reset"
            },
        )
        key(active, resetToken) {
            NavigatorCandidate(
                candidate = active,
                scenario = scenario.value,
                initialScope = scope,
                initialMode = mode,
                onBackToHome = onFinish,
                onTrace = { trace = it },
                narrowSheet = hybrid,
            )
        }
    }
}

@Composable
private fun PickerChrome(
    active: WideSelectionCandidate,
    hybrid: Boolean,
    trace: String,
    onActivate: (WideSelectionCandidate) -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(
            "GrillTrack navigator-detail-017 \u00b7 deciding: what a row TAP does on this wide layout. " +
                "Long-press on a row always opens GitHub; that is not being decided.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            val chips = if (hybrid) listOf(active) else WideSelectionCandidate.entries
            chips.forEach { candidate ->
                FilterChip(
                    selected = candidate == active,
                    onClick = { onActivate(candidate) },
                    label = { Text(candidate.label) },
                    modifier = Modifier.testTag("repoglance:picker-candidate-${candidate.letter}"),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            AssistChip(
                onClick = onReset,
                label = { Text("Reset") },
                modifier = Modifier.testTag("repoglance:picker-reset"),
            )
        }
        Text(
            "${active.letter}: tap a row \u2192 ${active.tapEffect}. GitHub via ${active.gitHubPath}." +
                if (hybrid) " HYBRID PREVIEW, folded: tap a row \u2192 raises a detail sheet over the list." else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp).testTag("repoglance:picker-active"),
        )
        Text(
            "Last action: $trace",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)
