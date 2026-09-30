# Play Store Listing

Copy and declarations for RepoGlance's Google Play entry
(`co.saari.repoglance`), written for **0.4.0-beta.1**, the first
closed-testing build. Paste the fenced blocks into the matching Play Console
fields. Every claim here must stay true of the build being submitted. When
the app changes, update this file in the same PR.

Sources: [PRIVACY.md](PRIVACY.md) (published at
<https://saari-co.github.io/RepoGlance/privacy/>),
[AUTH_ARCHITECTURE.md](AUTH_ARCHITECTURE.md), `app/src/main/AndroidManifest.xml`
and the merged release manifest, and the verify-repoglance feature map.

## App name

(30 characters max; 10 used)

```
RepoGlance
```

"GitHub" stays out of the name. The description names GitHub only to say
what the app works with, and ends with a non-affiliation line.

## Short description

(80 characters max; 78 used)

```
Read-only GitHub widgets, pinned repos, and open issues and PRs on your Pixel.
```

## Full description

(4,000 characters max; 2,330 used)

```
RepoGlance keeps an eye on your GitHub repositories from your Android home screen. It is read-only: it never comments, closes, merges or changes anything on GitHub.

PIN WHAT MATTERS
Browse the repositories you share with RepoGlance, filter them by account or organization, search, and sort by most recent push. Pin a repository right from its row. Pinned repositories sort first, and they are the only ones RepoGlance refreshes in the background.

OPEN ISSUES AND PULL REQUESTS
Open a repository to see its open issues and pull requests, or just one kind, and search the loaded rows. Tap an item to open the full thread in the GitHub app.

WIDGETS AND A QUICK SETTINGS TILE
• A per-repository widget with open issue and pull request counts, and the latest items at the taller size.
• A stack widget that lists your pinned repositories, most recent push first.
• A Quick Settings tile with your latest push.
Every value shows how old it is. When GitHub can't be reached, RepoGlance shows the last good value with its age, or "no data". It never shows a made-up zero. Pinned repositories refresh about every 30 minutes on a network connection, within GitHub's rate limits.

TRY IT WITHOUT AN ACCOUNT
Tap "Explore with sample data" on the first screen to try the catalog and repository views with made-up repositories, clearly marked SAMPLE. Sample mode makes no network requests. The widgets and the tile show data from a signed-in GitHub account.

SIGN IN WITH GITHUB'S OWN FLOW
RepoGlance uses GitHub's device sign-in: you enter a short code on GitHub's own page, so RepoGlance never sees your password. You choose which repositories RepoGlance can read when you install its GitHub App, and you can change that anytime from the app's menu.

NO SERVER, NO TRACKING
There is no RepoGlance server. The app talks only to GitHub, directly from your phone. Your GitHub token is encrypted with Android Keystore and is used only to reach GitHub. There are no ads, no analytics and no trackers. Disconnect GitHub in the app's menu to delete your session and cached data from the phone.

Made for Pixel phones and foldables, with Material You dynamic color and light and dark themes.

RepoGlance is open source: https://github.com/saari-co/RepoGlance

RepoGlance is an independent app. It is not affiliated with or endorsed by GitHub, Inc.
```

## What's new (0.4.0-beta.1)

(500 characters max; 383 used)

```
First test build. Sign in with GitHub's device flow, pin the repositories you share with RepoGlance, open their issues and pull requests, and add per-repository and stack widgets plus a Quick Settings tile. No account? Tap "Explore with sample data" on the first screen. RepoGlance is read-only and talks only to GitHub. Not yet: a CI column, and sample data in the widgets and tile.
```

## Graphics

Nothing here is committed as an image (AGENTS.md). The Play-ready files and
their SHA-256s are in the private asset release
[`repoglance-play-listing-20260930`](https://github.com/saari-co/swarm-pr-assets/releases/tag/repoglance-play-listing-20260930).
Capture proof is in
[proof/play-listing-20260930/PROOF.md](../proof/play-listing-20260930/PROOF.md).

| Console field | File | Size |
| --- | --- | --- |
| App icon | `playstore-icon-512.png` | 512×512 PNG, full-bleed; Play applies the rounded mask |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 PNG, no alpha |
| Phone screenshot 1 | `phone-01-catalog-pinned.png` | 1080×1920 |
| Phone screenshot 2 | `phone-02-repository-issues-and-prs.png` | 1080×1920 |
| Phone screenshot 3 | `phone-03-repository-prs.png` | 1080×1920 |
| Phone screenshot 4 | `phone-04-owner-filter.png` | 1080×1920 |
| Phone screenshot 5 | `phone-05-connect-or-explore-sample.png` | 1080×1920 |

- **All screenshots are sample mode.** Every one comes from **Explore with
  sample data**, so none shows a real account's repositories. The repository
  names sit under the maintainer's own accounts (`saari-co`, `dinkuskit`,
  `saariuslystoned`), and each screenshot carries the `SAMPLE` bar.
- **Icon.** The icon is the ringed commit-eye, rendered from the adaptive
  launcher icon's background and foreground vectors over the full 108 dp
  canvas. This is what Android Studio's Play Store icon export does.
- **Fold.** Play has no foldable slot. The tablet slots need 16:9 or 9:16
  images of at least 1080 px. The three `fold-reference-*` files are native
  inner-display captures (2076×2152), for reference only. The tablet slots
  are optional, so leave them empty for the closed test. A 16:9 landscape
  render showed only one catalog row under the header, so it was not used
  (see the proof).
- **Widgets and tile.** There are no screenshots of these yet. Sample mode
  does not feed the widgets or the Quick Settings tile until GrillTrack
  `sample-widgets-039` lands. Add widget screenshots from sample mode after
  that, not from a real account.

## Store settings

- **App or game:** App
- **Category:** Productivity
- **Tags:** up to five that fit a read-only developer utility, from the
  Console's list
- **Email:** `smokyproductcompany@gmail.com` (Play shows it publicly; it is
  already public in the privacy policy)
- **Website:** `https://github.com/saari-co/RepoGlance`
- **Phone:** leave empty

## App content declarations

### Privacy policy

```
https://saari-co.github.io/RepoGlance/privacy/
```

### App access

Choose **All or some functionality in my app is restricted**, because live
data needs a GitHub sign-in. Add one set of instructions. Leave the username
and password empty; if the form requires them, put `Not required` in both.

- **Name:**

  ```
  Sample mode (no credentials needed)
  ```

- **Any other information** (726 characters):

  ```
  No credentials are needed. On the first screen, tap "Explore with sample data". This opens the app's real catalog, repository view and issue/PR lists on seven made-up repositories, marked SAMPLE on every screen. Sample mode makes no network requests. Things to try: filter "Account or organization" by dinkuskit, search for "rocket", pin a repository, and open saari-co/rocket to switch between Issues, PRs and Both. Tapping a sample item shows "Sample item — not on GitHub". Tap "Sign in with GitHub" to leave sample mode. Signing in with a real GitHub account shows the same screens for that account's repositories. In this version, the home-screen widgets and the Quick Settings tile show data only for a signed-in account.
  ```

If review asks for credentials anyway, the fallback decided on 2026-09-29 is
a dedicated reviewer GitHub account. That is a maintainer action.

### Ads

**No, my app does not contain ads.** The dependency list has no ad SDK.

### Content rating (IARC)

- **Email:** `smokyproductcompany@gmail.com`
- **Category:** All other app types (Utility, Productivity, Communication or
  Other)
- **Violence, sexuality, language, controlled substances, crude humour,
  gambling:** No to each
- **Users can interact or exchange content:** No. RepoGlance is read-only
  and has no posting, messaging, comments or sharing. It shows issue and
  pull request titles, labels and author names that already exist on
  GitHub.
- **Shares the user's location:** No. RepoGlance has no location permission.
- **Digital purchases:** No
- **Unrestricted internet access or a web browser:** No. RepoGlance opens
  GitHub's sign-in and access-settings pages in a Custom Tab (the system
  browser) and has no in-app browser.

