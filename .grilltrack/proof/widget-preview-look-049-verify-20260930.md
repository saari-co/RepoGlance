# Widget previews — round and device proof (2026-09-30)

GrillTrack `widget-previews-048` (surfaces and Glance upgrade) and
`widget-preview-look-049` (what a preview shows). Branch
`claude/widget-previews`, implementation commit `54b95f6`. Images stay in
ignored `runs/verify-repoglance-runs/<run>/`; this file holds their SHA-256s.

## Decisions

- **048 (AskUserQuestion, 2026-09-30):** both surfaces get a preview (the in-app
  Add sheet and the launcher's widget picker), and Glance moves 1.1.1 → 1.2.0
  now. Placement on the emulator and the Pixel 10 Pro XL was approved for this
  task in the same question.
- **049 (round widget-preview-round-1):** five candidates on the real launcher
  sheet: A sample widget, B skeleton, C your data, D annotated, E poster. The
  question named all five in its text; AskUserQuestion's four-option cap listed
  A–D with E reachable through Other. The maintainer picked B, then wrote
  "actually im changing my answer to A" before confirmation. The ledger keeps
  B → reopened → A. B–E were rejected; their picker code is removed.

## Facts checked before the round

- Neither widget declared `previewLayout` or `previewImage`; the sheet showed
  the app icon (seen on the emulator earlier on 2026-09-30, task brief).
- Glance 1.1.1 `requestPinGlanceAppWidget` composes the preview at the
  provider's minimum size with `AppWidgetId(INVALID_APPWIDGET_ID)`; passing a
  real widget would render its unconfigured state, so the sheet needs its own
  preview widget (`WidgetSheetPreview`).
- Glance 1.2.0 (stable, maven.google.com) adds `providePreview`,
  `setWidgetPreviews`, `composeForPreview` and a `previewSize` for the sheet.
  Its AAR needs compileSdk ≥ 35 (36 here), AGP ≥ 8.6 (8.10.1), Compose runtime
  1.7.8 and Kotlin stdlib 2.0.21 (2.0.21 here). After the bump `./gradlew
  assembleDebug testDebugUnitTest` was green with all 343 existing tests.

## Round 1 renderer

Debug-only `WidgetPreviewPickerActivity` (round build, before the lock): chips
A–E, the question, a per-candidate "Sheet: … / Widget picker: …" line, a live
trace, Reset, and `Open Repository sheet` / `Open Pinned repos sheet`, which
compose the candidate and call `GlanceAppWidgetManager.requestPinGlanceAppWidget`
so the launcher's own `QuickstepAddItemActivity` sheet is the canvas. Candidate
C read the phone's own widget data behind a proof filter (saari-co/RepoGlance
only). Manifest `.grilltrack/work/picker/widget-preview-round-1.json`
validated with `validate_picker.py` (`valid`).

Finding: the Pixel Launcher honours `EXTRA_APPWIDGET_PREVIEW` on Android 17
(XL) and API 36 (emulator). `uiautomator dump` cannot see inside the preview,
so the sheet's text is asserted from dumps and the preview from captures.

### Pixel 10 Pro XL `63310DLCQ000RV`, Android 17, live session, dark

APK `31921af656287238b174b1a026848ae47475bbf33b583cf50fd756e6e9aba14b`
(`e8fd7be7…` from the hidden-pin trace onward). Run
`widget-preview-round1-xl`. `picker-A-light` and `sheet-A-repo-light` are
named light but the phone was dark.

| capture | SHA-256 |
| --- | --- |
| sheet-A-repository-dark | `713018872a6b9b4d9085cb9195b3e8bb420607041d65f86a6f181e4bbd27c76e` |
| sheet-A-pinned-dark | `23b2058447c3c47e8c043c63784295bed79c0ae27523ed4585d7a1ad7731632b` |
| sheet-B-repository-dark | `62c64a90e77ec37f8513927852da61b33f1dfaf4f338a6b7db543f6e7508f386` |
| sheet-B-pinned-dark | `cd38572cd1e43361a32ab3774a399568e4a5aaadc7cf80bc783a04d4c3ed3ddd` |
| sheet-C-repository-dark (saari-co/RepoGlance, 9:10 PM) | `c9df39a22c116faab689b8687488ea3a08c53071d90cc4f7614b9ce79f40b8c8` |
| sheet-C-pinned-dark (filter: Pinned · 0) | `b5492519c4ccf6c883f750236ab932ef49ef62dd19bb16defc65643b05c512e7` |
| sheet-D-repository-dark | `1e77e636988d856ed3d020ff9850e62ef43e1057534dd22e0ab2c70e37ca5662` |
| sheet-D-pinned-dark | `b496892f2928c54c8e490700370efc555e4ea08c8fddf0769fba1d458e9bc6a3` |
| sheet-E-repository-dark | `4c147761b68e11438ea0626c0194d3439a7b825e136325d340f732fa5635ebba` |
| sheet-E-pinned-dark | `d91e750f35aec30223bde40a8fbc0b2dfb8ac758b1d89fdccc17150260a87279` |
| contact-xl-dark (sent to the maintainer) | `872a9df342bc481bb1abcc3856752e3eea6f70ff58af5eb3687d2ad73e1f2e74` |

