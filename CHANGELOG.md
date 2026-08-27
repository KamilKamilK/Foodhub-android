# Changelog

All notable changes to `foodhub-android` are recorded here. The format is based
on [Keep a Changelog](https://keepachangelog.com/) and the project follows
[Semantic Versioning](https://semver.org/).

`foodhub-android` versions on its own line, independent of `foodhub-api` /
`foodhub-app` / `foodhub-panel` — see `docs/development/VERSIONING.md` in the
docs repo. This file is maintained automatically by release-please from
Conventional Commits; do not edit released sections by hand.

## 0.1.0 (2026-08-27)

Initial versioned baseline of the Android POS terminal (Faza 1 skeleton). Every
request declares the foodhub-api contract version this build targets in the
`X-Api-Contract-Version` header (`BuildConfig.API_CONTRACT_VERSION`, from
`foodhub.apiContractVersion` in `gradle.properties`).
