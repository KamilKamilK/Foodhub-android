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
`.github/workflows/android-quality.yml`, just earlier. Requires a working Android
SDK + JDK 17 locally.

Skip once with `git push --no-verify` (only for something CI-runner-specific, never
a genuine failure).
