# Play Console read-back (2026-09-30)

This is a read-only check of Play Console for `co.saari.repoglance`. It was
taken after the setup session and after #49 merged. It backs the Console
state that `docs/store-listing.md` records.

- **How:** Claude in Chrome, in the maintainer's signed-in Chrome, account
  "smoky" (Personal). Values were read from the page DOM with small
  JavaScript reads.
- **Nothing changed:** no Save or Submit was pressed during the read-back.
  One sign-in-details dialog was opened to read it, then closed; Console
  showed no pending changes.
- **The earlier saves** were made with the maintainer's approval item by
  item. The approvals are in the session and in the #7 checklist comment.

## App content

**Need attention** reads "You're all caught up."

**Actioned** has 10 declarations, each last edited Sep 30, 2026: Data
safety, Target audience and content, Sign in details, Content ratings,
Health apps, Financial features, Government apps, Advertising ID, Ads,
Privacy policy.

| Declaration | Saved value read back |
| --- | --- |
| Privacy policy | `https://saari-co.github.io/RepoGlance/privacy/` |
| Ads | "No, my app does not contain ads" checked |
| Advertising ID | "No" checked |
| Government apps | "No" checked |
| Financial features | only "My app doesn't provide any financial features" checked |
| Health apps | only "My app does not have any health features" checked |
| Content ratings | see the ratings list below |
| Target audience and content | only "18 and over" checked |
| Data safety | "Does your app collect or share any of the required user data types?" is "No". The preview reads "No data collection declared", "No data shared with third parties", and the privacy policy URL above. |
| Sign in details | see the fields below |

**Content ratings:** IARC status Completed, contact
`smokyproductcompany@gmail.com`, submitted "September 30, 2026, 5:59 PM".
The ratings are:

- ClassInd (Brazil): All ages
- ESRB (North America): Everyone
- PEGI (Europe): PEGI 3
- USK (Germany): All ages
- IARC Generic (rest of world): Rated for 3+
- Google Play, Russia: Rated for 3+
- Google Play, South Korea: Rated for 3+

**Sign in details:**

- "Is any part of your app restricted?" is Yes.
- There is one entry, "Sample mode (no credentials needed)", and the Google
  testing toggle is on.
- In the entry, the username and password are empty.
- The instructions are the 442-character text in `docs/store-listing.md`,
  read back verbatim.
- The box "Sign in details in this declaration provide full access to all
  the features and content within this app, including premium or paid
  content" is checked.

## Store presence

- **Store settings:**
  - App or game: App
  - Category: Productivity
  - Tags: none
  - Email: `smokyproductcompany@gmail.com`
  - Phone: empty
  - Website: `https://github.com/saari-co/RepoGlance`
- **Default store listing (en-US):**
  - Name: `RepoGlance` (10 / 30)
  - Short description: the 76-character line in `docs/store-listing.md`
    (76 / 80)
  - Full description: 2,304 / 4,000. Its SHA-256 computed in the page equals
    the doc's fenced text,
    `ca5be6f16203304dabc80334b2ca2a02cb68e01e66e4ffc1340c7c0f0fc22c1e`.
  - App icon: 1 / 1
  - Feature graphic: 1 / 1
  - Phone screenshots: 7 / 8
  - These assets predate the #49 marker change; see below.

## Dashboard and publishing

