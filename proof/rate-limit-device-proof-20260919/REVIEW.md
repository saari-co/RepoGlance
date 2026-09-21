# Review — `rate-limit-device-proof-026`

Source reviewed: `git:c52e4e3` (the full diff `948c1593..c52e4e3`), by an
independent read-only reviewer agent. The reviewer also ran
`testDebugUnitTest testReleaseUnitTest compileReleaseKotlin detekt lintDebug`,
which exited 0.

**Standards: clean.**
- Main has one wiring line, with no comment and no `Log` call.
- The release twin is the identity function and compiles at the same call
  site.
- OAuth and device flow keep their own transport, so the fault can't reach
  them.
- No main-thread I/O on a production path.
- A null header key is handled.
- Nothing is logged or leaked.

**Source intent: matches the locked decision.** The feature docs match the
code, and PROOF.md is consistent with the code and with itself.

Findings (all P3, debug-only), with adjudication:

| # | Finding | Classification |
| --- | --- | --- |
| 1 | `load()` running in a worker can race a concurrent `arm()`, so the new arm is ignored until the process restarts | defer |
| 2 | `record()` read-modify-write can lose a count when two responses are recorded at once; PROOF's served counts show that faults were served, not an exact audit | defer |
| 3 | While LOW is armed, a real GitHub 403/429 is masked by the rewritten headers | reject_false_positive (by design: decision Q1 rewrites headers on real calls) |
| 4 | An unknown `rateFault` value (for example lowercase) is silently ignored | defer |

This review is advisory. It doesn't replace the merge rails (CI on the head,
OpenClaw autoreview, a ClawSweeper review of the exact head).
