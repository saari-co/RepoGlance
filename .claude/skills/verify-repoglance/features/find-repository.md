# Find a repository

On the live screen a signed-in user narrows the repository catalog by account or organization and searches by name, then opens a repository to see its live issues and PRs.

## Sub-features

- `find-owner` filters the catalog to one account or organization.
- `find-search` filters by typed text; `No repositories match` is the empty state.
- `find-sort` orders the catalog: `Recent push` (default; most recently pushed first, `Push time unknown` rows last) or `A to Z`; the choice is remembered.
- `find-open` opens a matching repository's live view.
- `find-guard` (verification only) proves the catalog shows a single public repository before any capture.

## How to get to it (user POV)

- Open RepoGlance while signed in: the live catalog is the first screen.
- Tap `Account or organization` to pick an owner; type in `Find a repository` to search; tap a row to open it.

## Driving it with verify-repoglance

Preconditions:

- `bin/verify-repoglance doctor` passes.
- The phone holds the maintainer's GitHub session (human-gated; see Sign in with GitHub). Without it, this feature is `verified-unreachable` with precondition `no session`.
- The public repository `saari-co/RepoGlance` is visible to that session.

- **Open the live catalog.** Run `bin/verify-repoglance launch MIXED live` then `bin/verify-repoglance dump live`. The dump contains `repoglance:live`, `repoglance:refresh-repositories`, `repoglance:owner-filter`, and `repoglance:repo-search`. Do not capture yet.
- **Search.** Run `bin/verify-repoglance tap repoglance:repo-search`, `bin/verify-repoglance type saari-co/RepoGlance`, then `dump filtered`. Every repository-shaped label (`owner/name`) in the dump equals `saari-co/RepoGlance`; if any other appears, stop and report, and delete any capture taken.
- **Guarded capture.** Run `bin/verify-repoglance capture find-filtered`. The image shows one card, `saari-co/RepoGlance`, `Public`.
- **Open it.** Tap the card text `saari-co/RepoGlance` from the dump, then `dump repo`. The dump contains `repoglance:live-home`, `repoglance:refresh-repository`, `repoglance:live-mode-BOTH`, the section titles `Issues` and `PRs`, and `Search loaded rows`.
- **Sort.** From the unfiltered catalog run `dump sort-recent`: the first repository-shaped label has the newest `Updated … ago` line and every `Push time unknown` row sits after the dated ones. Tap `repoglance:catalog-sort-alpha`, `dump sort-alpha`: labels are in case-insensitive alphabetical order. Tap `repoglance:catalog-sort-recent` to restore. Never capture the unfiltered list; dumps only.
- **Empty state.** Return with `bin/verify-repoglance tap repoglance:live-home`, tap `repoglance:repo-search`, `type zzz-no-such-repo`, `dump empty`. The dump contains `No repositories match`.
- **Owner filter.** Tap `repoglance:owner-filter`; the menu lists `All` plus each owner. Choosing an owner leaves only that owner's rows; assert from the dump that every `owner/name` starts with the chosen owner, and do not capture an unfiltered list.

## Gotchas

- A dump of the unfiltered catalog contains the maintainer's private repository names; it may be read to check the guard but never captured, retained, or quoted.
- The search field keeps its text across a `BACK` from the repository view; clear it explicitly (`type` replaces nothing; select-all then delete, or relaunch).
- `LIVE` is a status chip, not a button; tapping it does nothing.
- If the dump shows `Connect GitHub` instead of the catalog, the session is gone; stop and report `verified-unreachable: no session`.
