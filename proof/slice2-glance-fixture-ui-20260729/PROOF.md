# Slice 2 Proof — Glance Widgets, Fixture Mode, Device Screenshots

Date: 2026-07-29

Repo: `saari-co/RepoGlance` (public), branch `codex/slice2-glance-fixture-ui-20260729`
PR: #4 (stacked on #2 / `codex/slice1-model-fixtures-20260729` @ `47a7b64`)
Implementation head this proof binds to: `c9294825fefbb6d914b85624d951d9c77bca63dd`

## Task

Slice 2 of the RepoBar Pixel companion plan (x-api#522), routed by the
maintainer's build-lane goal: fixture-first Jetpack Glance widgets (per-repo
+ stack), an in-app fixture mode with in-place pinning (`pin-model-010`),
navigator (scope x mode x filters), instant cached search, deep-link stubs,
dynamic color light+dark. No network, no auth.

## Verification

- CI green on exactly `c9294825`:
  [run 30457064602](https://github.com/saari-co/RepoGlance/actions/runs/30457064602)
  — `testDebugUnitTest` (83 tests, Slice 1's 62 all still passing) +
  `assembleDebug`; debug APK exported as the `repoglance-debug-apk` artifact.
- Device proof on the registered **Pixel 10 Pro XL** test device
  (`63310DLCQ000RV`, Android 16 `CP1A.260505.005`), APK from the bound head's
  CI artifact: `versionCode 2`, `versionName 0.2.0-slice2`, streamed install
  success. Device dark-mode setting was toggled for light frames and restored
  to its original value (`night: yes`) afterwards.
- Every frame below was reviewed by the orchestrating agent before
  publication (house media policy).

## Media

Screenshots are release assets (never in product-repo git):
<https://github.com/saari-co/swarm-pr-assets/releases/tag/RepoGlance-pr-4-c9294825fefb>

| asset | bytes | sha256 | shows |
|---|---|---|---|
| 01-home-mixed-light.png | 204149 | `32414df2ad4c6e85b78a6061b20550900f3457ee2c89b9843bb7c3485bf2985a` | Home, MIXED, light: exact ("Updated just now"), last_good ("Cached · 45m" chip), unknown ("—" counts, "Updated: unknown"), rate-limited (banner + "Cached · 2h"); all unpinned |
| 02-pin-toggle-before.png | 203814 | `6a5eda5ddafa062ad59da9ca4c25cf61952d440499e73b5eae3978d2bf20656b` | Pin toggle before: star outlines on every row |
| 03-pin-toggle-after.png | 202613 | `4a3e7830a935a1772dfed77f3446870b470f1c03cbab962ca279b1f5be7b7382` | Pin toggle after: pinned rows filled-star and sorted above unpinned (all three same-named rows pinned together — finding 1) |
| 04-scenario-switcher-open.png | 221300 | `0ca7c32d5fe34fe72ee9dcf128e82dd493cd9c2d89a5b8232e7aec983611d2ae` | Fixture switcher open: all seven scenarios |
| 05-last-good.png | 137496 | `a68782f1319e966d9898030153870cc684137a0e305c7c99513303bfb6a72513` | LAST_GOOD: age chips "Cached · 45m" / "Cached · 3h"; "Rate limit low" banner (LOW bucket) on second repo |
| 06-rate-limited.png | 185947 | `969be685c855d3e09416d350925d8bf4fa873efd67a80e8a364680dcd85dec75` | RATE_LIMITED: "Rate-limited — backing off" banner first-class on every card, counts still cached-labelled |
| 07-unknown.png | 115024 | `719678a89fc5a0e9121f1a15626683e886890534da6989f256bb9b7de8569f1b` | UNKNOWN: "—" counts (never zero), "Last push: unknown", "Updated: unknown" |
| 08-no-ci.png | 115864 | `78c05e63a407d5fee87176843ac41ee03c6414bd2fd80bba00294ae16c5899f4` | NO_CI: explicit "No CI" neutral label, distinct from Passing; exact-basis honest zeros |
| 09-empty.png | 61807 | `3eaacff992a1c8dfb903a7dd3269ed3e1a79f0c2f271c9c5d71029737c430a48` | EMPTY: "No repositories in this fixture" (no spinner) |
| 10-navigator-issues.png | 234995 | `1cd5d27b05fc5bd2c19385c94024588623e4dd9c8e623cb1471e0df939a1e864` | Navigator, Issues/Open: rows #100–#106 with state/labels/author/comments/per-row updated age |
| 11-navigator-search.png | 153632 | `fd0de69c3ea376e8c3e4f2879027af05995d15cc91eeca89650925b7cbcb826c` | "Search cached rows" with query "ci": instant filter to one row, keyboard up |
| 12-navigator-scope-open.png | 232620 | `31c4268b20b42c65ae12e9bfee782a1b7ce56ac4a5952c1708ee78c801c1044b` | Scope dropdown: Account / Org: saari-co / Org: acme / per-repo entries (duplicates — finding 1) |
| 13-navigator-prs-awaiting.png | 259371 | `b45afa2dc75efcdd373c98ee7c9a5c1c6e65bdf7e51378f84f595146df1c463c` | PRs + AWAITING_MY_REVIEW: PR-only rows, Draft badges, Review: REVIEW_REQUIRED, CI rollups P/F/R/N/U |
| 14-navigator-detail.png | 140014 | `8745a0f622864e6bfdb3f06e875170e61a7ba7731ca54b754895d2932793b4df` | Detail (narrow single-pane): #201 title/labels/author/assignee/comments/updated/draft/review/CI + "Open on GitHub" |
| 15-widgets-home-light.png | 1723051 | `e230559e8b4498b0588d1b0a1e6d8ae03fa341b3a11ab7988aff15364bb63de4` | Launcher, light: per-repo widget SMALL (CI + PRs + "Updated just now") and stack widget (MIXED header with data-age, pinned rows, amber stale ages) |
| 16-widget-rate-limited.png | 1697963 | `6b09b6020b264856c55c26353c6096971f9562be4b3245139c944a34003bdfd5` | RATE_LIMITED widgets: stack header "RATE_LIMITED · Updated 2h ago", pinned row with 2h age; per-repo SMALL shows age but no rate-limit marker (finding 2) |
| 17-widget-last-good.png | 1701525 | `77b4bb5b1affc7ca46be75a3425a7b0afc35c1f0a02d0d881e7a661205e40000` | LAST_GOOD widgets: stack "LAST_GOOD · Updated 45m ago" + amber 45m row age; per-repo "Updated 45m ago" |
| 18-widgets-home-dark.png | 1695600 | `8458fbdf0b89cef3cd84b2a49aedc3fc3875ca5bc8646b8ab2bc318b83b66a0a` | Launcher, dark: both widgets re-themed via GlanceTheme dynamic color |
| 19-home-mixed-dark.png | 198759 | `5c4132e235e40c7eacc45c1f9544e252a3eb2676bbbbad95b9af1223749518ab` | App home, dark: MIXED cards incl. dark-variant rate-limit banner, chips, ages |
| 20-navigator-dark.png | 239398 | `92b39e89e3e6c92494a912a15d7f8b33926323533f756d4e0c9e630bb904c1f4` | Navigator, dark: Issues/Open list, filters row |
| device-facts.txt | 306 | `9bc042db8c2cd09c5076840c51bc9acaee4783f13b9ab2a0e237fbc8844eb1b3` | `pm`/`dumpsys`/`getprop` install + build-fingerprint facts |

## Findings (honest, none blocking; owner routing noted)

1. **Fixture identity collision.** MIXED reuses `saari-co/RepoGlance` for
   three value-basis variants (and `acme/rocket` twice). Pinning is keyed by
   repo identity — correct per `pin-model-010` — so one tap stars the whole
   triplet (frame 03) and the navigator scope dropdown lists duplicates
   (frame 12). Fix belongs in the fixture corpus (diversify names), not the
   pin model. Follow-up slice.
2. **SMALL per-repo widget carries no rate-limit indicator** (frame 16); the
   WIDE layout renders the banner line. Widget content priority is exactly
   the deferred five-variant grill (`widget-content-priority-008`) — recorded
   there, not patched ad hoc here.
3. **Cosmetic:** the scenario dropdown wraps long names ("RATE_LIMITED" →
   two lines, frame 06); the navigator's floating scope box crowds the
   status-bar inset on the XL (frames 10–14).
4. **Capture-lane incidents (device handling, not app defects):** two
   mis-taps into the status-bar gesture strip briefly foregrounded unrelated
   system/app surfaces; one stray screenshot was deleted on-device before any
   sync, an accessibility dump was deleted unread, and no other-app content
   was retained. Root cause (taps must land below y≈162 on this device) is
   recorded in the swarm's device-access memory.

## Gates Honored

- Fixtures only: no network stack, zero manifest permissions, deep links are
  `ACTION_VIEW` on `GitHubLinks`-built `https://github.com/...` URLs only.
- Registered test device only (XL). The Fold is the maintainer's off-cable
  carry phone: Fold installs/screenshots ship as a WAITING_FOR_HUMAN packet
  in the delivery-rail slice, never improvised.
- No screenshots in product-repo git; assets live in the private
  `swarm-pr-assets` release above; per-frame review done before upload.
- No secrets, keystores, or `local.properties`; merges remain the
  maintainer's gate.

## Next Route

PR C (delivery rail): rolling `canary` prerelease on push to main (draft
PR #3), SwarmPocket companion-rail generalization (swarm-side, stacked on
swarmpocket#28), cp-1 deployment + Fold install as WAITING_FOR_HUMAN packets,
then the one-time rail e2e proof.
