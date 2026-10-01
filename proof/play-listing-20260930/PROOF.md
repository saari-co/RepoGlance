# Play listing assets and release-build check (2026-09-30)

Tracker: [#7](https://github.com/saari-co/RepoGlance/issues/7), step 1 (a
build Play will accept) and the store-listing graphics. Branch
`claude/play-release-0.4.0-beta.1`, off `main` at
`a0625da73c02e299b7a122b6ab78b22c19a169b3`. There were no source changes;
the proof below is of that `main` source.

## Release build Play will receive

A local `./gradlew --no-daemon testDebugUnitTest assembleRelease
bundleRelease assembleDebug` on `a0625da` finished with `BUILD SUCCESSFUL`
(101 tasks) and exit 0. These are the same Gradle tasks `release.yml` runs
on a tag. The local build is unsigned; CI signs with the upload key.

- `GITHUB_REF_NAME=v0.4.0-beta.1 ./gradlew -q printVersion` printed
  `versionName=0.4.0-beta.1` and `versionCode=40001`.
- `aapt2 dump badging` (build-tools 37.0.0) shows `targetSdkVersion:'36'`,
  `compileSdkVersion='36'`, and `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86'
  'x86_64'`.
- `aapt2 dump permissions` on the release APK lists:
  - `INTERNET`
  - `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` and
    `FOREGROUND_SERVICE`, merged from WorkManager 2.9.1
  - the AndroidX `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` signature
    permission

  There is no `AD_ID` permission and no typed `FOREGROUND_SERVICE_*`
  permission.
  - `grep` finds no `WakeLock`, `PowerManager`, `startForeground` or
    `setForeground` in `app/src/main/java`.
  - The published privacy policy said the app "requests only
    `android.permission.INTERNET`", which is false for the shipped APK. It is
    corrected in this PR (see `docs/PRIVACY.md` and `docs/privacy/index.html`,
    "Permissions").
- **16 KB page size.** The only native library is
  `libandroidx.graphics.path.so`, from Compose, for 4 ABIs.
  - Every ELF `PT_LOAD` segment is aligned to `16384` on all four ABIs.
  - `zipalign -c -P 16 -v 4` printed `Verification successful`, with each
    `.so` `(OK)`.
- Sizes: the release APK is 44,786,303 bytes and the AAB is 12,328,778 bytes
  (R8 minify is off).

## Sample-mode captures

- **Device:** the approved emulator `EMULATOR37X1X11X0` (AVD
  `Pixel_10_Pro_Fold`, API 36), booted with `-no-snapshot-save`. It was the
  only emulator running.
- **Other phones:** the Pixel 10 Pro XL (`63310DLCQ000RV`) holds the
  maintainer's GitHub session, so sample mode is unreachable there. The
  physical Fold (`59151FDCG000JA`) was attached and idle on the launcher,
  but it was not driven: its session state was unknown, and checking it
  would mean opening a possibly live catalog.
- **Doctor:** with `VERIFY_SERIAL=EMULATOR37X1X11X0`, after `launch` installed
  the local debug build, `apk_local` and `apk_device` were both
  `da3ae9cc81add08d5a77ecc78f13f070c293ddbc0482a25e9ebef8e9967d6afb`. The
  scenario launcher was present, the device was awake, and the posture was
  `OPENED`.
- **Clean status bar:** SystemUI demo mode showed the clock at `9:30`, a full
  battery and wifi, and no notifications. Dark theme came from `cmd uimode
  night yes`. Both were restored afterwards.

The walk followed `features/sample-mode.md`, run as
`VERIFY_RUN_ID=play-screens-20260930`:

1. **Entry.** The `signin` dump had `repoglance:connect-github` and
   `repoglance:explore-sample`. After `tap repoglance:explore-sample`, the
   dump had `@saariuslystoned · 7 repositories`, `repoglance:sample-bar`,
   `SAMPLE` and `Made-up repositories, not your GitHub`.
2. **Pins.** The pin toggles for `saari-co/rocket` and `dinkuskit/infra`
   pinned them, and the dump order became `saari-co/rocket`,
   `dinkuskit/infra`, `saari-co/api-server`, `saari-co/mobile-app`. Both were
   unpinned before exit; the next dump had zero `Unpin`.
