# cold-start-icon-028 — exact-source review

Source identity reviewed: `git:7761a96ea46620b5bd045f888dda953faa06f3bc`
(range `bc2e378..7761a96`). The reviewer was an independent, read-only agent
checking the diff against AGENTS.md, REPO_HYGIENE.md, docs/INVARIANTS.md, the
locked choice, the handoff and PROOF.md.

## Confirmed clean

- The artwork is word for word from the handoff.
- The resource folders resolve correctly for all four light/dark and API
  cases.
- Manifest wiring is correct, and `ic_repoglance_mark` is still used by
  `RepoGlanceMark`.
- Every activity that inherits the app theme calls `enableEdgeToEdge()`, so
  the dark parent theme has no side effect.
- The Material 3 1.3.0 claim is confirmed from its sources.
- The 027 delivery record matches merge commit `7b59139`.
- No binaries or secrets were committed, and the baselines are unchanged.

## Findings and adjudication

| # | Sev | Finding | Classification |
| --- | --- | --- | --- |
| R1 | P2 | On API 31–33, Material 3 draws the app on a computed neutral-variant tone 6 (the `neutral2` family). The `system_neutral1_900` / `_10` fallback is a different family and tone, so a small tone step is likely there. Not verified: no API 31–33 device was run. `system_neutral2_*` may be closer. | defer: API 34+ is proven seamless. API 31–33 is approximate and unverified. Swapping to `neutral2` changes a locked value, so the maintainer decides. |
| R2 | P2 | Recorded as verified with only dark mode on API 36 proven on the device. The light-mode start, themed icon and Quick Settings tile are still pending. | human_gate: the maintainer runs the three checks before delivery. |
| R3 | P3 | The "Checks" section in PROOF.md was out of date: it listed only two asserted values and one mutation. | required_fix: section updated, and the API 34+ values were mutation-checked. |
| R4 | P3 | The guard test did not check each folder's parent theme, so a dark parent in `values-v34` would pass. | required_fix. The reviewer proposed defer and the coordinator raised it because the fix was cheap. `ColdStartIconGuardTest` now asserts the light or dark parent per folder. |
| R5 | P3 | The foreground is centred about 5 dp up and left, and the handle cap goes past the 66 dp safe zone but not the 72 dp visible area. | reject_false_positive: this is the chosen artwork, word for word, picked on the real launcher. |
| R6 | P3 | `candidate_refs` points at a gitignored local image. | reject_false_positive: the repo rule keeps screenshots out of Git. |

## Re-review of the repair (`cd8dbca77dc3a9f316dae4f0756eeef2934da0f7`)

- **R3 and R4:** fixed. The new parent-theme test cannot pass when it should
  not.
- **Leftover "closest resource" line in PROOF.md:** reworded after the
  re-review. The change is to documentation only; the code is the same as
  `cd8dbca`.
- **Open for the maintainer:** the locked `choice` still says "closest
  resource … no flash or tone step". That is true only on API 34+. Rewording
  a lock is the maintainer's call (human_gate).
- **Still pending:** the light-mode, themed-icon and Quick Settings tile
  checks (R2) must be done before delivery.

**Result:** clean at `cd8dbca`.

## R2 closed

The light-mode, themed-icon and Quick Settings tile checks were run on
2026-09-21 after the maintainer changed the settings. All three pass (see
PROOF.md "Maintainer-gated checks"). This is a documentation update after
`cd8dbca`; the source is unchanged.
