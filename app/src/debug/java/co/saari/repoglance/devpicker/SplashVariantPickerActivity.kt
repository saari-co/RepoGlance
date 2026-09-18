@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)

package co.saari.repoglance.devpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import co.saari.repoglance.ui.theme.RepoGlanceTheme

/**
 * GrillTrack live variant picker for decision `checking-splash-018` —
 * development tooling, debug source set only.
 *
 * Renders the production Checking canvas (the locked message, no spinner, no
 * counts, no sign-in control) with one mark and one motion applied. Two
 * dependent slots are grilled in order: `mark` (which logo) then `motion`
 * (which choreography while checking). Only one slot is active at a time;
 * the other slot's current pick stays visible on the canvas. The chrome
 * states the question for the active slot, names each candidate, describes
 * the active one, offers Replay (re-runs the active motion only) and Reset.
 * Pointer/touch: tap a chip. Keyboard: `1`–`5` jump, DPAD left/right step,
 * Tab switches slot, `R` replays.
 *
 * Manifest: .grilltrack/work/picker/checking-splash-round-1.json. Candidate
 * `A+B` (Commit eye) is the maintainer-requested hybrid of A and B; launched
 * with it, slot 1 shows that single chip as a replacement preview for the
 * five, never as a sixth option.
 *
 * Launch (via the scenario launcher):
 *   bin/verify-repoglance launch MIXED splash-picker "" "" <A..E>[/<A..E>]
 *   adb shell am start -n co.saari.repoglance/.devlaunch.ScenarioLaunchActivity \
 *     --es screen splash-picker --es candidate B/C --es slot motion
 */
class SplashVariantPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val candidate = intent.getStringExtra(EXTRA_CANDIDATE).orEmpty()
        val mark = MarkCandidate.entries.firstOrNull { it.letter.equals(candidate.substringBefore("/"), true) }
            ?: MarkCandidate.COMMIT_LENS
        val motion = MotionCandidate.entries.firstOrNull {
            it.letter.equals(candidate.substringAfter("/", ""), true)
        } ?: MotionCandidate.SWEEP
        val slot = if (intent.getStringExtra(EXTRA_SLOT) == SLOT_MOTION) PickerSlot.MOTION else PickerSlot.MARK
        setContent {
            RepoGlanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Picker(initialMark = mark, initialMotion = motion, initialSlot = slot)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_CANDIDATE = "candidate"
        const val EXTRA_SLOT = "slot"
        const val SLOT_MOTION = "motion"
    }
}

internal enum class PickerSlot(val question: String) {
    MARK(
        "deciding: the RepoGlance MARK. Every candidate has a magnifying glass and is one monochrome shape " +
            "so it can be the status-bar icon (bottom strip). Motion is not being decided in this slot.",
    ),
    MOTION(
        "deciding: the MOTION while the session is being checked. The mark shown is the current slot-1 pick. " +
            "Production will hold still under reduced motion.",
    ),
    ;

    fun other(): PickerSlot = if (this == MARK) MOTION else MARK
}

@Composable
private fun Picker(initialMark: MarkCandidate, initialMotion: MotionCandidate, initialSlot: PickerSlot) {
    var slot by rememberSaveable { mutableStateOf(initialSlot) }
    var mark by rememberSaveable { mutableStateOf(initialMark) }
    var motion by rememberSaveable { mutableStateOf(initialMotion) }
    var replayToken by rememberSaveable { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun pick(index: Int) {
        when (slot) {
            PickerSlot.MARK -> mark = MarkCandidate.entries[index]
            PickerSlot.MOTION -> motion = MotionCandidate.entries[index]
        }
        replayToken += 1
    }

    fun step(delta: Int) {
        val current = if (slot == PickerSlot.MARK) mark.ordinal else motion.ordinal
        pick((current + delta + CANDIDATES) % CANDIDATES)
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
                    digit >= 0 -> pick(digit)
                    event.key == Key.DirectionLeft -> step(-1)
                    event.key == Key.DirectionRight -> step(+1)
                    event.key == Key.Tab -> slot = slot.other()
                    event.key == Key.R -> replayToken += 1
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        PickerChrome(
            slot = slot,
            mark = mark,
            motion = motion,
            onSlot = { slot = it },
            onPick = ::pick,
            onReplay = { replayToken += 1 },
            onReset = {
                slot = PickerSlot.MARK
                mark = resetMark(initialMark)
                motion = MotionCandidate.SWEEP
                replayToken += 1
            },
        )
        key(mark, motion, replayToken) {
            SplashCandidateCanvas(mark = mark, motion = motion)
        }
    }
}

@Composable
private fun PickerChrome(
    slot: PickerSlot,
    mark: MarkCandidate,
    motion: MotionCandidate,
    onSlot: (PickerSlot) -> Unit,
    onPick: (Int) -> Unit,
    onReplay: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(
            "GrillTrack checking-splash-018 · ${slot.question}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            PickerSlot.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == slot,
                    onClick = { onSlot(candidate) },
                    label = { Text("slot ${candidate.ordinal + 1}: ${candidate.name.lowercase()}") },
                    modifier = Modifier.testTag("repoglance:picker-slot-${candidate.name.lowercase()}"),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            AssistChip(
                onClick = onReplay,
                label = { Text("Replay") },
                modifier = Modifier.testTag("repoglance:picker-replay"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            AssistChip(
                onClick = onReset,
                label = { Text("Reset") },
                modifier = Modifier.testTag("repoglance:picker-reset"),
            )
        }
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            val labels = when (slot) {
                PickerSlot.MARK ->
                    MarkCandidate.entries.filter { it.isHybrid == mark.isHybrid }.map { it.letter to it.label }
                PickerSlot.MOTION -> MotionCandidate.entries.map { it.letter to it.label }
            }
            val activeLetter = if (slot == PickerSlot.MARK) mark.letter else motion.letter
            labels.forEachIndexed { index, (letter, label) ->
                FilterChip(
                    selected = letter == activeLetter,
                    onClick = { if (!mark.isHybrid || slot == PickerSlot.MOTION) onPick(index) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("repoglance:picker-candidate-$letter"),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
        val active = when (slot) {
            PickerSlot.MARK -> "mark ${mark.letter}: ${mark.description}"
            PickerSlot.MOTION -> "motion ${motion.letter}: ${motion.description}"
        }
        Text(
            active,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp).testTag("repoglance:picker-active"),
        )
        Text(
            "On canvas: mark ${mark.letter} ${mark.shortName} · motion ${motion.letter} ${motion.shortName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).testTag("repoglance:picker-trace"),
        )
    }
}

private fun resetMark(initial: MarkCandidate): MarkCandidate =
    if (initial.isHybrid) initial else MarkCandidate.COMMIT_LENS

private const val CANDIDATES = 5
private val DIGIT_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five)
