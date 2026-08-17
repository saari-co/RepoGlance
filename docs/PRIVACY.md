# RepoGlance Privacy Policy

Last updated: 2026-08-17

## Summary

RepoGlance is a read-only Android home-screen widget that shows information
about GitHub repositories. RepoGlance has no developer-operated backend: the
app on your device communicates directly with GitHub's own servers
(`github.com` / `api.github.com`). The developer (Saari) does not collect,
store, receive, or share any personal data through this app.

## Data handled on your device

- **Sign-in**: RepoGlance uses GitHub's official OAuth device-authorization
  flow to let you sign in. This flow is run by GitHub; RepoGlance never sees
  your GitHub password.
- **Access token**: the access token GitHub issues after sign-in is stored
  locally on your device. It is used only to make API calls to GitHub on
  your behalf (for example, to fetch the repositories you ask the widget to
  track). It is never transmitted to the developer or to any third party.
- **Repository data**: repository metadata (name, stats, activity, etc.) is
  fetched directly from GitHub's API and displayed on-device. RepoGlance does
  not relay this data anywhere else.

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
Segment, or Matomo (or similar) libraries are present. The app requests only
the `android.permission.INTERNET` permission.

## Data retention and deletion

Your access token and any cached repository data live only on your device.
To remove them:

- Sign out within the app, or
- Uninstall the app.

You can also revoke RepoGlance's access to your GitHub account at any time
from GitHub itself: **Settings → Applications** (Authorized OAuth Apps), on
[github.com](https://github.com/settings/applications).

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
