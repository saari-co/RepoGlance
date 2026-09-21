# signin-return-027 — proof

GrillTrack decision `signin-return-027` on track `gt-20260728163459-227573`.
Implementation `git:e5ec96365107e03130b9061d3ca5222ed1000eae` on branch
`claude/zen-kowalevski-fed0f6`, fast-forwarded onto `origin/main` at `4f52510`
(PR #29) before the first commit.

## What was confirmed

1. Auto-return + honest instruction: when the token is committed, MainActivity
   starts itself (`CLEAR_TOP|SINGLE_TOP`), clearing the Custom Tab; the code
   screen says RepoGlance comes back on its own and to close the tab if not.
2. Post-token: the checking-splash-018 mark held, no spinner, `Finishing
   sign-in…` then `Loading your repositories…`, then the catalog.
3. While the poll is pending: code screen unchanged.
4. Failure and expiry screens unchanged.
5. No polling change (the proposed `slow_down` fix was withdrawn: the resume
   wake already rechecks GitHub's interval and a guard test pins that).

## Status by claim

| Claim | Status | Evidence |
| --- | --- | --- |
| Self-start is allowed and clears the Custom Tab | **verified (probe)** | `BAL_PROBE.md`: Pixel 11 Pro Fold, Android 17/SDK 37, tab removed from task in 58 ms, 0 BAL blocks |
| Return fires only after commit, once, only if we opened the tab, never when already in front | **verified (guard)** | `SignInReturnGuardTest`, mutation-checked |
| `Finishing sign-in…` covers the real Keystore commit | **verified (guard)** | ordering assertion in `SignInReturnGuardTest` |
| Two post-token screens: mark, messages, no spinner | **verified (device)** | dumps + captures below |
| Rings keep running across the message change | **verified (device)** | screen recording + contact sheets below |
| Plain catalog refresh keeps its spinner | **verified (guard) only** | device path needs a signed-in session; this phone is signed out |
| Code-screen instruction line | **verified (guard) only** | redact guard forbids dumping the code screen, and reaching it starts a sign-in |
| Real end-to-end auto-return during a sign-in | **verified (device, maintainer-driven)** | see End-to-end run |

## Commands and results

### Build gate

```
./gradlew check
BUILD SUCCESSFUL — 265 debug unit tests, 0 failed, 0 errors, 0 skipped
```

Lint/detekt baselines unchanged. No comments added under `app/src/main`. The
first `check` failed detekt `CyclomaticComplexMethod` (15) on the scenario
launcher's `nextIntent`, pushed over by the new `signin-finishing` branch; it
was restructured (holder screens as a lookup table), not baselined.

### Mutation check of the new guards

Moved `deviceAuthorizationCommits.trySend(Unit)` before the commit and deleted
the `RESUMED` early return, then ran `SignInReturnGuardTest`:

```
committedTokenBringsTheAppBackOverTheVerificationTab FAILED
commitSignalFiresOnlyAfterTheTokenIsCommitted FAILED
4 tests completed, 2 failed
```

Source restored; the full `check` above ran on the restored tree.

### Device

The Pixel 11 Pro Fold (`66261FDDJ002J5`) was not attached on 2026-09-21; adb
listed only the Pixel 10 Pro Fold. That phone is on the approved list
(`devices.tsv`) and was pinned explicitly.

| | |
| --- | --- |
| Device | Pixel 10 Pro Fold, `VERIFY_SERIAL=59151FDCG000JA`, inner display, OPENED |
| APK | debug from `e5ec963`, SHA-256 `dde17579894e306befa54476bb35068a2143c2a9bed7bd6c235ef59c3ab3cf9a` (doctor: local = device) |
| RepoGlance session | none (fresh install on this phone) |

RepoGlance was not installed on this phone, and `bin/verify-repoglance`
`doctor`/`launch` exit silently in that case (known; another session owns the
fix). It was installed with `adb -s 59151FDCG000JA install -r`, after which
`doctor` passed.

```
bin/verify-repoglance launch MIXED signin-finishing
bin/verify-repoglance dump signin-finishing
  repoglance:signin-mark    |                            | [921,941][1155,1175]
  repoglance:signin-message | Finishing sign-in…         | [848,1224][1228,1293]
  progress nodes: 0
bin/verify-repoglance capture signin-finishing

adb shell input tap 1038 1258          (advances the holder)
bin/verify-repoglance dump signin-loading
  repoglance:signin-mark    |                            | [922,941][1156,1175]
  repoglance:signin-message | Loading your repositories… | [752,1224][1325,1293]
  progress nodes: 0
bin/verify-repoglance capture signin-loading
```

Continuity: `screenrecord --display-id 4619827677550801152 --time-limit 7`
across the tap, 60 fps, no packet gaps over 250 ms. The message changes at
about 2.9 s. In the 50 ms frames around it, the outer ring keeps fading and
the inner ring keeps growing through the change; a restart would snap one ring
to the lens edge at full alpha and drop the other. From 3.5 s to 7 s every
250 ms frame shows the rings at a different phase. The lighter wash on the
first frames after the tap is the debug holder's click ripple; production has
no clickable surface there.

## End-to-end run (2026-09-21, maintainer-driven)

Bobby signed in on the Pixel 10 Pro Fold running `e5ec963`: Connect GitHub,
Copy code & open GitHub, authorized on GitHub, left the tab alone. He
reported that RepoGlance came back on its own. The agent ran nothing while
the code was on screen and did not use `launch`; afterwards, without
relaunching:

```
adb shell dumpsys activity activities | grep topResumedActivity
  topResumedActivity=ActivityRecord{... co.saari.repoglance/.MainActivity t235}
bin/verify-repoglance dump after-signin
  repoglance:live       |                                    | [588,396][753,513]
  repoglance:rate-limit | GitHub rate limit: 4851 remaining  | [39,542][560,589]
  no "Connect GitHub"
task t235:  * Hist #0: co.saari.repoglance/.MainActivity   (no Custom Tab record)
```

The GitHub tab was cleared from the task, not left behind it, matching the
probe. No capture was taken of the signed-in catalog (it would record
unfiltered repository names).

## Artifacts

Images and video stay in the gitignored run directories on Bobby's MacBook.

| File | SHA-256 |
| --- | --- |
| `runs/verify-repoglance-runs/20260921T121238Z/signin-finishing.png` | `76f37456dc4b1eba57e8ea64a8663ed4bc2d68235aae7dd2e165a09a391dba7a` |
| `runs/verify-repoglance-runs/20260921T121421Z/signin-loading.png` | `c1473cd5a775b3d2a9e91646404972f412a5edb31cb79fab376ce1fc7b2d7014` |
| `runs/signin-return-20260921/signin-continuity.mp4` | `a64bd4588283cc98427babfdb413fafccc4295f477929c63ea0eecfb8b8d356e` |
| `runs/signin-return-20260921/frames/sheet-0-4s.png` | `65f27d3c31021851f120d29e36612806d5a0810e137cac5aacb70f0fe8a32e17` |
| `runs/signin-return-20260921/frames/sheet-change.png` | `4dba6923ff967690dee0a0db958d2dd750a97c97d21fe33317f02575e12f8823` |
| `runs/signin-return-20260921/frames/sheet-after.png` | `94a7dd512c93dae7d49cb33a8fb244268d10904639bc993f1ef43637741ce665` |

No artifact contains a device code, a token, or catalog data.

## Open

- **Other phones and browsers.** The self-start is proven on one OS build with
  Chrome. Elsewhere it may be blocked; the instruction line is the fallback.
- **Tab no longer in front.** If the user leaves the tab for another app before
  the token lands, the self-start may either bring RepoGlance forward over that
  app or be blocked by Android. Neither was tested.
