# family-look round 1 (status-colour-029): picker captures on the Pixel 10 Pro Fold

- Date: 2026-09-21/22 UTC. Device: Pixel 10 Pro Fold `59151FDCG000JA`, Android 17,
  inner display `4619827677550801152` (2076x2152), posture OPENED, light mode with
  themed icons on; dark judged through the picker's own preview toggle. System
  settings untouched; `dumpsys trust deviceLocked=0` before every drive.
- Build: debug APK from the worktree at the commit recorded in the ledger
  (`implementation_ref`), installed by `bin/verify-repoglance launch`.
- Canvas: production `HomeScreen` under `RepoGlanceTheme`, chrome collapsed.
  EXACT top frame shows Passing + Failing; EXACT scrolled shows Running + Passing;
  MIXED top shows Failing + Passing (Cached); MIXED scrolled shows the
  "Rate-limited — backing off" banner, Passing and No CI.
- Method: per frame, `dump` before, `capture`, `dump` after; the picker trace
  line and the package set (`co.saari.repoglance` only, full frame) must match
  and the needed state text must be present, else the PNG is deleted. All 40
  frames passed. PNGs stay in ignored `runs/verify-repoglance-runs/<run>/`.
- Fidelity notes: harmonisation in candidates C and E is an HSV hue nudge, not
  HCT. The first attempt with expanded chrome showed only one card and was
  discarded as misleading (kept in runs/ 20260921T2356..2358 for history).

| frame | sha256 |
| --- | --- |
| EXACT-A-dark-scrolled.png | cfc340d5e7bd14b188f711adddef76b38e7777c2f7e49754e4499461fa0bfee0 |
| EXACT-A-dark.png | 46c7f1a3ce587ecea22f01c8b7224b23e31ee0e72e7d10c7c59ef139bca9f85c |
| EXACT-A-light-scrolled.png | 5ab9f339ae8ef17da47976125a8dfff032802170a2fa36ef628b3eeadae71458 |
| EXACT-A-light.png | 28ce4bb3269a3f069d3c081ce897e84d7da1186ccd9e51ebd19d93f95366eefa |
| EXACT-B-dark-scrolled.png | 876fd4ec6a4e684bb449102883cf49f58a4a5612c669d013ab87c9d5d464c3e0 |
| EXACT-B-dark.png | 71a10a21537ef760d1b1ed781229d66f6d6ad024918d7a49fcc10db127b00588 |
| EXACT-B-light-scrolled.png | 5e88028233f0fc179a83a33fa363a8eb8eab459d9ffced56549a49339d2e4219 |
| EXACT-B-light.png | 658f1dd6dcf4a24d3bd596ef0d62def722e2d89fb21a9cd137dbb9a052182e5f |
| EXACT-C-dark-scrolled.png | 18d8517f71bf62884a3ff0698f51e73bc2ef9865cd915678fa290f0d4f8355a9 |
| EXACT-C-dark.png | 0d467157b57ab543c821920fdcabfbc51a1d3b747106afd91adb716ba0385f6e |
| EXACT-C-light-scrolled.png | 38f874e64512ddb0e2bd0860183a2b1f253f97d76f64eefa384b0a107d3c362e |
| EXACT-C-light.png | 7009ca9d7664fc7c02ac5e4cf3f0d32d8e7a64e663f03b580048075e4ab0e03f |
| EXACT-D-dark-scrolled.png | 2c93b18b16c76d87a10cd12a05e5cf97e538024340f5bd996738ea3305a39557 |
| EXACT-D-dark.png | 13b2d8aa00bf05fdbe5b0707a599a4b2667fa8c8152f65957353905023aefe0b |
| EXACT-D-light-scrolled.png | e4fa7571d083fa92af641ca19086811a8a685512db20ed77d572b381be6fba62 |
| EXACT-D-light.png | e298e2e27ce561529d7704c49d12b898d961de7bee2717b128854bf35c69acf4 |
| EXACT-E-dark-scrolled.png | 2e59601eb78150dc0f7af049528287b13df080814d743fc8d1a6155684f1941b |
| EXACT-E-dark.png | f28382bb24cbed47ba1ac1c78d3818838599c451fdf3ab3856fbb21294e0a643 |
| EXACT-E-light-scrolled.png | ae77fc3e9ddc081e2543e28d6f82d426eaa0c9ba68b481f26947daa64244047e |
| EXACT-E-light.png | 7fa2d8a4c20b1b29c6cd5ec9b00d0998c75c26fd5420c5d68ebed3af48c9efe6 |
| MIXED-A-dark-scrolled.png | 5b38c969a03a28027c18ccc70787d2ff285b59a7dd5c5893bc3a2ff4d0f1cc72 |
| MIXED-A-dark.png | 75ce274af979a685c6a2632e6f40287220462a678bd631a8e65bc46b45efd868 |
| MIXED-A-light-scrolled.png | 1b822d8787412432f32ca90a892891969d6b9f1a6e96baec6418018fbee90308 |
| MIXED-A-light-scrolled.png | 575331cdbc7a381b3e402e1260fce2ddf0f42165d69c9823c33d3b23bcaa2a19 |
| MIXED-A-light.png | 094ea00f5b2999921545d53f09ea624d2c572d7c7d145986efe575e623f2fe82 |
| MIXED-B-dark-scrolled.png | aa3560761f4aea46daac507b14e1f579f2645632941c4465bef01a7726826ec1 |
| MIXED-B-dark.png | 9ad21dc90e0f2bf516be0bf4228996bf3a24cd74fc0bf4456e3db93e1d6d7a3b |
| MIXED-B-light-scrolled.png | 4495df2920f672938ba85325471d671642c75dbc947ff6d5aa64b1c099913fdd |
| MIXED-B-light.png | 94bb2434a784e5a6ebe8f7217d475c7355c1af1fccd8c6eae752e010cc4a25be |
| MIXED-C-dark-scrolled.png | d74924d15e0e064079a3a263340d84cb9605ebe4ede991908beb653d1ffca90f |
| MIXED-C-dark.png | 8ddbb746d65d5cfb7e6dd05db01a120f726f0d01988785f7d8385cab2870f467 |
| MIXED-C-light-scrolled.png | 8dcc14e83a5a67ee7e14e45336b5578605a422f3a6c83619332e9c01c56b1f07 |
| MIXED-C-light.png | faef44abbda84d5c2d710d4d6d253ea22e4f72c66d53f1aebd601b200f547404 |
| MIXED-D-dark-scrolled.png | 1af54f103818cf2ae9ee2499adfe4789e881c0183acfe429f1a2d716f613cedd |
| MIXED-D-dark.png | 1aab9b9eaebf156c35130808ec3b468ea135abd7fae4244c1662578095b9f3be |
| MIXED-D-light-scrolled.png | f06c19a13afae63ef26569d5ca6ebd0890c1991158c1cc517ee285e5e34f96c3 |
| MIXED-D-light.png | 61592a6b3da5d3ee854326196dfa0b9030280fea2596923a79b646419a873aff |
| MIXED-E-dark-scrolled.png | 76464eaee36aebaff8ec04436fc856a330d636e09964c2a77c6925160d454b1b |
| MIXED-E-dark.png | 3fad5f02610c746134c36d2fbeafbcd741ff5ab98c6d097337d01cef1d051612 |
| MIXED-E-light-scrolled.png | cbfcc309ac276d329ddeab0747a160120d0c836899ffe7b36471f95ac050dbe4 |
| MIXED-E-light.png | 266018d16ef4fb8e0d2cb7770d8336db17d72ba2a2eebf686f0f43d05d2406e0 |

