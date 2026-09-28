# label-typography-030 (D mono labels): production verification on the Pixel 10 Pro Fold

- Date: 2026-09-28 UTC. Device `59151FDCG000JA`, Android 17, inner display `4619827677550801152`,
  light mode, themed icons on; `deviceLocked=0` before each drive; settings untouched.
- Build: debug APK of the implementation commit (ledger `implementation_ref`); `./gradlew check` green.
- Production default `LabelType.Mono` (ui/theme/LabelType.kt). Frames:
  - `prod-navigator-EXACT`: Issues/PRs heads, Home, mode segments and Filters in mono.
  - `prod-home-EXACT`, `prod-home-MIXED`: status pills (Failing/Passing/Unknown, equal mono widths),
    `Cached · 45m` chip, Pushed/Updated/Last push lines, Navigator and Pin buttons in mono; card titles,
    counts and release lines stay M3 body.
  - `prod-live-filtered`: live catalog filtered to `saari-co/RepoGlance` only (strict repo-name guard):
    LIVE chip, rate-limit line, Sort head, Recent push / A to Z chips, card Updated line in mono.
  - `picker-D-dark-catalog`: the same type through the picker's dark preview.
- Guard: dump before/after equal, package only `co.saari.repoglance`, needed text present, else PNG
  deleted. One live attempt failed closed on a regex false positive (`id/content`, `pin-…` tags); rerun passed.
- Known, left as is: the live header line (@login · N repositories · Updated) and the Public/Private
  line stay body type; widgets and the tile are not in this slot. Dark only via preview toggle.

| frame | sha256 (crop, rows 150..2100) |
| --- | --- |
| picker-D-dark-catalog.png | c5574707c3813da1fb40229482491f64173d427bc691afee5389f266591ac5db |
| prod-home-EXACT.png | 506a1796ce950eafea87952808fb514a7e069d4726d3f340992fa34fdf4cab34 |
| prod-home-MIXED.png | 2945e01e25f4e171670b8c1c7f5e02500564a3ac127a2131a9f0e90b65b6cd2d |
| prod-live-filtered.png | 08fc3d359bb2d6cee6f8928c4a676be615a0fc03ecc0e9ff0f63d2de588b7694 |
| prod-navigator-EXACT.png | f8b985380ca4ae8079f3b470826dd09ed15e55012ab5961fe41a52438ac3e667 |
