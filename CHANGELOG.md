# Changelog

All notable changes to `foodhub-android` are recorded here. The format is based
on [Keep a Changelog](https://keepachangelog.com/) and the project follows
[Semantic Versioning](https://semver.org/).

`foodhub-android` versions on its own line, independent of `foodhub-api` /
`foodhub-app` / `foodhub-panel` — see `docs/development/VERSIONING.md` in the
docs repo. This file is maintained automatically by release-please from
Conventional Commits; do not edit released sections by hand.

## [0.2.0](https://github.com/KamilKamilK/Foodhub-android/compare/v0.1.0...v0.2.0) (2026-09-08)


### Features

* **auth:** pick device-identity strategy by terminal manufacturer ([6cdc858](https://github.com/KamilKamilK/Foodhub-android/commit/6cdc858b647c51f66126d1c0189835ec2957e37a))
* **designsystem:** align visual design with foodhub-app's actual brand ([3d39e0d](https://github.com/KamilKamilK/Foodhub-android/commit/3d39e0df396ff26fe91d2716ebf1a99635fba652))
* **fiscal:** add Novitus/Posnet fiscal printer drivers (Faza 5, part 2/2) ([23f1004](https://github.com/KamilKamilK/Foodhub-android/commit/23f1004ca0472e29bc5262b8fd18a72988ec9c14))
* **menu:** read the menu item productId as a string, wave 5c of [#184](https://github.com/KamilKamilK/Foodhub-android/issues/184) (contract 29) ([ae8d554](https://github.com/KamilKamilK/Foodhub-android/commit/ae8d554ac1d1af3d131d4c4caa128557c6a7eda5))
* **printing:** add core:printing module and LAN ESC/POS ticket routing ([f829dac](https://github.com/KamilKamilK/Foodhub-android/commit/f829dac07549cda245a4587f11d10e6e5f6ef1e9))
* **realtime:** subscribe to Mercure pokes and refresh the tables screen live ([ec8ef88](https://github.com/KamilKamilK/Foodhub-android/commit/ec8ef88ab62843e025e84be3e07e7600c7426807))
* **sync:** implement the offline write-ahead queue (Faza 2 core:sync) ([8a3a249](https://github.com/KamilKamilK/Foodhub-android/commit/8a3a249924858798698a47441d6ba5651a51f504))
* **tables:** add a local read cache with offline fallback ([125e7fb](https://github.com/KamilKamilK/Foodhub-android/commit/125e7fbfba94b36d151c2fb79e8c928188d10e47))
* **testing:** add Hilt/Compose instrumented test infrastructure ([8b4af05](https://github.com/KamilKamilK/Foodhub-android/commit/8b4af05fb4c4fda21a70982f2cdd57bdbae95fdf))
* **theme:** default the POS terminal to the navy dark theme ([26f4ed3](https://github.com/KamilKamilK/Foodhub-android/commit/26f4ed328ad47cfe1edf3825d6cc0b4ca588c413))
* **update:** check for and install newer POS builds from the backend ([d3795ef](https://github.com/KamilKamilK/Foodhub-android/commit/d3795ef115d7e11a7adc985c727cdb6e5232a654))


### Bug Fixes

* close out Faza 1 device verification — real bugs found on live backend ([482a5e0](https://github.com/KamilKamilK/Foodhub-android/commit/482a5e0952d2e8effbadedffc0d6196a2666bfc2))
* **menu:** show an empty-state message when there is no cache and the fetch fails ([6194c30](https://github.com/KamilKamilK/Foodhub-android/commit/6194c30a6e5b0d05ea9644c4e2d193b6038f6faa))
* **testing:** provide PrintersApi in the instrumented-test Hilt module ([e903648](https://github.com/KamilKamilK/Foodhub-android/commit/e9036482675ffd8ba286cbdf9a7f72226322c235))
* **test:** provide FiscalDeviceApi in the instrumented-test Hilt module ([0cebe15](https://github.com/KamilKamilK/Foodhub-android/commit/0cebe15260017860001154b4f0274a03225fd1b7))

## [0.1.0](https://github.com/KamilKamilK/Foodhub-android/compare/v0.1.0...v0.1.0) (2026-08-30)


### Features

* **pos:** resolve place/POS session, occupy/release tables, invoice+attribute checkout ([b8371c7](https://github.com/KamilKamilK/Foodhub-android/commit/b8371c75a9a892d3bf6a302921f047be0f27889e))
* **versioning:** independent SemVer line + API contract version header ([aa8bea4](https://github.com/KamilKamilK/Foodhub-android/commit/aa8bea4f8d655cec306fff8567a27ef8f9ec5851))


### Bug Fixes

* **build-logic:** disable CI Gradle cache, de-collide catalog version aliases ([5d3904e](https://github.com/KamilKamilK/Foodhub-android/commit/5d3904eef46c5008e292a7e050a1405582029cc3))
* **build-logic:** resolve the Android extension by concrete type in the compose plugin ([99bfc22](https://github.com/KamilKamilK/Foodhub-android/commit/99bfc22f6e6d98bf883c0695009961ef33db680e))
* **build-logic:** stop the included build from generating catalog accessors ([4911e23](https://github.com/KamilKamilK/Foodhub-android/commit/4911e23b9689ef7170a39352fbeeb451404616d4))


### Chores

* release 0.1.0 ([ef772c4](https://github.com/KamilKamilK/Foodhub-android/commit/ef772c47008b8d285acdab4384b367576476802f))

## 0.1.0 (2026-08-27)

Initial versioned baseline of the Android POS terminal (Faza 1 skeleton). Every
request declares the foodhub-api contract version this build targets in the
`X-Api-Contract-Version` header (`BuildConfig.API_CONTRACT_VERSION`, from
`foodhub.apiContractVersion` in `gradle.properties`).
