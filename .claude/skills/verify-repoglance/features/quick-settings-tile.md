# Quick Settings tile

A RepoGlance tile in the swipe-down Quick Settings shade opens the live catalog with one tap and names the repository that was pushed to most recently, with the age of that push.

## Sub-features

- `tile-label` shows `RepoGlance` with the commit-eye mark.
- `tile-subtitle` reads `owner/name · <push age>` from the last catalog load; `owner/name · open to refresh` when that load is over a day old; `Open to connect` (inactive tile) with no session.
- `tile-tap` collapses the shade and opens the live catalog; on a locked phone it asks to unlock first.

## How to get to it (user POV)

- Swipe down twice, tap the edit pencil, drag `RepoGlance` into the active tiles.
- Once placed, swipe down and tap the tile.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- **Human-gated placement.** Adding a tile changes the phone's shade layout. The maintainer either drags the tile in or approves `adb shell cmd statusbar add-tile co.saari.repoglance/.tile.RepoGlanceTileService`; remove it afterwards with `remove-tile` if the maintainer did not ask to keep it.
- A signed-in session is needed for a repository subtitle; without one the tile reads `Open to connect`.

- **Populate the subtitle.** Run `bin/verify-repoglance launch MIXED live` so a catalog load writes the latest-push record; then `adb shell am force-stop co.saari.repoglance`.
- **Open the shade.** Run `adb shell cmd statusbar expand-settings`, then `dump shade`. The dump contains a node whose `content-desc` starts with `RepoGlance, latest push to`. Capture only if the shade shows nothing private; the shade holds other apps' notifications, so treat the image as local evidence and cite its hash only.
- **Tap.** Run `adb shell cmd statusbar click-tile co.saari.repoglance/.tile.RepoGlanceTileService`, wait, then `dump after-tile-tap`. The top resumed activity is `co.saari.repoglance/.MainActivity`; the dump contains `repoglance:live` and no `RepoGlance, latest push` node (the shade collapsed).
- **Cleanup.** `adb shell cmd statusbar collapse`; `bin/verify-repoglance cleanup`.

## Gotchas

- On the Fold's wide shade the first small tile slots show the icon only; the subtitle is visible in a wide slot or on the cover display. Prove the subtitle from the `content-desc` in the dump, not from a capture.
- The subtitle is computed when the shade opens from a saved record; it never fetches. A stale line says `open to refresh` on purpose.
- `add-tile` places the tile at the front of the shade and is a device change; it needs the maintainer's approval every time.
