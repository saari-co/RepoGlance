# Decision map: sample mode for reviewer access (2026-09-29)

Destination: someone without a GitHub account can explore every RepoGlance
surface, including the widgets, using clearly labelled sample data. This lets
Google Play reviewers review the app (`reviewer-access-036`, #7) and lets store
screenshots come from sample data instead of the maintainer's repositories.

Facts (source-linked, main 432be33df55d14c5f0d58f10c5e3fa35fc94d118):

- A release build has no way into fixture mode. `FixtureRoot` is selected
  only by `EXTRA_REPO_FULL` (`MainActivity.kt:80,192-220`), which only
  debug code sends.
- The fixture home and navigator are an older UI than the signed-in catalog.
  `LiveRepoGlanceScreen` renders from `LiveUiState.Ready(catalog)` plus
  content state (`RepoGlanceViewModel.kt:50,154-190`), so it can be fed
  sample data.
- Widgets read only live stores (`RepoWidget.kt:107-120`,
  `StackWidget.kt:90-103`). `widget-live-config-022` and
  `stack-widget-live-024` lock "no fixture rows on live widgets".
- The fixture corpus mixes the real repos `saari-co/RepoGlance` and
  `dinkuskit/blocks` with fictional ones (`Fixtures.kt:24-28,68-246`;
  `docs/INVARIANTS.md:192-195`).
- Fixture rows open real GitHub URLs (`NavigatorScreen.kt:146-156`) that
  mostly do not exist.
- An unconfigured repo widget still says `FIXTURE PREVIEW`
  (`RepoWidget.kt:156-177`), including for signed-in users.

Locked behaviour (`sample-mode-037`, superseded by `sample-mode-042`, maintainer 2026-09-29):

- Sample mode uses the real signed-in screens (catalog, repo view,
  navigator), fed sample data.
- Widgets work in sample mode and are marked as sample. Signed-in widgets
  never show sample rows.
- Sample data uses fictional owners only. **Superseded 2026-09-29 by
  `sample-mode-042`:** owners are the maintainer's own accounts (`saari-co`,
  `dinkuskit`, viewer `saariuslystoned`) under repository names that do not
  exist on GitHub, after ClawSweeper flagged `acme`, `octoco` and `octodev` as
  real third-party accounts. Ledger note: `sample-app-038` still lists
  `sample-mode-037` as its dependency (the ledger tool cannot re-point it),
  so it is re-verified under `sample-mode-042` by hand; see the proof.
- Sample mode persists across restarts until the user chooses to sign in.
  A sign-in action is always visible while in sample mode.
- Default: the detail view of a sample issue or PR replaces "Open on GitHub"
  with a note that it is a sample item.

Frontier nodes (one per GrillTrack cycle):

1. `sample-app-038`: the in-app sample mode. It covers the "Explore with
   sample data" entry on the sign-in screen, the fictional sample corpus
   behind the real screens, persistence, and the sign-in exit. It also
   covers the sample-item note and a provisional sample marker that reuses
   the existing `LIVE` chip pattern. **Verified, review clean
   (`.grilltrack/proof/sample-app-038-verify-20260929.md`).**
2. `sample-widgets-039`: repo and stack widgets and the Quick Settings tile
   in sample mode, including sample pins and widget config from the sample
   catalog. Sample widgets switch to live or empty on sign-in, and background
   refresh never runs for sample. It also removes the stale `FIXTURE PREVIEW`
   label. **Current frontier.**
3. `sample-marker-040`: the look of the sample marker across app, widgets
   and tile. This is a round of five on the real sample screens.
4. `play-app-access-041`: the Play Console App access text and store
   screenshots from sample mode. This is human-gated (Console).

Dependencies: 2 needs 1. 3 needs 1 and 2, so the canvas is real. 4 needs 1–3.
