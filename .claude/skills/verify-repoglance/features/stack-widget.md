# Pinned repos widget

The large RepoGlance Pinned repos widget (`Pinned repos` in the launcher's widget picker; the stack widget in code) lists every pinned repository from the live catalog, most recent push first, each with its open issues, open PRs, PRs awaiting the user's review, and the clock time its numbers were observed.

## Sub-features

- `stack-live-pins` lists exactly the live pins (the thumbtack in the live catalog), most recent push first; pinning or unpinning in the app redraws it; there is no setup screen.
- `stack-row` each row shows the repository, `issues N · PRs N · review N`, and its own clock time (`2:31 AM`, `last good Mon 7:00 PM`); a pin with nothing saved yet reads `no data` with no counts, never `0`.
- `stack-header` the header reads `Pinned · N`, with `rate limited · resets <time>` while backing off.
- `stack-empty` with no pins it reads `Pin repositories in RepoGlance`, and a tap opens the app.
- `stack-taps` a row tap opens that repository's live view in the app; a header tap opens the live catalog.

## How to get to it (user POV)

- In RepoGlance, three-dot menu → `Widgets` → `Pinned repos widget` → `Add`, then `Add to home screen` (see [Settings and Widgets](./settings-widgets.md)). It needs no setup.
- Or long-press the home screen, Widgets, RepoGlance, and drag `Pinned repos` out.
- Pin repositories with the thumbtack in the live catalog; they appear in the stack.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes and the maintainer's live session exists with at least one pin.
- **Placement.** An agent may place the stack itself (long-press the home screen, Widgets, search RepoGlance, drag the 4 x 3 out), preferring an empty home page and reporting where it landed. The maintainer may also drag it out, or an existing one can be reused.

- **Read the stack.** Go to the home page holding the stack, `dump stack`: the tree contains `Pinned · <N>` and one row per pin with its repository name and a clock time or `no data`; no `FIXTURE PREVIEW` text.
- **Pin change.** `launch MIXED live`, filter to `saari-co/RepoGlance`, tap its thumbtack, go home, `dump stack-after-pin`: the `Pinned · <N>` count and the rows changed accordingly. Restore the pin afterwards.
- **Row tap.** Tap a row's repository text, wait, `dump after-stack-row-tap`: the top activity is `co.saari.repoglance/.MainActivity` and the dump shows that repository's live view (`repoglance:live-home` and the repository name).
- **Header tap.** From home, tap the `Pinned · <N>` text, wait, `dump after-stack-header-tap`: the live catalog (`Find a repository`) is showing.
- **Proof.** `capture stack` on the home page; it shows the maintainer's home screen, so keep it local and cite its SHA-256.

## Gotchas

- The stack reads the numbers the background refresh saved. A newly pinned repository reads `no data` until the next refresh (about every 30 minutes) or until it is opened in the app.
- At a low rate limit only repositories with their own repo widget refresh; other stack rows keep their numbers with an older clock time.
- After an app update both widgets (repo and stack) redraw from saved data without the app being opened; no GitHub call is made, so the numbers and their time are whatever was last saved.
- CI is not fetched on the live path, so the stack has no CI column.
- In sample mode the stack lists the sample pins with `sample` in each row's time slot; see [Sample mode](./sample-mode.md). Removing a repo widget redraws the stack at once.
- While rate limited, the one-line header holds the full `rate limited · resets <time>` in the dump, but the screen cuts the time (`resets 12:0…`); assert it from a capture as well as the dump.
- Glance test tags (`stack-header`, `stack-age`, `stack-counts`) are visible to the JVM Glance tests only; on device assert by visible text.
