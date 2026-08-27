# foodhub-android

Native Android POS terminal for FoodHub, the fourth REST client of `foodhub-api`
alongside `foodhub-app`, `foodhub-panel` and FoodHub Order.

The cross-cutting design lives in
[`docs/development/ANDROID_POS_ARCHITECTURE.md`](../foodhub-docs/development/ANDROID_POS_ARCHITECTURE.md)
in the docs repo. This README covers only how to build and what is in the tree today.

## Status — Faza 1 skeleton

This is the **Faza 1 skeleton**: module structure, DI graph, navigation, the network
layer wired to the real `foodhub-api` contract, PIN login, read-only menu browsing,
and an online-only checkout path. Offline queue, printing, Mercure real-time, FCM and
fiscalisation (Fazy 2–6) are **not** here yet.

> **This skeleton was committed without being compiled.** It was authored in an
> environment with no Android SDK / Gradle / JDK 17. The first real quality gate is
> `.github/workflows/android-quality.yml` (or a local run once the toolchain is set up).
> Expect to fix compilation and lint issues on the first build —
> [`docs/bring-up.md`](docs/bring-up.md) is the checklist and lists the risky spots.

## Prerequisites

- JDK 17 (Android Gradle Plugin 8.x requires it)
- Android SDK with API 35 (`compileSdk`) and build-tools 35
- `local.properties` with `sdk.dir=/path/to/Android/sdk` (git-ignored)

## First-time setup

The Gradle wrapper (jar + scripts, Gradle 8.11.1) is committed. CI verifies the
wrapper jar checksum. Just enable the hooks:

```
git config core.hooksPath .githooks
```

Then follow [`docs/bring-up.md`](docs/bring-up.md).

## Build & check

```
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew ktlintCheck detekt   # static analysis
```

`./gradlew ktlintFormat` auto-fixes style. The pre-push hook runs the same four
checks as CI.

## Module map

```
app/                      Application, Hilt graph, NavHost, MainActivity
build-logic/              Gradle convention plugins (foodhub.android.*)
core/
  common/                 ApiResult, DispatcherProvider, Money (grosze)
  designsystem/           Compose theme (foodhub-app palette), PinPad, PrimaryButton
  network/                Retrofit/OkHttp, JWT interceptor + refresh authenticator,
                          error mapping, DTOs, Auth/Menu/Sales/Tables APIs
  auth/                   EncryptedSharedPreferences token store, device identity,
                          AuthRepository (PIN login), AuthTokenProvider impl
  database/               Room menu cache; TransactionQueue interface (Faza 2 stub)
feature/
  auth/                   PIN login screen + ViewModel
  menu/                   read-only menu browsing (cache + refresh)
  sales/                  cart + online checkout (orders -> lines -> finalize -> receipt)
  tables/                 room/table occupancy view
```

Dependency rule: `feature:* -> core:* -> (nothing)`. `core:auth -> core:network` is the
only allowed cross-`core` edge; it never runs the other way.

## REST contract

The endpoints this client consumes are listed in
[`docs/rest-contract.md`](docs/rest-contract.md) and in
`ANDROID_POS_ARCHITECTURE.md`. `POST /v1/auth/pos-login` (PIN login) exists in
`foodhub-api` as of the commit that introduced this repo.
