# Changelog

All notable changes to `foodhub-android` are recorded here. The format is based
on [Keep a Changelog](https://keepachangelog.com/) and the project follows
[Semantic Versioning](https://semver.org/).

`foodhub-android` versions on its own line, independent of `foodhub-api` /
`foodhub-app` / `foodhub-panel` — see `docs/development/VERSIONING.md` in the
docs repo. This file is maintained automatically by release-please from
Conventional Commits; do not edit released sections by hand.

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
