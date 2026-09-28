# family-look round 2 (label typography): picker captures on the Pixel 10 Pro Fold

- Date: 2026-09-28 UTC. Device `59151FDCG000JA` (Pixel 10 Pro Fold, Android 17), inner display
  `4619827677550801152` (2076x2152), light mode, themed icons on. Settings untouched;
  `dumpsys trust deviceLocked=0` before each drive.
- Scope confirmed by the maintainer: section heads, chips/status pills, age and rate-limit lines,
  button labels. Body text stays M3.
- Canvas: `type-picker` route, EXACT scenario, chrome collapsed, production HomeScreen (catalog) and
  NavigatorScreen (Account scope, BOTH) with the candidate provided through `LocalLabelType`.
  Status E tonal pills, mark, dynamic colour unchanged.
- Method: per frame `dump` before, `capture`, `dump` after; traces equal, package set only
  `co.saari.repoglance`, needed text present (Navigator / Issues, case-insensitive), else PNG deleted.
  First pass lost B/C navigator to a case-sensitive text check (PNGs deleted); the rerun passed 10/10.
  Committed crops remove the system status bar and gesture bar (rows 150..2100). PNGs stay in `runs/`.
- Fidelity: C uses Space Grotesk static instances (500, 600) cut from Intercom's OFL variable woff2,
  debug source set only. D uses the system monospace, not Space Mono.

| frame | sha256 (crop) |
| --- | --- |
| type-A-catalog.png | 87cb3360029654af6cab105b0905a1d121e1f330e3544650c08d7d2dc24999c9 |
| type-A-navigator.png | d26086bc4dca41168ec4e810791c8465683c60f81696eff2492fb92fb97b3ce1 |
| type-B-catalog.png | db01d8d900f0e6090976a8d583866061731cfc96b69e6f4a8db04cb8841968a2 |
| type-B-navigator.png | f3828734d782573d764db377a11463a65ff20c3ca825b197d0f638ce2e33753c |
| type-C-catalog.png | 0b81109d6bb8e1bc21fc76015277485da7ee77cf1a4ddb26f94e7d39052fe97e |
| type-C-navigator.png | 9f5ed011f4e498910ed850d70624974d3da35efac52a467ff5ddd83d01c923c2 |
| type-D-catalog.png | c5574707c3813da1fb40229482491f64173d427bc691afee5389f266591ac5db |
| type-D-navigator.png | aa55325600a631f322f46487c8353e6407656955098723b716faf074ad250b7d |
| type-E-catalog.png | 3a15d08704d3fdc86e58995c659e24813b32c3d79031b5f196682ea19fc6a881 |
| type-E-navigator.png | 18240e9f0e2dc46e5991e2face004abb4dcbbd729ec1a2d369faf0efdfba37d6 |