## Lock E implemented in production (2026-09-22)

- Production seam: `FamilyStatus` in `ui/theme/StatusColors.kt` (CIELCh harmonise,
  tonal pills). Consumers: fixture catalog CI pill + rate-limit banner, navigator
  rate-limit banner, live screen rate-limit ink.
- Device: same Fold, light mode. Production fixture home reached through
  `launch <scenario> navigator` then the navigator's home control. Dark verified
  through the debug picker's E candidate, which now renders the production seam.
- Not device-verified: the navigator rate-limit banner (no fixture reachable
  through the launcher rendered it) and the live screen's LOW/EXHAUSTED ink (the
  signed-in session was in the OK bucket). Both share the seam with the proven
  banner and pill.
- `./gradlew check` green before commit.

| frame | sha256 |
| --- | --- |
| prod-EXACT-scrolled.png | 161a6a8f074004949f210964ae64cb62a93c2d04017f606aec4fecc82d2576cc |
| prod-EXACT-top.png | 34239e76ea52d4d937d7e5dd2bd7aadd62660b0b874e24a70e2e9c2216056b3d |
| prod-MIXED-scrolled.png | 1d6a9e027f04301cc1f36bc4b913090a799e7ad0422381c2584d113e5cbc026d |
| seam-E-dark-EXACT-scrolled.png | 5f9583dfd0cda16b0c5cda6b5e503f5389d3f569e353cd972296c39b9e09d3d2 |
| seam-E-dark-EXACT-top.png | 3f7c7466fbfc7e0a5fc0f8148cd25d4efdb976ae7c483840fb5da529fe46bf1b |
| seam-E-dark-MIXED-scrolled.png | acc4b80246bd6aba21bf53697fad6c32a7f61270822f3f8e66baa4a05e2efba5 |

## Review fix (2026-09-22)

- `1a286d8` adds the achromatic-primary guard (review P2 on `19b463d`). Reinstalled
  and recaptured the production fixture home; `./gradlew check` green.

| frame | sha256 |
| --- | --- |
| prod-fix-EXACT-top.png | 41dd9cecec4d5d7e3a0c8ae7417dbca13c3fb6314db7dc90051858d5c49a2e48 |