3. **Repository view.** `saari-co/rocket` showed `repoglance:live-mode-*`,
   Issues `#415`–`#419` and PRs `#412`–`#414`, with `Draft` on `#413`.
4. **Persistence.** After folding (`device_state state 0`) and relaunching
   with `launch MIXED live`, the app reopened in sample mode with the pins
   kept.
5. **Exit.** `tap repoglance:sample-sign-in` led to a dump with
   `repoglance:connect-github` and `repoglance:explore-sample`, and zero
   `Enter this code`, so no device code was requested. After a force-stop
   and relaunch, `explore-sample` was present: the Connect screen, not
   sample mode.

**Guard on every capture.** A script read the paired dump(s) and asserted
three things:

- every `owner/name` label is one of the seven sample names;
- `repoglance:sample-bar` is present (absent only on the Connect screen);
- `saari-co/RepoGlance` and the `repoglance:live` chip are absent.

The owner-filter dropdown is a popup window whose dump holds only the menu,
so for that capture the dumps just before and after it (`p-home`, `p-back`)
carry the sample bar.

### Play-ready files

The Play-ready files were converted to 24-bit RGB PNG (Play rejects alpha in
screenshots). They were uploaded to the private asset release
[`saari-co/swarm-pr-assets@repoglance-play-listing-20260930`](https://github.com/saari-co/swarm-pr-assets/releases/tag/repoglance-play-listing-20260930).
The asset digests GitHub reports equal the local SHA-256s below.

| Asset | Size | SHA-256 | Raw capture (SHA-256 prefix) |
| --- | --- | --- | --- |
| [`phone-01-catalog-pinned.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-01-catalog-pinned.png) | 1080×1920 | `2abb02de39b79c2d7edbdd5ca147dc48c3a1d12f1abf174de09665390dd4b6ce` | `play-phone-1-catalog` `b50dc4122f15` |
| [`phone-02-repository-issues-and-prs.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-02-repository-issues-and-prs.png) | 1080×1920 | `8eb02c9985117504c7d91749c80b46a0ccbbf235102c34b21e77b610a4c7d6ac` | `play-phone-2-repo-both` `83535c1edbb3` |
| [`phone-03-repository-prs.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-03-repository-prs.png) | 1080×1920 | `3c3e3c24ea3949e699fa9ea9e91152ba29f10ad4145bcfdc46d4241636f8fb2d` | `play-phone-3-repo-prs` `0ee0c4df4776` |
| [`phone-04-owner-filter.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-04-owner-filter.png) | 1080×1920 | `7d9a3a3d428e642bf356e043184e021dc116e262f5b720adb4eb48ac41c15618` | `play-phone-4-owner-filter` `4de190a701c7` |
| [`phone-05-connect-or-explore-sample.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-05-connect-or-explore-sample.png) | 1080×1920 | `e26d3dc08f4da4308d43c376b1b99b6d0ea8f6f3eaaee897ebf40d660b186127` | `play-phone-5-connect` `3687ce3830ed` |
| [`fold-reference-01-catalog-pinned-inner.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/fold-reference-01-catalog-pinned-inner.png) | 2076×2152 | `a9dea5b2425aa674e6ccb5b63b8f6ae7235d90e5dd3a26d99c72694895dd5efe` | `sample-catalog-pinned-inner-dark` `12c54c947481` |
| [`fold-reference-02-repository-inner.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/fold-reference-02-repository-inner.png) | 2076×2152 | `d4591874ff5c962dd19b862b5d8da78a82c0208ef9fdc47c6a07b3b7d10e5343` | `sample-repo-inner-dark` `3dc619faea87` |
| [`fold-reference-03-repository-prs-inner.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/fold-reference-03-repository-prs-inner.png) | 2076×2152 | `2f03bf5e6b3ae2b320f2f5b296efb0af7f36df94cf1224149f9447dc17147441` | `sample-prs-inner-dark` `79a3a20fa697` |
| [`playstore-icon-512.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/playstore-icon-512.png) | 512×512 | `ca169a39b067de4abea1c8a13a41e7b63f7cf280761dd333ae1c80def8710f59` | rendered |
| [`feature-graphic-1024x500.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/feature-graphic-1024x500.png) | 1024×500 | `d013bdb49d9ce33087835d4d0efd9a3d15a8c1d723ceada76f1119d163f18a15` | rendered |

- **Phone shots.** These are real app frames on the cover display with
  `wm size 1080x1920`, which gives Play's recommended 9:16. The native cover
  panel is 1080×2364, which breaks Play's rule that the long side be at most
  twice the short side. The override was reset afterwards.
- **Fold references.** These are the native inner display. They are not
  used for a Play slot, because tablet slots need 16:9 or 9:16.
- **Icon.** The icon is a transcription of `ic_launcher_background.xml` and
  `ic_launcher_foreground.xml` to SVG, drawn over the full 108 dp canvas with
  no mask and rendered by headless Chrome. Pixel samples: top centre
  `(46,50,56)` = `#2E3238`, bottom centre `(4,5,6)` = `#040506`, mark centre
  white.
- **Feature graphic.** It shows the same mark, the wordmark (Roboto), the
  tagline, and phone shot 1 in a device frame.

### Not used

- **`sample-catalog-cover-dark`** (`6c1708e0…5158`) caught the keyguard
  after folding. It was discarded, and the retake came after `wm
  dismiss-keyguard`.
- **Landscape large-screen renders** (`play-large-1-catalog` `b3772261…ec6a`
  and `play-large-5-connect` `2ebab1f3…f88a`, at 2560×1440) were rejected as
  listing assets. In landscape the catalog header takes most of the height,
  and one repository row shows. Finding: large-screen landscape layout is
  worth a GrillTrack node before the tablet slots are filled.
- **Widgets and the Quick Settings tile** were not captured. They do not
  render sample data until `sample-widgets-039`, and faking their data is
  out of bounds.

## Restored state

After the walk:

- The sample mode exit was done.
- `wm size reset`, SystemUI demo mode `exit` and `sysui_demo_allowed 0`, and
  `cmd uimode night no` were run.
- `bin/verify-repoglance cleanup` printed `app stopped, scenario restored to
  MIXED, airplane mode off`.
- Evidence stays in the ignored `runs/verify-repoglance-runs/play-screens-20260930/`
  and `runs/play-release-runs/20260930/`.

## Addendum: widget and tile captures after #45 (2026-09-30)

These were taken after `sample-widgets-039` merged in #45 (`main` at
`f2c8816b8d69967daa091776c9e0727579c1c141`).

