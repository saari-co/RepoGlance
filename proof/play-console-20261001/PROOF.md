# Play Console: store listing website field (2026-10-01)

Maintainer instruction in chat: "update the play listing website field to
repoglance.com". The change was made in the maintainer's signed-in Chrome
(Claude in Chrome), developer account "smoky", app `co.saari.repoglance`.

- **Where:** Grow users, Store presence, Store settings, "Store listing
  contact details", Edit.
- **Change:** Website `https://github.com/saari-co/RepoGlance` to
  `https://repoglance.com`. Email address and phone number untouched.
- **Publish:** "Save and publish", then the "Publish change on Google Play?"
  dialog confirmed. The footer read "Change published".
- **Read-back:** the Store settings page shows Website
  `https://repoglance.com`.
- **Nothing else changed.** No listing text, graphics, declarations, release
  or testing settings were opened or saved.

The site itself is `saari-co/repoglance-site` (live since 2026-10-01 on
Cloudflare Workers; its own proof packets record hosting and the
Access-gated CMS). The home page links back to the app's source and to the
privacy policy, which stays at `https://saari-co.github.io/RepoGlance/privacy/`
and is unchanged in Console.