### Target audience and content

- **Target age groups:** 18 and over only. This is a developer utility; the
  privacy policy says it is not directed at children.
- **Could the store listing unintentionally appeal to children:** No

### Data safety

**Does your app collect or share any of the required user data types?** No.

This follows Play's definitions: "collect" means sending data off the device
to the developer or a third party, and "share" means passing collected data
on to a third party. What the docs establish:

- RepoGlance has no developer backend, relay, token broker or analytics.
  Authentication and all reads go directly from the app to fixed HTTPS hosts
  at `github.com` and `api.github.com` (AUTH_ARCHITECTURE.md, "Network and
  credential boundary"; PRIVACY.md, "Summary" and "Data sharing").
- The GitHub token is encrypted with a non-exportable Android Keystore key,
  stored in the app's no-backup directory, and sent only to GitHub
  (AUTH_ARCHITECTURE.md, step 4; PRIVACY.md, "Access token"). A token is not
  one of Play's data types.
- Repository, issue and pull request metadata comes from GitHub, is shown
  and cached on the device, and is never relayed (PRIVACY.md, "Repository
  data").
- There are no analytics, crash-reporting, advertising or tracking SDKs
  (PRIVACY.md, "Analytics and advertising").

The only outbound traffic is the user's own sign-in and read requests to
GitHub, the service the data comes from. They carry no location, contacts,
device identifiers, app activity or diagnostics. This is the one judgment
call in the form. If Play pushes back, the conservative alternative is to
declare **Personal info → User IDs** as collected, not shared, required for
app functionality, and encrypted in transit. Nothing leaves the device
otherwise.

If a later screen asks about account creation, choose that the app does not
let users create an account. RepoGlance signs in with an existing GitHub
account and stores nothing off the device. If a deletion URL is required
anyway, use
`https://saari-co.github.io/RepoGlance/privacy/#data-retention-and-deletion`.

### Advertising ID

**No.** The merged release manifest has no
`com.google.android.gms.permission.AD_ID` (see the proof packet's permission
dump).

### Government apps, financial features, health

- **Government apps:** No, this app is not developed by or on behalf of a
  government.
- **Financial features:** My app doesn't provide any financial features.
- **Health apps:** My app does not have any health features.
- **News app** (if shown): No.

### Permissions Play may ask about

The release APK lists `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`,
`RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`. The last four come from
WorkManager's library manifest. There is no typed `FOREGROUND_SERVICE_*`
permission, so no foreground-service declaration is expected. RepoGlance's
own code never starts a foreground service. If the Console asks anyway,
answer from that fact and flag it for follow-up rather than inventing a use.