- **Dashboard:** the "Finish setting up your app" section is gone. The
  closed-testing lock text ("To start a closed test, finish setting up your
  app") is gone. Production shows "0 testers currently opted-in".
- **Publishing overview:** "Managed publishing off" and "Changes not yet
  submitted for review". "Send app for review" is locked with "To send
  changes for review, complete the required steps in the app dashboard",
  because no release exists yet. This was before the closed test; see
  [Closed test started](#closed-test-started-maintainer-approved-2026-09-30).

## Android developer verification

`co.saari.repoglance` is Registered. All four keys read **Verified**,
including the upload key
`D2:02:92:84:3E:10:C7:8E:5B:38:84:A8:23:C6:76:4E:AB:E1:15:B6:1E:26:2D:DF:37:40:79:E6:5A:56:2B:E6`,
which was added earlier the same day with status "In review".

## Asset swap after #49 (maintainer-approved, 2026-09-30)

Bobby approved swapping the retaken assets into Console. The steps:

- The old feature graphic and the seven old phone screenshots were removed
  from the default listing's slots. They stay in the Console asset library.
- The new files from `repoglance-play-listing-20260930-040` were uploaded
  under a `-040` suffix so they could not be confused with the old ones.
  Their bytes are identical to the release files.
- The new files were added in order, and the listing was saved with the AI
  asset declaration "Don't label assets".

**Read-back of the saved slots**, in order:

1. `playstore-icon-512.png`
2. `feature-graphic-1024x500-040.png`
3. `phone-01-catalog-pinned-040.png`
4. `phone-02-repository-issues-and-prs-040.png`
5. `phone-03-repository-prs-040.png`
6. `phone-04-owner-filter-040.png`
7. `phone-05-connect-or-explore-sample.png`
8. `phone-06-home-widgets-040.png`
9. `phone-07-quick-settings-tile-040.png`

Notes on the read-back:

- **Screenshot 5 kept its old name.** The library has a single
  `phone-05-connect-or-explore-sample.png`. The `-040` upload was
  byte-identical (`e26d3dc0…`) and was merged into the existing asset, so
  this slot shows the same image as the retake.
- **Listing status:** "Ready to send for review".
- **AI declaration:** reopening Review and selecting "Don't label assets"
  left Save disabled. Console saw no change, so the stored declaration is
  "Don't label assets".

## Closed test started (maintainer-approved, 2026-09-30)

Bobby asked for the closed test to be started and chose each option in the
session:

- the testers come from a Google Group
- the test is open in all countries
- he drags the AAB in himself from Finder
- the changes are sent for review

Everything else was done through Claude in Chrome. Bobby created the Google
Group himself, including its CAPTCHA.

**Track "Closed testing - Alpha":**

- **Testers:** Google Groups, `repoglance-testers@googlegroups.com`.
- **Feedback URL:** `https://github.com/saari-co/RepoGlance/issues`.
- **Countries / regions:** 177 named, plus "rest of world" (178 in all),
  unsynced from production.
- **Group settings, read back:**
  - Who can see group: Anyone on the web
  - Who can join group: Anyone on the web can join
  - Who can post and who can view members: Group managers

**Release:**

- **Signing:** the bundle panel reads "Releases are signed by Google Play"
  and "Automatic protection is on". No signing choice was offered.
  Automatic app text translation was left off.
- **Bundle:** `RepoGlance-0.4.0-beta.1.aab`, 12,395,180 bytes, SHA-256
  `79b9cea5e29ba93243480eb427f1196d31f5d2955cc15e5ef8374f23c35912c7`. Its
  SHA-256 was computed locally before the upload and equals the
  `v0.4.0-beta.1` GitHub Release asset.
- **Bundle row in Console:** App bundle, Enhanced, `40001 (0.4.0-beta.1)`,
  API levels 31+, target SDK 36, 4 screen layouts, 4 ABIs, 1 required
  feature.
- **Release name:** `40001 (0.4.0-beta.1)`, as Console suggested it.
- **Release notes:** en-US only. They are the 385-character "What's new"
  block in `docs/store-listing.md`, read back verbatim from the textarea
  inside `<en-US>` tags.
- **Warnings:** Console showed two, and neither blocks the release. Both are
  tracked in #51:
  - "There is no deobfuscation file associated with this App Bundle." R8
    is off for release.
  - "This App Bundle contains native code, and you've not uploaded debug
    symbols." The only native code is AndroidX's prebuilt
    `libandroidx.graphics.path.so`.

**Submission:**

- **Publishing overview** listed 15 changes:
  - the release, set to "Start full rollout"
  - the countries (three entries)
  - "Resume track"
  - the testers group and the feedback channel
  - the default store listing
  - Content Rating, Target audience, Privacy policy, the Ads declaration,
    Data safety and Health apps
  - the app category
- **"What you've told us"** listed Sign in details, Advertising ID,
  Government apps and Financial features.
- **Sent:** "Submit 15 changes for review", then "Send changes for review".
- **After sending:**
  - The page reads "Changes in review" and "Running quick checks for
    commonly found issues" ("Up to 14 minutes remaining").
  - The track summary reads "Active", "Release 40001 (0.4.0-beta.1) in
    review" and "178 countries / regions".
  - Managed publishing stays off, so the release goes live to testers as
    soon as Google approves it.
- **Opt-in link:** the Testers tab reads "The link will be shown here when
  you publish your app", so there is no opt-in link yet.

## Not covered

- Console gives no IARC certificate ID yet; it shows `-`.
- The review result and the tester opt-in link are not recorded yet.
