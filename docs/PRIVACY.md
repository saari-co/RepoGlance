# RepoGlance Privacy Policy

Last updated: 2026-09-30

## Summary

RepoGlance is a read-only Android app with home-screen widgets and an issue
navigator for GitHub repositories. RepoGlance has no developer-operated
backend: the app on your device communicates directly with GitHub's own
servers (`github.com` / `api.github.com`). The developer (Saari) does not
collect, store, receive, or share any personal data through this app.

## Data handled on your device

- **Sign-in**: RepoGlance uses GitHub's official device-authorization flow
  to let you sign in. You enter a short code on GitHub's own page; RepoGlance
  never sees your GitHub password.
- **Access token**: the access token (and refresh token) GitHub issues after
  sign-in is encrypted with a key held by Android Keystore and stored only in
  the app's private storage on your device, excluded from backups. It is used
  only to make API calls to GitHub on your behalf (for example, to list the
  repositories you have shared with RepoGlance and their open issues and pull
  requests). It is never transmitted to the developer or to any third party.
- **Repository data**: repository metadata (names, visibility, push times,
  open issue and pull request titles, labels, authors, and counts) is fetched
  directly from GitHub's API and displayed on-device. The latest successful
  result is cached on your device so the app and its widgets can show it with
  its age when GitHub cannot be reached. RepoGlance does not relay this data
  anywhere else.
- **Sample mode**: "Explore with sample data" shows made-up repositories that
  are built into the app. It makes no network requests and needs no account.

## Data sharing

RepoGlance does not share any data with the developer or with third parties,
because it has no backend to share data with. The only network
communication the app performs is with GitHub, which is governed by
[GitHub's own privacy policy](https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement).

## Analytics and advertising

RepoGlance contains no analytics, crash-reporting, advertising, or tracking
SDKs. This was verified directly against the app's source and its declared
dependencies as of this writing: no Firebase, Crashlytics, Google Analytics,
Google Play Services, AdMob, Sentry, Mixpanel, Amplitude, App Center,
Segment, or Matomo (or similar) libraries are present.

## Permissions

RepoGlance requests `android.permission.INTERNET` to reach GitHub. The
installed app also lists `ACCESS_NETWORK_STATE`, `WAKE_LOCK`,
`RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`: these are declared by
Android Jetpack WorkManager, the system library RepoGlance uses to refresh
pinned repositories in the background about every 30 minutes while a network
connection is available. RepoGlance's own code does not request wake locks or
start a foreground service. It requests no location, contacts, storage,
camera, microphone, or notification permission.

## Data retention and deletion

Your access token and any cached repository data live only on your device.
To remove them:

- Choose **Disconnect GitHub** in the app's menu, which deletes the saved
  session and the cached GitHub data, or
- Uninstall the app.

You can also revoke RepoGlance's access to your GitHub account at any time
from GitHub itself: **Settings → Applications → Authorized GitHub Apps**, on
[github.com](https://github.com/settings/apps/authorizations).

## Children

RepoGlance is a developer utility for browsing GitHub repository data and is
not directed at children.

## Contact

Questions about this policy can be raised by opening an issue at
<https://github.com/saari-co/RepoGlance/issues>, or by emailing
<smokyproductcompany@gmail.com>.

## Changes

Material changes to this policy will be posted here with an updated "Last
updated" date at the top of this document.
