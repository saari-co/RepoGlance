---
name: verify-repoglance
description: "Drive RepoGlance on the registered Pixel test phone the way a user does and capture proof: launch into a named fixture scenario, walk a mapped feature, dump the UI tree, screenshot the inner display. Use before claiming any RepoGlance UI behavior, to reproduce a bug report, or to verify a PR's visible result."
---

# verify-repoglance

RepoGlance is a read-only Android home-screen widget and issue navigator for
Pixels. This skill is the scripted way to drive the real app and prove what a
user sees. Read `features/README.md` before driving, then the feature file
that matches the claim you need to prove. The map is the maintained
verification source: a proof that drives one convenient entry point is
incomplete when the map lists others.

## Launch

There is no server. Launch means: build the debug APK, install it on the one
registered test phone, and start the app in a named state through the
debug-only scenario launcher.

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon -q assembleDebug
bin/verify-repoglance launch <SCENARIO> <screen> [owner/name] [ISSUES|PRS|BOTH]
```

- `SCENARIO` is one of `EXACT`, `LAST_GOOD`, `UNKNOWN`, `RATE_LIMITED`,
  `NO_CI`, `EMPTY`, `MIXED`. It is stored as the fixture scenario before the
  screen opens.
- `screen` is `live` (the real signed-in catalog, or the sign-in screen when
  there is no session), `navigator` (the fixture navigator for the given
  repository and mode), or `picker` (the GrillTrack widget picker, which
  renders the production compact widget through the real Glance pipeline).
- The launcher is a debug-source-set Activity; a release build does not have
  it and `doctor` fails on such a build.
- Ready means `launch` prints the resumed activity and `dump` shows the
  expected screen text. Widgets never need a home-screen placement for
  proof: the picker renders them.

Teardown is `bin/verify-repoglance cleanup`.

## Doctor

```bash
bin/verify-repoglance doctor
```

Read-only. Pins one transport of the registered phone `66261FDDJ002J5`,
matched by `ro.serialno` so a wireless `ip:port` entry counts (USB
preferred over a wireless-debugging entry for the same phone), and fails
when only other devices are attached. `VERIFY_SERIAL` is the explicit
override and skips that check. It passes only when
the installed APK's SHA-256 equals the local debug build's, the debug
scenario launcher resolves (`cmd package resolve-activity --components`),
and the phone is
awake and unlocked. It also prints the Fold posture (`CLOSED`,
`HALF_OPENED`, `OPENED`) and the run directory every later helper writes
into. Run it first, again after any failed drive, and never drive a device
it did not pass on. A locked or sleeping phone makes every dump show the
keyguard and every capture show the lock screen; that is a doctor failure,
not app evidence.

The registered phone may be attached over USB or wireless debugging. When
`adb devices` is empty, `adb mdns services` lists the phone as
`adb-<serial>-…  _adb-tls-connect._tcp  <ip>:<port>` once it is paired;
`adb connect <ip>:<port>` brings it back, and `doctor` then passes. A serial
that is not the registered one is not a target; `doctor` refuses it unless
`VERIFY_SERIAL` names it on purpose.

## Drive

All control goes through resource ids. `MainActivity` sets
`testTagsAsResourceId`, so every `Modifier.testTag("repoglance:…")` is a
`resource-id` in `uiautomator dump`, and content descriptions are
`content-desc`. Prefer them over coordinates; `tap` resolves ids from the
last dump.

```bash
bin/verify-repoglance dump <label>          # writes <label>.xml and a flat <label>.txt (id | text | desc | bounds)
bin/verify-repoglance tap <resource-id|content-desc|text>
bin/verify-repoglance type "<text>"         # into the focused field
adb shell input -d 0 keyevent BACK          # one reversible back
adb shell cmd device_state state 0|1|2      # Fold posture: CLOSED | HALF_OPENED | OPENED
adb shell cmd connectivity airplane-mode enable|disable
```

Pixel Use (`/pixel-use`, `pixel_observe`, named controls) is the preferred
control plane when it is available in the session; these helpers are the
adb fallback and the canonical evidence path either way.

## Evidence

```bash
bin/verify-repoglance capture <label>       # PNG of the single active physical display + manifest with SHA-256
```

`capture` refuses a sleeping screen and captures whichever physical panel
is active, so a recipe that needs the cover display sets the posture first
(`adb shell cmd device_state state 0`) and names it in its proof.

Standards: `dump` and `capture` stream the UI tree from the device through
`scripts/verify_redact_guard.py` in memory and write a file only after it
passes, so while the GitHub device-code screen is showing nothing is written
on the phone or the host and a live sign-in code can never land in a run
artifact; exercise the real user path
(a scenario launched through the launcher counts; setting preferences by
hand does not); capture the action
and the resulting state, not only the final screen; assert the observable
strings from a `dump`, then screenshot; for the live screen, filter the
catalog to `saari-co/RepoGlance` and prove from the dump that it is the only
repository-shaped label before any capture, and discard any capture that
shows the unfiltered catalog. Evidence lives in
`runs/verify-repoglance-runs/<run-id>/` (ignored by git). Committed proof
packets cite SHA-256s and hold no images.

## Cleanup

```bash
bin/verify-repoglance cleanup
```

Force-stops the app, restores the `MIXED` scenario, and turns airplane mode
off if a drive left it on. It never deletes the run directory: evidence
survives cleanup, and the feature file names where it is.

## Helpers

- `bin/verify-repoglance` — `doctor`, `launch`, `dump`, `tap`, `type`,
  `capture`, `cleanup`; invocation shown above and in each feature file.
- `scripts/verify_redact_guard.py` — reads the streamed UI tree on stdin and
  emits it only when it shows no GitHub device-code screen; `dump` and
  `capture` write nothing until it has passed.
- `scripts/check_feature_map.py` — structural gate run by `./gradlew check`
  (`checkFeatureMap`): index vs files, the four fixed feature headings,
  helpers present and executable, the `.cursor` symlink intact.
- `~/Developer/SaariusSkills/skills/phone-proof/scripts/phone_proof.py` —
  display inventory and capture; `capture` wraps it.

Upkeep: run pstack's `maintain-verification-skill` loop against this
directory. It may edit only this skill; a behaviour the map describes that
the app no longer does is either doc drift (fix here) or a product
regression (report it, never paper over it).
