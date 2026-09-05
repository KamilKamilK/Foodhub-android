# foodhub-android

Native Android POS terminal for FoodHub, the fourth REST client of `foodhub-api`
alongside `foodhub-app`, `foodhub-panel` and FoodHub Order.

The cross-cutting design lives in
[`docs/development/ANDROID_POS_ARCHITECTURE.md`](../foodhub-docs/development/ANDROID_POS_ARCHITECTURE.md)
in the docs repo. This README covers only how to build and what is in the tree today.

## Status — Faza 1 and Faza 2 closed, device-verified

Faza 1 (PIN login, table occupy/resume, read-only menu, online checkout with receipt/NIP
invoice) and Faza 2 (`core:sync`'s offline write-ahead queue: Room-backed queue, WorkManager
sync, conflict/idempotent-retry handling) are both closed and verified end to end against a
live `foodhub-api` on a real emulator, including realistic offline/online network toggling.
Printing, Mercure real-time, FCM and fiscalisation (Fazy 3–6) are **not** here yet. See
`docs/development/ANDROID_POS_ARCHITECTURE.md` §14 for the full verification record.

`./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug` is green on JDK 17 +
Android SDK 35; CI runs the same. `app/src/androidTest/` also carries a hermetic,
`MockWebServer`-backed instrumented Compose UI test suite covering PIN login, the
tables → menu → cart flow, the offline queue, and the menu/tables screens' read caches.

## Prerequisites

- JDK 17 (Android Gradle Plugin 8.x requires it)
- Android SDK with API 35 (`compileSdk`) and build-tools 35
- `local.properties` with `sdk.dir=/path/to/Android/sdk` (git-ignored)

## First-time setup

The Gradle wrapper (jar + scripts, Gradle 8.11.1) is committed; CI verifies its
checksum. Enable the hooks:

```
git config core.hooksPath .githooks
```

Remaining hardening (signing config, per-client API URL, cert pinning) is tracked in
[`docs/bring-up.md`](docs/bring-up.md).

## Build & check

```
./gradlew assembleDebug                    # debug APK
./gradlew testDebugUnitTest                # JVM unit tests
./gradlew ktlintCheck detekt                # static analysis
./gradlew :app:connectedDebugAndroidTest   # instrumented (on-device) tests
```

`./gradlew ktlintFormat` auto-fixes style. The pre-push hook runs the first three
checks (same as CI's `android-quality` job); instrumented tests need a running
emulator/device so they're a separate CI job (`instrumented-tests`) and not part of the
hook.

## Module map

```
app/                      Application, Hilt graph, NavHost, MainActivity; androidTest/
                          holds the instrumented Compose UI tests (PIN login, tables ->
                          menu -> cart, core:sync's offline queue, menu/tables offline
                          read caches) plus their Hilt test support (HiltTestRunner,
                          HiltTestActivity, TestNetworkModule backed by MockWebServer)
core/
  common/                 ApiResult, DispatcherProvider, Money (grosze)
  designsystem/           Compose theme (foodhub-app palette), PinPad, PrimaryButton
  network/                Retrofit/OkHttp, JWT interceptor + refresh authenticator,
                          error mapping, DTOs, Auth/Menu/Sales/Tables APIs
  auth/                   EncryptedSharedPreferences token store, device identity,
                          AuthRepository (PIN login), AuthTokenProvider impl,
                          PosSession resolved from the login JWT (place/posId)
  database/               Room: menu cache, table cache, offline write-ahead queue
feature/
  auth/                   PIN login screen + ViewModel
  menu/                   read-only menu browsing (cache + refresh)
  sales/                  cart + online checkout (lines -> finalize -> receipt/invoice,
                          sales-attribute picker), releases the table on success
  tables/                 room/table occupancy view (cache + refresh); opens a table by
                          creating and occupying an order, or resuming its existing
                          open order
```

Dependency rule: `feature:* -> core:* -> (nothing)`. `core:auth -> core:network` is the
only allowed cross-`core` edge; it never runs the other way.

Each module's `build.gradle.kts` applies plugins and Android config directly (no
convention plugins / `build-logic` — an included/`buildSrc` build that generates
`org.gradle.accessors.dm.LibrariesForLibs` shadows the root catalog on the module
classpath and breaks `libs.*`). ktlint + detekt are applied to every subproject from
the root `build.gradle.kts`.

## REST contract

The endpoints this client consumes are listed in
[`docs/rest-contract.md`](docs/rest-contract.md) and in
`ANDROID_POS_ARCHITECTURE.md`. `POST /v1/auth/pos-login` (PIN login) exists in
`foodhub-api` as of the commit that introduced this repo.