**Device and setup**

- The approved emulator `EMULATOR37X1X11X0`, booted with `-no-snapshot-save`.
- `launch` installed the debug build of that commit. Doctor then reported
  `apk_local` = `apk_device` =
  `36042d89028c10053529428a2f6e47cd748c88f3334353534a8ba5c47f3b6745`.
- Capture conditions matched the first set: cover display, `wm size
  1080x1920`, dark theme, SystemUI demo mode.
- The maintainer approved widget and tile screenshots from sample mode for
  this run. Run id: `play-widgets-20260930`.

**Walk**

1. **Entry.** `Explore with sample data` gave a dump with
   `repoglance:sample-bar` and `@saariuslystoned · 7 repositories`.
2. **Stack widget.** The debug fixture home's `Pin stack widget` led to the
   launcher's `Add to home screen`.
3. **Repo widget.** `Pin repo widget` led to `Add to home screen`, then to
   `RepoWidgetConfigActivity`. Its note read `Sample data: saving pins this
   sample repository; removing the widget unpins it. Sample widgets show
   made-up numbers and never refresh from GitHub.` Saving `saari-co/rocket`
   pinned it.
4. **Second pin.** Pinning `saari-co/api-server` in the catalog turned the
   stack into `Pinned · 2`.
5. **Tall size.** Dragging the repo widget's resize handles by hand made it
   tall. The launcher dump then read:
   - stack: `Pinned · 2`, `saari-co/rocket` `sample` `issues 5 · PRs 3 ·
     review 0`, `saari-co/api-server` `sample` `issues 3 · PRs 2 · review 0`;
   - repo widget: `saari-co/rocket`, `ISSUES 5 · PRS 3 · sample`, `ISSUE #415
     · 25m`, `PR #412 · 25m`, `ISSUE #416 · 1h`.
6. **Tile.** `cmd statusbar add-tile …RepoGlanceTileService` added it, and
   its content-desc read `RepoGlance, sample data, latest push to
   saari-co/rocket updated 25m ago`.
   - QS edit mode widened the tile, which then showed `RepoGlance` / `Sample
     · saari-co/rocket · 25m`.
   - `click-tile` opened `co.saari.repoglance/.MainActivity` on the sample
     catalog (`@saariuslystoned · 7 repositories`, `repoglance:sample-bar`).
