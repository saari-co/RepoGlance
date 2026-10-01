# widgets-look-047 — Widgets screen look, round 1 (2026-09-30)

Decision: **C preview tiles**, picked by the maintainer. The Widgets screen
(menu → Widgets) shows two tonal tiles, each with a sketch of the widget's
shape, its name, a sentence and a full-width `Add`. `On your home screen`
follows as icon rows.

## Round

- **Round id:** `widgets-look-round-1`. The manifest,
  `.grilltrack/work/picker/widgets-look-round-1.json` (ignored), validated
  with `validate_picker.py`: `valid`.
- **Canvas:** the production `WidgetsContent` (the screen behind
  menu → Widgets) on a Pixel 10 Pro XL `63310DLCQ000RV` at its own width.
  The rows were examples: one set-up repository widget, one not set up, and
  a Pinned repos widget with three pins. Toggles covered sample mode,
  "launcher can't pin" and "nothing placed".
- **Renderer:** the debug-only `WidgetsLookPickerActivity` from `3db76ca`,
  built from the working tree with one uncommitted change, the picker's
  status-inset fix. The picker was removed in `ac1637f`. Its chrome named the slot ("deciding: the LOOK of the Widgets
  screen…"), gave each chip a short name, and kept a "Last tap" trace and a
  Reset. Taps were traced only: the picker never pinned anything or opened a
  setup.
- **Locks on the canvas:** `label-typography-030` (mono sentence-case
  section heads), `shape-control-031` (tonal capsule `Add`, 24 dp tonal
  cards), `status-colour-029` (no status hue here), `settings-044`,
  `widgets-entry-045`.
- **One capture was discarded:** the first `look-A-light` showed a doubled
  status-bar gap above the top bar. The picker canvas now consumes the
  status-bar inset, and all ten captures below were taken after that fix.

| candidate | light | dark |
| --- | --- | --- |
| A add cards, then list | `9d393e3d018cba8135919b1cdacd0f3868268db628f217365ea42dcfb361dbc6` | `279332b42bdb5000ea5396c8ec62dcc250e034c608e2aed02206693cb05aa62f` |
| B settings list | `82dc51b2328c86b6224a5a709d8a6b6c78b04b9303117a53c430330a46f7dcb9` | `a7134e974224f59ad8160a4c88b50bb4051f744a353f0c5285fe2d6ceb3705dd` |
| C preview tiles | `0e8a37a67d03ff67281bf3945d98c9f926a3aac74311f349c63941bbb6189ff5` | `a7949deba9fdef24135a6c4bfd93d2268f0a4f34a68ba8af5588ade4f9ae03c2` |
| D placed first + Add | `5f9969eb42d27d2f2a8e5f84360be8e18d10a2c22eb830de0b5c0bb0c88c7168` | `3972a7a1d4d6fe6a14c0af10bf91802e3ed8229d97fdbccc1bdc18b9d2974d65` |
| E grouped by widget | `fcba0f3178412623fc8c600c8d3fe3a47adfdccbfddef1d7bc3357a8e2675d96` | `ea1851d53511413296b8a40ddeb98dce538555f018eb733352231014e0e10f7f` |

The contact sheets sent to the maintainer were built from these captures:
`sheet-light.png` `70bc82a134c4ddadbc3a193293dd4a32205eeb0c7eddf0842891fe8591ae335e`
and `sheet-dark.png` `18488cea137154d562883232492d0d65dc8ae4b72b215baff8bbd5aa2f0bb92c`.
Captures and sheets are kept in the ignored `runs/verify-repoglance-runs/`;
none is committed.

## Choice

- The live picker was left on the XL for the maintainer, and the two sheets
  were sent. The question form offered A–D plus "Other"; E was on the picker
  and in both sheets. The maintainer chose **C preview tiles**.
- **Rejected:** A, B, D and E. Their code, the `LocalWidgetsLook` seam and
  the debug picker were removed in `ac1637f`. There is no pre-lock look to
  keep, because the screen is new.

## Production verification of C

On the approved emulator `EMULATOR37X1X11X0` (AVD Pixel_10_Pro_Fold,
API 36, inner display), in sample mode, at `ac1637f`:

- `widgets-sample-before.png`
  `6c1f2c64276aa8d7107a6a046146a90a1df127359280e45acbfb00869a26596b`
- `widgets-after-change.png`
  `b844ff7c9c12cb464a6057f84cf5e5c8f67530f266533aaf6a3024c00cceabb6`

These match the C capture in layout. The Fold's inner display stretches
the tiles to full width, which is recorded as unresolved in `docs/design.md`.

On the Pixel 10 Pro XL with the live session:

- `xl-widgets-empty.png`
  `0410312304c87a782b11f71d48795d6fb97fb690ad870ce972a37c817cc04da2` at
  `ac1637f`.
- `xl-widgets-live-configured.png`
  `0fa418b10093c1cc5bf2891f981960d52b9bd403bfdacd38d6ca4ee9e3e9e9b4` at
  `9bd56be`, the branch head with the theme merge.

Full device proof is in `proof/settings-widgets-20260930/PROOF.md`.
