# PR #2 OpenClaw Review-Fix Proof

Date: 2026-08-12

Repo: `saari-co/RepoGlance`
PR: https://github.com/saari-co/RepoGlance/pull/2
Base: `main` at `7c9789e8f4996f40912dc94fc516baf92ab41306`
Reviewed head: `47a7b6452318112aa5a85c834136a17af7d268a9`
Repair branch: `codex/repoglance-pr2-review-fixes-20260812`
Worktree: `/Users/bobbybones/Developer/worktrees/RepoGlance-pr2-review-fixes-20260812`

## Review Input

The official OpenClaw autoreview first ran on Spark-2 through the x-api queue.
Its credential scanner failed before code review, so the result was classified
as a degraded review-host blocker rather than a product finding. The contract's
cockpit fallback then reviewed a clean detached worktree at the same immutable
base/head tuple and reported five actionable findings.

## Accepted Findings And Repairs

1. `required_fix` — standard `Authorization: Bearer <credential>` input could
   leave the credential behind after the generic authorization matcher consumed
   only the scheme. A full bearer-header matcher now redacts the scheme and
   credential together; a dynamically constructed regression input proves the
   credential is absent.
2. `required_fix` — `NavigatorList` did not enforce the value-basis truth
   contract. `UNKNOWN` now requires empty rows and no observation time;
   `EXACT` and `LAST_GOOD` require `observedAt`. Model-level construction tests
   cover the rejected and accepted shapes.
3. `required_fix` — fixture rows could contradict `OPEN` and
   `AWAITING_MY_REVIEW`. Open fixtures now contain only open rows; awaiting-
   review PRs are open, non-draft, and `REVIEW_REQUIRED`. Fixture-integrity
   tests cover both modes.
4. `required_fix` — the mixed snapshot corpus reused two repo identities for
   five scenarios. It now selects five distinct repositories, and the corpus
   test asserts one unique `RepoRef` per snapshot.
5. `required_fix` — the freshest push label rendered `Pushed just now ago`.
   It now mirrors the existing updated-label special case and renders
   `Pushed just now`; rendering tests cover fresh and older timestamps.

No finding was rejected, deferred, or routed to a human gate. This is repair
cycle 1 for the PR #2 source slice.

## Verification

```text
git diff --check
  PASS

ANDROID_HOME=/Users/bobbybones/Library/Android/sdk \
  ./gradlew testDebugUnitTest assembleDebug
  PASS — BUILD SUCCESSFUL, 69 unit tests, debug APK assembled
```

The SDK path was supplied only as a process environment value. No
`local.properties`, credential, token, auth log, keystore, or secret was read,
created, copied, or committed.

## Gates And Next Step

- No merge, release, deploy, signing, distribution, GitHub App/auth change,
  device mutation, customer send, destructive cleanup, or secret handling.
- Push the repair commit as a fast-forward to PR #2's existing head branch.
- Re-materialize and rerun official OpenClaw against the new exact head.
- Request ClawSweeper only after that terminal result is clean and the PR head
  still matches.
