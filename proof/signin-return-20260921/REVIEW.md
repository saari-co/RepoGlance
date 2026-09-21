# signin-return-027 — exact-source review

GrillTrack's own review step, run by the implementing agent after
verification. It is advisory. It is **not** the merge rails: CI on the head,
OpenClaw autoreview and a ClawSweeper review of that exact head are still
required before any merge.

- Reviewed: `git:2334325a953c6680e81018f1f820548f0f2bd801` (diff from `4f52510`)
- Repair: `git:84d5b30b6534045a0c95faec7c67ba76463b9f64`
- Clean at: `git:84d5b30b6534045a0c95faec7c67ba76463b9f64`, `./gradlew check` green

## Standards

| Check | Result |
| --- | --- |
| Comment ban in `app/src/main` | pass: no comment lines added |
| Lint/detekt baselines only shrink | pass: unchanged; one complexity finding fixed by restructuring, not baselined |
| `./gradlew check` | pass: 265 tests, 0 failed (and again after the repair) |
| Debug-only tooling stays in the debug source set | pass: holder and launcher entry are in `app/src/debug` |
| Feature map contract | pass: `check_feature_map.py` ok; four H2s kept |
| Truth rule 5 (code visible, Custom Tab, no token exposure) | pass: code screen unchanged while pending; nothing logs code or token |
| Boundaries (no callback, no server) | pass: app-side self-start only |
| Honest `main`: docs describe available and proven behaviour | **finding F1**, repaired |

## Source intent

| Confirmed item | Result |
| --- | --- |
| Auto-return on token commit, clearing the tab | matches; end-to-end verified with the maintainer |
| Honest manual-return instruction | matches; replaced the old "waiting safely" line in the same slot |
| Mark held across both messages, no spinner | matches; one call site, device recording shows continuous rings |
| Code screen unchanged while pending | matches |
| Failure and expiry unchanged | matches; `FailureScreen` untouched |
| No polling change | matches; poller untouched, its guards pass |
| Mark not widened beyond sign-in | matches; `LoadingRepositories` keeps its spinner |
| Probe removed | matches |

## Findings

| ID | Finding | Classification | Outcome |
| --- | --- | --- | --- |
| F1 | README "Auth" and `docs/AUTH_ARCHITECTURE.md` described only the manual return, omitting the new automatic return | `required_fix` | fixed in `84d5b30` |
| F2 | A stale return could stay armed after a cancelled sign-in and fire on a later one | `reject_false_positive` | `connectGitHub` is the only caller of `beginGitHubAuthorization` and disarms first |
| F3 | Setting `Connecting` before the commit could strand the app on "Finishing sign-in…" if the commit returns false | `reject_false_positive` | every generation change (begin, cancel, sign out) cancels the job first on the main thread, so `withContext` throws before `!committed` is read, and the cancelling path's `SignedOut` wins |
| F4 | User leaves the tab for another app before the token lands: RepoGlance may come forward over that app, or be blocked | `defer` | untested; recorded as open risk in `PROOF.md` |
| F5 | Self-start proven only on Android 17 with Chrome Custom Tabs | `defer` | the instruction line is the fallback; recorded as open risk |
