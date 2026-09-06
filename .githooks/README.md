# Git hooks

Enable once per clone:

```
git config core.hooksPath .githooks
```

(`foodhub-api` and `foodhub-app` wire this up automatically from their package
managers; this repo has no equivalent install step yet, so run the command above
after cloning.)

## pre-commit

`ktlintFormat` on staged `*.kt` / `*.kts`, then re-stages the result. Fast enough
for every commit. Does not run detekt, tests or the build.

Skip once with `git commit --no-verify`.

## pre-push

`ktlintCheck`, `detekt`, `testDebugUnitTest`, `assembleDebug` — the same checks as
`.github/workflows/android-quality.yml`'s `android-quality` job, just earlier. Also
runs `assembleDebugAndroidTest`, which compiles the instrumented (`androidTest`)
source set and its Hilt component — this catches a DI graph broken only for
`@HiltAndroidTest` (e.g. a `@TestInstallIn` module missing a `@Provides` the real
module has) without needing an emulator. It does **not** run the actual
instrumented tests (`android-quality.yml`'s separate `instrumented-tests` job,
`connectedDebugAndroidTest` on a KVM-accelerated emulator) — that stays CI-only,
since spinning up an emulator on every push is too slow for a local hook; a bug
only reachable by actually *running* those tests (not just compiling their DI
graph) can still slip through here. Requires a working Android SDK + JDK 17
locally.

Skip once with `git push --no-verify` (only for something CI-runner-specific, never
a genuine failure).
