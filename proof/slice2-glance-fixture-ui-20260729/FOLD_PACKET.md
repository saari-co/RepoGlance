# WAITING_FOR_HUMAN — Fold Device Proof Packet (Slice 2)

Status: **WAITING_FOR_HUMAN** (maintainer action required)
Why gated: the Pixel 10 Pro Fold (`59151FDCG0013B`) is the maintainer's
carry phone and normally off-cable; wireless adb is forbidden by the
device-use decisions. Agents cannot and will not improvise access.

The XL proof (PROOF.md in this directory) covers app + widget behavior.
This packet covers what only the Fold can prove: **cover-display widget
fit and the inner-display two-pane navigator**, per the goal's device-proof
list.

## Option 1 — no cable, by hand (fastest)

1. On the Fold, open:
   `https://github.com/saari-co/RepoGlance/releases/tag/canary`
2. Download `repoglance-87bc859b2c53.apk`
   (sha256 `c3a053dd5071b7b570bb5452be1c725730f33233b4b1856379310f57c849b316`,
   28,667,110 bytes — built from the Slice 2 tree; debug-signed preview,
   zero permissions, fixtures only).
3. Install (allow install from browser when prompted). Expected app:
   "RepoGlance", version `0.2.0-canary.1`.
4. Long-press home screen → Widgets → RepoGlance → place **both** widgets
   (per-repo + stack). Or use the in-app "Pin repo widget" / "Pin stack
   widget" buttons.

## Option 2 — docked on aiworker-01

Dock the Fold, then any agent lane can run the exact XL flow
(`ssh aiworker@aiworker-01.swarm 'zsh -lc "adb -s 59151FDCG0013B ..."'`)
including install, scenario driving, and screencaps — say the word and it
runs unattended.

## Capture list (either option)

Screenshots wanted (filenames if agent-driven; any naming if by hand):

| frame | display | shows |
|---|---|---|
| f01 | cover | home screen with per-repo widget (SMALL fit on cover width) |
| f02 | cover | stack widget on cover display |
| f03 | inner | both widgets on inner display (wide/resized per-repo widget) |
| f04 | inner | app Navigator at inner width — **two-pane** (list left, detail right) |
| f05 | inner | two-pane with a row selected + "Open on GitHub" visible |
| f06 | inner | dark mode: widgets or two-pane (either) |
| f07 | any | one stale state: LAST_GOOD scenario (age chip) or RATE_LIMITED (banner) |

Screen-capture on the Fold: `Power + Volume-down`, or if docked use
`adb shell screencap -p /sdcard/x.png` + `adb pull` (exec-out is corrupt on
multi-display devices).

## After capture

Hand frames to any agent lane (or drop them on aiworker-01). The lane will:
review each frame, upload to `swarm-pr-assets` release
`RepoGlance-pr-4-c9294825fefb`, and append the asset table to PROOF.md.
Personal-launcher content in Fold home-screen frames gets per-frame human
review before publication per house media policy.

## Not in this packet

The rail one-tap proof (merge → canary → cp-1 pull → banner → install) is a
separate packet delivered with the PR C swarm-side deployment steps — it
needs the companion-rail deployment on cp-1 first.