7. **Exit.** `remove-tile` removed the tile, and `Sign in with GitHub` led
   back to the Connect screen (`explore-sample` present). The launcher dump
   then read `Pinned · 0`, `Pin repositories in RepoGlance`, `RepoGlance`,
   `Tap to choose a repository`.
8. **Restore.** `wm size reset`, demo mode exit, `uimode night no`, posture
   `OPENED`, and `cleanup`. The emulator was then stopped.
   - The two widgets stayed placed on the launcher's second page, because
     adb drag-to-remove did not take.
   - The emulator's quickboot snapshot is not saved, so they should not
     persist. This was not verified.

| Asset | Size | SHA-256 | Raw capture (SHA-256 prefix) |
| --- | --- | --- | --- |
| [`phone-06-home-widgets.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-06-home-widgets.png) | 1080×1920 | `00f79a4182f724d226349c07a9bcf493ae3e55dc137700f0abc47c37cdfe09a6` | `home-widgets-tall` `c27c1e5a3a7a` |
| [`phone-07-quick-settings-tile.png`](https://github.com/saari-co/swarm-pr-assets/releases/download/repoglance-play-listing-20260930/phone-07-quick-settings-tile.png) | 1080×1920 | `42eda062fa1d8ddb2fc2dc233e500f2d3e44064d30da5506bde7eb720813b6af` | `qs-tile-large` `50d74439fc24` |

**Checks on the new assets**

- The launcher dump's repository-shaped labels are `saari-co/rocket` and
  `saari-co/api-server`, both sample names.
- Every widget time slot reads `sample`.
- `saari-co/RepoGlance` does not appear.
- The asset digests GitHub reports equal the local SHA-256s.
- The screenshots still show the provisional marker (`sample-marker-040`).

## Addendum: retake after `sample-marker-040` (#49), 2026-09-30

#49 locked the sample marker: a tonal banner in the app, tertiary widget
bands, and a `Sample data · …` tile. So every listing asset was retaken from
`main` `d7421d3`.

**Device and run**

- The approved emulator `EMULATOR37X1X11X0`, running the debug build of
  branch `claude/play-console-sync`. That branch is `main` `d7421d3` plus
  docs only, so the app source is identical.
- Doctor reported `apk_local` = `apk_device` =
  `24d50effd809c4fbc51c5fa759d538e1403d260a8ffdbaa72c74c80106e9001c`.
- Capture conditions matched the earlier sets: dark theme, demo-mode status
  bar, cover display at `wm size 1080x1920`, and native inner display for
  the fold references. Run id: `play-screens-040`.

**What the dumps show**

- **Banner.** The catalog dump shows `repoglance:sample-bar`, `SAMPLE`,
  "These repositories are made up. Sign in to see your own GitHub." and
  `Sign in with GitHub`.
- **Pins.** The pinned set is `saari-co/rocket` and `saari-co/api-server`.
  The taller banner pushed `dinkuskit/infra` below the fold.
- **Widgets.** The launcher dump for screenshot 6 reads:
  - stack: `Pinned · 2`, `saari-co/rocket` `sample` `issues 5 · PRs 3 ·
    review 0`, `saari-co/api-server` `sample` `issues 3 · PRs 2 · review 0`;
  - repo widget: `saari-co/rocket`, `ISSUES 5 · PRS 3 · sample`, `ISSUE #415
    · 25m`, `PR #412 · 25m`, `ISSUE #416 · 1h`.

  This repo widget is launcher-resized and tall. It was set up again by
  tapping its `Tap to choose a repository` state, which opened
  `RepoWidgetConfigActivity` with the sample note, then saving.
  - So this run covers the case the #49 proof listed as not directly proven:
    a tall repo widget resized on a real launcher.
- **Tile.** The tile, widened in QS edit mode, reads `RepoGlance` / `Sample
  data · saari-co/rocket · 25m`. Its content-desc is `RepoGlance, sample
  data, latest push to saari-co/rocket updated 25m ago`, and `click-tile`
  opened the sample catalog.
- **Exit.** `Sign in with GitHub` led to the Connect screen with zero `Enter
  this code`. The widgets then read `Pinned · 0`, `Pin repositories in
  RepoGlance` and `Tap to choose a repository`.
- **Screenshot 5.** The Connect-screen capture is byte-identical to the first
  set (`3687ce38…`), because #49 does not touch that screen.

**Correction to the previous addendum.** The two widgets placed in the first
widget run *did* persist across the emulator restart, despite
`-no-snapshot-save`. They were reused here, and they are still on the
emulator launcher's second page.

**Assets** are in the private release
[`saari-co/swarm-pr-assets@repoglance-play-listing-20260930-040`](https://github.com/saari-co/swarm-pr-assets/releases/tag/repoglance-play-listing-20260930-040).
The earlier release is kept unchanged. Asset digests reported by GitHub equal
the local SHA-256s.

| Asset | Size | SHA-256 | Raw capture (SHA-256 prefix) |
| --- | --- | --- | --- |
| `phone-01-catalog-pinned.png` | 1080×1920 | `a2125e1d90bdb976c2cc16aef891bc30bc524d62302af1afad85e9bb4dc85f12` | `phone-1-catalog` `305889d08667` |
| `phone-02-repository-issues-and-prs.png` | 1080×1920 | `af4ca3a87071670272eb017ac9ebf6d88c900e1d6d9b8e75bef40abe31af83f5` | `phone-2-repo-both` `ada166addb50` |
| `phone-03-repository-prs.png` | 1080×1920 | `15a1c1b5e4af8212587326d66b4cad867c153a3b74b393d59c4ec2ae79fc1c1a` | `phone-3-repo-prs` `b2389dcd8530` |
| `phone-04-owner-filter.png` | 1080×1920 | `96b1004710e2253b1bfb9d801d6df5da953513b12c42d6d984739391dd4fd974` | `phone-4-owner-filter` `a4d170af9b1c` |
| `phone-05-connect-or-explore-sample.png` | 1080×1920 | `e26d3dc08f4da4308d43c376b1b99b6d0ea8f6f3eaaee897ebf40d660b186127` | `phone-5-connect` `3687ce3830ed` |
| `phone-06-home-widgets.png` | 1080×1920 | `96d3239897c6f10d445d9451ebc86373ebef58058f8747191b6e1e132c415f45` | `phone-6-home-widgets` `a4f91dcb94a9` |
| `phone-07-quick-settings-tile.png` | 1080×1920 | `ca2e02569ddd77b83216da357a1e17a63810b38009ba2cb828f6de741adf543c` | `phone-7-qs-tile` `0ae2b824168f` |
| `fold-reference-01-catalog-pinned-inner.png` | 2076×2152 | `a64e1b099b51cf4aa1656e8da8fb4a5812169993dd400885d0f6c0f33370b9a0` | `fold-catalog-pinned` `4876da66aeac` |
| `fold-reference-02-repository-inner.png` | 2076×2152 | `27e2173280c884fa2adee767a5eee02267f5b008da963304b8e1baa944fd8e58` | `fold-repo` `0ff3d06ccce9` |
| `fold-reference-03-repository-prs-inner.png` | 2076×2152 | `a1b806127ca37bd368ccfbe000e314c3d43fd6f9edbb008dea3a406e81bd5eb9` | `fold-prs` `ea8936dcb2ed` |
| `feature-graphic-1024x500.png` | 1024×500 | `68a0ffa50e5d48fb3bf792664746d1594e4063b477bcc98b962ee5314258b1b8` | rendered from the new screenshot 1 |
| `playstore-icon-512.png` | 512×512 | `ca169a39b067de4abea1c8a13a41e7b63f7cf280761dd333ae1c80def8710f59` | unchanged |

**Guard on every asset.** A script asserted that:

- every repository label in the paired dump(s) is a sample name;
- `repoglance:sample-bar` is present on the app screenshots;
- `explore-sample` is present on the Connect screenshot;
- `ISSUES 5 · PRS 3 · sample` is present for the widgets;
- `Sample data · saari-co/rocket · 25m` is present for the tile;
- `saari-co/RepoGlance` appears in none of them.

**Console.** When this retake was made, Console still showed the
pre-#49 assets. The maintainer then approved the swap. These files are now
live in the listing; see "Asset swap after #49" in
`proof/play-console-20260930/PROOF.md` for the saved slots, read back in
order.
