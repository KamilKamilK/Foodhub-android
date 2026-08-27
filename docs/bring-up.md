# Bring-up

## 0. Środowisko (jednorazowo, per maszyna)

- [ ] JDK 17 (Temurin/Zulu). `java -version` → 17.
- [ ] Android SDK: `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`.
- [ ] `local.properties` w katalogu repo z `sdk.dir=/ścieżka/do/Android/Sdk` (plik jest w `.gitignore`).
- [ ] `git config core.hooksPath .githooks`

## 1. Build

Wrapper (jar + skrypty, Gradle 8.11.1) jest w repo; CI weryfikuje sumę kontrolną jara.
Cała bramka jakości przechodzi na JDK 17 + SDK 35:

```
./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug
```

`./gradlew ktlintFormat` auto-poprawia formatowanie. Pre-push hook uruchamia to samo co CI.

### Uwaga o strukturze build-scriptów

Nie ma `build-logic`/`buildSrc` ani convention pluginów. Included/buildSrc build
generujący `org.gradle.accessors.dm.LibrariesForLibs` przesłania katalog `libs`
głównego builda na classpathie modułów — `libs.*` przestaje się rozwiązywać w każdym
module, który aplikuje taki plugin. Dlatego każdy moduł aplikuje wtyczki i konfigurację
`android {}` wprost (~15 linii powtórzenia), a `ktlint`/`detekt` idą z roota przez
`subprojects {}`. Jeśli ktoś rozwiąże tę interakcję (albo Gradle ją naprawi),
konwencjonalne pluginy można wprowadzić refaktoringiem.

## 2. Uzupełnienia do produkcyjnego builda

- [ ] `signingConfigs` + keystore dla `release` (`keystore.properties`, w `.gitignore`) — dziś `release` jest niepodpisany.
- [ ] `productFlavor` / build config per klient dla `foodhub_api_base_url` (dziś jeden URL w `core/network/src/main/res/values/config.xml`).
- [ ] `CertificatePinner` w `NetworkModule` (TODO w kodzie, sekcja 12 arch-doca).
- [ ] `gradle/verification-metadata.xml` — weryfikacja checksumów zależności; `./gradlew --write-verification-metadata sha256 help` po ustabilizowaniu wersji.
- [ ] Testy instrumentalne (`androidTest`) — dziś tylko testy JVM.
- [ ] Ochrona brancha `main` na GitHubie: wymagany zielony `Android Quality`.
- [ ] `feature:sales` / `feature:tables` — ciała `TODO` do dokończenia (picker atrybutów sprzedaży, NIP→faktura, occupy/release stolika, `placeId` z kontekstu POS-a).

## 3. Faza 1 — walidacja kontraktu na żywym terminalu

Uruchomić na urządzeniu/emulatorze przeciw działającemu `foodhub-api`, przejść ścieżkę
PIN → stoliki → menu → checkout, potwierdzić każdy endpoint z `docs/rest-contract.md`.
Wtedy zamknąć „Faza 1" w `ANDROID_POS_ARCHITECTURE.md` §14.