### Emulator `EMULATOR37X1X11X0` (AVD Pixel_10_Pro_Fold, API 36), signed out, light and dark

APK `e8fd7be7b41a13de56177781dfc885bb7327f8f21886764b773def772973bc88`. Run
`widget-preview-round1-emu`. C showed the real `Tap to choose a repository`
and `Pinned · 0` (no session, not in sample mode). Dark came from
`cmd uimode night yes`, restored to `no`.

| capture | light | dark |
| --- | --- | --- |
| A repository | `18e1e20355d894705b573dab2e7160d0232bde6de081cbe258a7721791b86e28` | `bab3e203e5661da15f93f4cf8764fc3bc4cb0c210faa095ef802774fd13a55f3` |
| A pinned | `18139b19c2ebd303193c6f5336e07a084223e0e2379b39414100b4a30db6ec2b` | `9406481c4f0e02ed7aaa928b2fdd89767572ab704672676c4eff693c5525547c` |
| B repository | `d4799a4e909f49b2b06b3c02988d36d0d588c95bbafd2d00024f5f859cbaa448` | `e119be24faaaa9ab2e71a24bf3e80c94f4459f586e0a77fa8ffcb2f173f05b46` |
| B pinned | `363dfa4c9dc060e36996747f0f48b52b8199fa95295080e51e29559887c365b6` | `4058f19b02d7f706589a16d2fa35aaa8da0494bd19ac8dd564ebe6490664b601` |
| C repository | `f42858fc0d5c3c13f031f883bdc0874d6bcc51ccb7ad5627ec958d577c8f002d` | `50e6738de33070c0d78eaf54e86cedcba7d49e30f51d8b4bbdee0964a6f4d2a4` |
| C pinned | `52b859fe60c381e6082737fada23b383fbb084cd741ecd60f9aa1591843f401f` | `cc505a094e534927a1d172842fd73970d9d8dfbba1d85f5c2d1355275bacfa67` |
| D repository | `be372a944d8f314a4aa3f23ec785e51fe8f3dc51c38df37b62d0e45ee522631e` | `12bf3da82e168693a426f32c2564a64cd66bdfcbb0f8a6e34a0d5b771d27ea8b` |
| D pinned | `fbfc0e80234ec57cede722497866dae93c1c9c6f4259660850e1f8b2e245089e` | `15a95cab1baabad441245dc8113ca42cd8aa67caf28b98165623289bc2e3d383` |
| E repository | `d4f28db236d133b5c8ebeaee9812d1bfc89e51c228b0e531930d61aca0a7bd24` | `c917e46587e384e6714306d154614ff8b283bef313db879894ba7e2d9897679c` |
| E pinned | `b00bac02524feaac1a6aea0882ac58174e51f9f5259310cde2503a76dbf46394` | `895dee6703c6eff69eeb23eeffe2e46a24c3f85e2dfe9ddd489859ddc9c4a43e` |

Contact sheet sent to the maintainer: `contact-emu`
`12fe6ac563dd0b3c08de04e7f21951e6cbcfd7e821e359701a86128a2e2bd18e`.

## Verification of the lock (production path)

Production code (`app/src/main`) in the verified builds is identical to
`54b95f6`; changes after APK `052afd52…` touched only the debug picker (status
from `getWidgetPreview`, the placed-widget line) and docs. `./gradlew
assembleDebug check` on `54b95f6`: BUILD SUCCESSFUL, 354 unit tests, 0
failures, lint no new issues, detekt clean, feature map ok.

### Pixel 10 Pro XL, Android 17, live session, dark

