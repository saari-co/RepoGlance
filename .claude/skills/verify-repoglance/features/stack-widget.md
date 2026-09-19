# Stack widget

The large RepoGlance stack widget lists every pinned repository from the live catalog, most recent push first, each with its open issues, open PRs, PRs awaiting the user's review, and the clock time its numbers were observed.

## Sub-features

- `stack-live-pins` lists exactly the live pins (the thumbtack in the live catalog), most recent push first; pinning or unpinning in the app redraws it; there is no setup screen.
- `stack-row` each row shows the repository, `issues N · PRs N · review N`, and its own clock time (`2:31 AM`, `last good Mon 7:00 PM`); a pin with nothing saved yet reads `no data` with no counts, never `0`.
- `stack-header` the header reads `Pinned · N`, with `rate limited · resets <time>` while backing off.
- `stack-empty` with no pins it reads `Pin repositories in RepoGlance`, and a tap opens the app.
- `stack-taps` a row tap opens that repository's live view in the app; a header tap opens the live catalog.

## How to get to it (user POV)

- Long-press the home screen, Widgets, RepoGlance, drag the stack widget out. It needs no setup.
- Pin repositories with the thumbtack in the live catalog; they appear in the stack.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes and the maintainer's live session exists with at least one pin.
- **Human-gated placement.** Placing the stack changes the home screen; the maintainer drags it out or keeps an existing one.

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
- Glance test tags (`stack-header`, `stack-age`, `stack-counts`) are visible to the JVM Glance tests only; on device assert by visible text.
