# Widget description honesty — proof

Claim: the Pixel launcher widget picker shows the repo widget description
"One repo's issues, PRs, and recent activity at a glance" and no longer
claims CI or release.

- Source commit: `67647af` (`app/src/main/res/values/strings.xml`, `widget_repo_description`)
- Device: Pixel 10 Pro Fold `59151FDCG000JA` (devices.tsv), posture OPENED, awake, unlocked
- Doctor: installed APK sha256 `077d32467a03ec7adfeb104cbc2ef6d34064b4cb0f5535c6bce9f0972175ca6f` equals local debug build
- Path: Home → long-press → Widgets → search "RepoGlance" → tap RepoGlance app row (real Pixel launcher picker)

UI tree (`bin/verify-repoglance dump picker-app`), relevant rows:

```text
- | RepoGlance |  | [1275,792][1526,845]
- | One repo's issues, PRs, and recent activity at a glance |  | [945,898][1856,942]
- | RepoGlance |  | [1275,1706][1526,1759]
- | A stack of your pinned repos at a glance |  | [1059,1812][1743,1856]
```

The string "CI, and release" appears nowhere in the dump.

Artifacts (ignored `runs/`, not committed):

| File | SHA-256 |
|---|---|
| `20260929T004159Z/picker-app.xml` | `4104facf87aa7bd713b06a06e55907377e754d027f8cedac81b80e1ab3eeada9` |
| `20260929T004159Z/picker-app.txt` | `3b556e457a3f11be817f30a1cae49fa56fdfb20bb5c094fe9369e861fc1afe52` |
| `20260929T004205Z/picker-app.png` | `91d9a8eefafdb5f0cbff555018874a4671b2adc24a9537dcfe390d52e784ff3c` |

Checks: `./gradlew check` exit 0.