APK `052afd5237413fc9d6ad3c0f82524f538bb2ca857141d06e2fc2d9a0c8866887`; doctor
passed. Run `widget-preview-verify-xl`. Before driving, the top activity was
the picker this run had left; the launcher was otherwise idle. No RepoGlance
widget was placed and the maintainer had 0 live pins.

- **Publish.** After install, `shared_prefs/widget_previews.xml` held `1.1` for
  both receivers (written only on a successful `setWidgetPreviews`). The debug
  picker read `Repository: generated preview published · Pinned repos:
  generated preview published (stamp 1.1)`.
- **Sheet, real Add.** Catalog → `repoglance:menu` → `repoglance:menu-widgets`
  → `repoglance:widgets-add-repository`: top activity
  `QuickstepAddItemActivity`; dump `Repository`, `2 × 1`, `Cancel`, `Add to
  home screen`. Capture `sheet-repository-dark`
  `77878794ebe474c5f95fa609b369f5c063fb039f81e08222e33b3d57c4b6182a`: rocket,
  `sample` capsule, issues 5, PRs 3.
- **Add.** `Add to home screen` → `RepoWidgetConfigActivity` opened (callback
  intact on Glance 1.2.0). Chose `saari-co/RepoGlance` (not pinned), `Save
  widget` → Widgets row `repoglance:widgets-placed-7`, `saari-co/RepoGlance`,
  `Repository widget · Issues and PRs`. The setup screen was not captured (it
  lists the maintainer's repositories).
- **Pinned repos.** `repoglance:widgets-add-pinned` → sheet capture
  `sheet-pinned-dark`
  `412514206e0ec6c446823443ea2178933cdbe8cfb023c9a7f028733a65e11b8e` (Pinned · 3
  band, three sample rows) → `Add to home screen` → no setup; row
  `repoglance:widgets-placed-8`, `1 pinned repository`.
- **Live widgets on Glance 1.2.0.** `widget-bounds`: `repo 193x103dp RepoGlance
  | 11:20 PM | issues | 4` and `stack 402x334dp Pinned · 1 |
  saari-co/RepoGlance | 11:20 PM | issues 4 · PRs 0 · review 0` (the stack's
  lazy list renders). Cropped capture (full screenshot deleted)
  `5d20ebc7c78a90b373ec0051df979fcf20da614cc0e4b2a1cb1487427fc209b4`. The tall
  repository size was not resized: the stack sits directly below and only the
  top/left handles showed; the home screen was not rearranged.
- **Launcher picker.** Long-press empty home → `Widgets` → search `RepoGlance`
  → `Repository, Pinned repos`: dump `Repository`, `2 × 1`, `Pinned repos`,
  `4 × 3`. Capture `picker-repoglance-dark`
  `878021aeab8df089a070ba9d13334e95959a11ea5181be7d90cceb56ed6e9e40`: the same
  two sample previews.
- **Cleanup.** Long-press → `Remove` on ids 7 and 8; `dumpsys appwidget` lists
  only the maintainer's ids 2, 3, 4; `live_pinned_repos` empty; no widget
  setups. Another package `co.saari.repoglance.cuaproof` (not this run's) was
  left alone.

### Emulator, API 36, sample mode, light and dark

APK `052afd52…` then `44c7d6e8918ea6b17c54b04d157b216c3207acb2d38d6a4ed6e969d95a09348e`
(debug-only picker changes). Run `widget-preview-verify-emu`. Started on the
Connect screen, light; widgets 4 (Pinned repos) and 5 (Repository) from
earlier runs present and left alone.

- **Publish.** `widget_previews.xml` held `1.1` for both receivers after
  install.
- **Real Add, sample mode.** `explore-sample` → menu → Widgets (`Sample mode:
  widgets you add show the sample repositories.`) → `widgets-add-repository`
  → sheet → `Add to home screen` → setup preselecting `saari-co/rocket` →
  `Save widget` → row `widgets-placed-7`; `widgets-add-pinned` → `Add to home
  screen` → row `widgets-placed-8` (`1 pinned repository`).

| capture | light | dark |
| --- | --- | --- |
| Widgets screen | `ead9dfbcefd791f569ce1b4594cd3e9001733bde721ac249ccc5043df95c8578` | `751063301c3024a96f4fd2759d3e24981c39c1dc9d63a699cef4f61d7791db0e` |
| sheet, Repository | `5d9aefafa0f8be00372f7e1259d38ba5640f1202ab396066123abb0b097d8d0c` | `98ee11eb0cfadee53a58b385f1b83ef9278c22801c64f53e4ec70d49c926f043` |
| sheet, Pinned repos | `457327b3780cbe594977f6816bb0cfa27525d98a154174f5991b42ff57baff93` | `3390e4bd28ee5d5bd67d7084063def9e3002cf12042e3deb60f2099bfbbedcd2` |
| launcher picker (generated) | `b4466e681fd5aac4f64a9bc3098ef7d32e9a84485c9d140986d915d54053b3b1` | `161725961bfb5c458f2ec64b06a23619f5edc6bc35317017b8c1b20c29c823d4` |

- **Home.** `emu-home` `d4ed208ca2fe460dde6cc344f84edafc0feea53108f6481d46c09156e7fdcfbb`:
  the new compact widget reads `rocket`, `sample`, issues 5, PRs 3; a stack
  reads `Pinned · 1`, `saari-co/rocket`, `sample`.
- **Fallback (`previewLayout`).** Debug picker `Remove generated previews`:
  status `Repository: no generated preview (previewLayout) · Pinned repos: no
  generated preview (previewLayout)` (from `getWidgetPreview`). An earlier
  status read from `generatedPreviewCategories` stayed `published` after the
  removal in the same process, so the picker now asks `getWidgetPreview`.
  The removal held because the start-up republish was refused: logcat
  `GlanceAppWidgetManager: setWidgetPreview call for …RepoWidgetReceiver with
  categories 1 was rate-limited` (and StackWidgetReceiver) at 23:33:33 and
  23:34:32, with the stamp unchanged; the next start after the window retries.

| fallback capture | light | dark |
| --- | --- | --- |
| launcher picker | `473385ef642028e28ee099966dbac4274c6919b46408bcabe34ee64610da4088` | `091b86ca87b3f0a0212064494a48035b724446960f194c2a2379605e4dba7c83` |
| sheet without a preview, Repository | `32d6c4ea424dbb9ffbeeae20c511ebaa54d0a00b092e858b48e3fad0b77f0e2f` | `875cb94d1255feafe49a809c001faba1ce690dee40fa581a518e3fbb99889f5d` |
| sheet without a preview, Pinned repos | `fd156f95424f2e12c5e7a60cd15745b153c870c10f7c1348ec8b741a59ddbe76` | `b1b3b453d2508d44f5ce3c1a7460c36a8dde77b97f221181f312a7b6e8f9e753` |

  The static copy follows dynamic colour and light/dark on this launcher
  (tertiary band and capsule, dark surfaces at night): the open question from
  the task brief is answered for API 36. Android 12–14 was not run.
- **Cleanup.** The debug picker's placed line read `id 5 Repository
  394x217dp · id 7 Repository 189x102dp · id 4 Pinned repos 394x332dp · id 8
  Pinned repos 292x447dp`; the 292 dp stack (712 px at 2.4375 px/dp) and the
  189 dp compact were dragged to `Remove` with `input motionevent`, and
  `dumpsys appwidget` then listed ids 5, 4, 2 only. Widgets read `Not set up`
  (5) and `No pins yet` (4); `Sign in with GitHub` left sample mode for the
  Connect screen with no device code; `cmd uimode night` reads `no`. The
  emulator's generated previews stay removed until a start after the
  rate-limit window republishes them.

## Re-verification after review round 1 (`8e8cdcb`)

Review: `.grilltrack/proof/widget-previews-review-20260930.md#round-1`.
`./gradlew assembleDebug check` on the fixes: BUILD SUCCESSFUL, 355 unit
tests (adds the `isDue` cases and minimum-size renders), lint no new
issues, detekt clean. APK
`2e241a3e789d006ef53253abd1e28331f0085510e99c986ebd49c7475cc7aa95`; `8e8cdcb`
changes only docs after that build.

Emulator, sample mode, light, run `widget-preview-reverify-emu` (2026-10-01):

- **Real Add on the fixed `WidgetPinning.request`.** Widgets →
  `widgets-add-repository` → sheet (`Add to home screen` in the dump) →
  `RepoWidgetConfigActivity` with `saari-co/rocket` → `Save widget`: widget
  id 9. `widgets-add-pinned` sheet capture `sheet-pinned-light-fixed`
  `03a5af5ccf598806a0794c510d5176ae1b33f332b62ca9a79fb2edbeac6dc940`.
- **Tall Repository widget on Glance 1.2.0.** Id 9 dragged to empty space in
  the left pane and stretched to 189×196 dp: the dump reads
  `saari-co/rocket`, `ISSUES 5 · PRS 3 · sample`, `ISSUE #415 · 25m`,
  `PR #412 · 25m`, `ISSUE #416 · 1h` with their titles (lazy rows render).
  Crop `95ef838d64603c1087c279e6898fca4ac5002f26d878f5fd831d0f37d7406def`
  (full screen `a4f3b518a1445da37c411fbe941312cdbef5b8ac6473f7e131e23b83502a8f3e`).
- **Rate-limited retries do not crash.** Every start of the fixed build logged
  `setWidgetPreview call for …RepoWidgetReceiver / …StackWidgetReceiver with
  categories 1 was rate-limited` (00:03:27, 00:04:18, 00:04:49) and the app
  kept running; the stamp stayed `1.1`.
- **Cleanup.** Id 9 dragged to `Remove`; `dumpsys appwidget` lists 5, 4, 2;
  Widgets reads `Not set up` (5) and `No pins yet` (4); sample mode exited
  to the Connect screen with no device code; night mode `no`.

Dependency change (`:app:dependencies --configuration
releaseRuntimeClasspath`, `origin/main` vs this branch): the Glance group
(`glance`, `glance-appwidget`, `glance-material3`, `glance-appwidget-proto`,
`glance-appwidget-external-protobuf`) 1.1.1 → 1.2.0, and the Compose runtime
group (`runtime`, `runtime-android`, `runtime-saveable`,
`runtime-saveable-android`) 1.7.3 → 1.7.8. Nothing else moves;
`core-remoteviews` 1.1.0 was already on the classpath.

## Re-verification after review round 2 (`d3f6a76`)

Review: `.grilltrack/proof/widget-previews-review-20260930.md#round-2`.
`./gradlew assembleDebug check`: BUILD SUCCESSFUL, 355 unit tests, lint no
new issues, detekt clean. APK
`b0e90876cf99d76e9381ce6685c1fb909e36e59f155f86f7100ac7127c593165`. Run
`widget-preview-reverify2-emu` (emulator, signed out, light, 2026-10-01 00:12).

- The fixture home's `Pin repo widget` and `Pin stack widget` call the same
  `WidgetPinning.request`; both opened `AddItemActivity` with the sample
  preview (`sheet-Pin-repo-widget`
  `577d276a03a7b078758cad3eb8e0e93b790c04957753b36ed2ecfbf704ca55fd`,
  `sheet-Pin-stack-widget2`
  `671d1d4c98c5a72ee7dd31bad15b5988a425aa3e69f57f817d7bc206fe6cd0df`).
  Nothing was added; `dumpsys appwidget` still lists 5, 4, 2.
  (`sheet-Pin-stack-widget` `8108f86c…` and `sheet-Pin-stack-widget2`'s first
  attempt `a2349f4e…` caught the navigator after the sheet had closed and
  are discarded.)
- The start-up publish was still refused at 00:12:30 (`rate-limited` for
  both receivers) with the stamp at `1.1` and no crash.
- Which path opened these sheets cannot be told from the device: the plain
  `requestPinAppWidget` fallback (after a failed Glance request) also returns
  true, and with the generated previews removed it would show the static
  copy, which is built to look the same. No failure was expected on this
  path; the fallback is guarded by
  `WidgetPreviewGuardTest.everyAddHandsTheSheetThePreview` only.
- `bin/verify-repoglance cleanup` afterwards.

## Final head check (review round 3 fixes)

`./gradlew assembleDebug check`: BUILD SUCCESSFUL, 355 unit tests, lint no
new issues, detekt clean. APK
`7d721885ceaa4c962368a898695e7507866a831c65b891c65cc0c3b3332fc645` on the
emulator (run `widget-preview-final-emu`): the fixture home's `Pin repo
widget` (`WidgetPinning.request`) opened `AddItemActivity` with the sample
preview, capture
`56ec6e25d36ea472d354c9ea8baf950f844a598c55c4d4bbc3386d25d99f5b6c`; nothing
added (`dumpsys appwidget`: 5, 4, 2); `cleanup` afterwards.

## Not proven

- Android 12–14 (the `previewLayout` path there) — no such device was run.
- The pre-lock app-icon sheet was not re-captured; with `previewLayout`
  declared it can no longer appear.
