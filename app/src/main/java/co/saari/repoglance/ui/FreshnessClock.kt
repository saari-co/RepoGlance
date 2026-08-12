package co.saari.repoglance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.delay

private val SystemFreshnessClock: Clock = Clock.systemUTC()

@Composable
internal fun rememberFreshnessNow(clock: Clock = SystemFreshnessClock): Instant {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember(clock) { mutableStateOf(clock.instant()) }

    LaunchedEffect(lifecycle, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = clock.instant()
                delay(millisUntilNextMinute(now))
            }
        }
    }
    return now
}

internal fun millisUntilNextMinute(now: Instant): Long {
    val remainder = Math.floorMod(now.toEpochMilli(), MILLIS_PER_MINUTE)
    return if (remainder == 0L) MILLIS_PER_MINUTE else MILLIS_PER_MINUTE - remainder
}

private const val MILLIS_PER_MINUTE = 60_000L
