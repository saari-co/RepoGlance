package co.saari.repoglance.hooks

import android.os.StrictMode

// Debug-only StrictMode (GrillTrack ci-floor-014). Network on the main
// thread kills the process; disk on the main thread is logged, and the
// emulator/device verifier tiers fail on those log lines. See
// docs/INVARIANTS.md ("Main-thread I/O").
object DebugHooks {
    fun install() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectNetwork()
                .penaltyDeathOnNetwork()
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }
}
