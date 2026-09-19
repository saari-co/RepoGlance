# Refresh and freshness

Every RepoGlance surface says how old its data is and what the GitHub rate limit looks like, refresh is explicit, and a refresh that fails leaves the last good data visible with its age instead of pretending it is current.

## Sub-features

- `fresh-catalog-age` shows `Updated <age>` and `GitHub rate limit: N remaining` under the catalog title.
- `fresh-refresh-catalog` re-fetches the catalog from the refresh control.
- `fresh-refresh-repo` re-fetches one repository's issues and PRs.
- `fresh-failed` shows `Could not refresh GitHub right now` and `No current value` after a failed refresh, and marks the compact widget's persisted counts `last good <time>`.
- `fresh-rate-limit` shows `GitHub rate limit is low`, `is exhausted`, or `is unknown` states with a visible wait when exhausted.

## How to get to it (user POV)

- Live catalog: age and rate-limit lines under the title; the refresh icon beside `LIVE`.
- Repository view: the refresh icon beside the repository name; section headers carry `Updated <age> · Rate limit N remaining`.
- Compact widget: the freshness label beside the repository name, a clock time rather than an age because the widget picture is frozen between redraws.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- Live session present (human-gated; otherwise the fixture-only parts of this recipe still apply through the picker and navigator).
- Airplane mode off at the start.

- **Read catalog freshness.** Run `bin/verify-repoglance launch MIXED live` then `dump fresh-catalog`. The dump contains `repoglance:rate-limit` with text starting `GitHub rate limit` and a title line containing `Updated`.
- **Refresh the catalog.** Run `bin/verify-repoglance tap repoglance:refresh-repositories`, wait 3 seconds, `dump fresh-refreshed`. The title line reads `Updated just now`.
- **Open the public repo.** Follow Find a repository through the guard, open `saari-co/RepoGlance`, then `dump repo-fresh`. Section headers contain `Updated just now · Rate limit`.
- **Force a failed refresh.** Run `adb shell cmd connectivity airplane-mode enable`, wait 60 seconds, `bin/verify-repoglance tap repoglance:refresh-repository`, wait 5 seconds, `dump repo-offline`. The dump contains `Could not refresh GitHub right now` and `No current value`.
- **Widget consequence.** Run `bin/verify-repoglance launch MIXED picker` then `dump picker-offline`. The `PERSISTED` row reads `last good <time>` with counts preserved.
- **Restore.** Run `adb shell cmd connectivity airplane-mode disable` (cleanup also does this) and confirm `adb shell settings get global airplane_mode_on` prints `0`.
- **Proof.** `capture fresh-offline` on the repository view and `capture picker-offline` on the picker; both SHA-256s go in the packet.

## Gotchas

- `Updated <age>` ticks once a minute only while the screen is resumed; wait a full minute after airplane mode before expecting `1m`.
- The catalog refresh control lives beside `LIVE`; the repository refresh control is a different control with its own id. Tapping the wrong one refreshes the wrong thing and still succeeds.
- A failed refresh keeps the previous rows on screen by design; the failure text sits in the section header, not in a dialog.
- Rate-limit `LOW`/`EXHAUSTED` states cannot be forced without real quota use; assert them through the fixture navigator (`RATE_LIMITED` scenario) rather than burning quota.
