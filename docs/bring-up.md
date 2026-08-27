# Bring-up — od szkieletu do zielonego builda

Szkielet powstał bez toolchainu Androida i **nie był kompilowany**. Ta lista to
minimalna droga do pierwszego zielonego `./gradlew assembleDebug` + CI.

## 0. Środowisko (jednorazowo, per maszyna)

- [ ] JDK 17 (Temurin/Zulu). `java -version` → 17.
- [ ] Android SDK: `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`.
- [ ] `local.properties` w katalogu repo z `sdk.dir=/ścieżka/do/Android/Sdk` (plik jest w `.gitignore`).
- [ ] `git config core.hooksPath .githooks`

## 1. Wrapper

Wrapper (jar + skrypty) jest już w repo, wersja Gradle 8.11.1
(`gradle/wrapper/gradle-wrapper.properties`). CI weryfikuje sumę kontrolną jara
(`validate-wrappers: true`). Nic nie trzeba robić; ewentualny bump wersji:

```
./gradlew wrapper --gradle-version <nowa>
```

## 2. Pierwsza kompilacja — spodziewane poprawki

Uruchamiaj w kolejności, naprawiając po drodze:

```
./gradlew help                     # 2a. czy settings/version catalog się ładują
./gradlew :build-logic:convention:compileKotlin   # 2b. convention pluginy
./gradlew :core:common:test        # 2c. czysty moduł JVM + testy (MoneyTest)
./gradlew assembleDebug            # 2d. cała aplikacja
./gradlew testDebugUnitTest        # 2e. testy JVM (PinLoginViewModelTest)
./gradlew ktlintCheck detekt       # 2f. statyczna analiza
```

**Najbardziej ryzykowne miejsca (do zweryfikowania na żywym AGP 8.7):**

- `build-logic/convention/src/main/kotlin/*ConventionPlugin.kt` — sygnatura
  `CommonExtension<*, *, *, *, *, *>` (liczba parametrów typu bywa zmieniana między
  wersjami AGP), API `compilerOptions` w `KotlinAndroid.kt`.
- `libs.versions.toml` — wersje wpisane z pamięci. Zweryfikuj wzajemną zgodność:
  AGP 8.7.3 ↔ Kotlin 2.1.0 ↔ compose-compiler 2.1.0 ↔ KSP 2.1.0-1.0.29 ↔ Hilt 2.54
  ↔ Room 2.6.1 (KSP1). Compose BOM 2024.12.01.
- Room 2.6.1 działa na KSP1 — `ksp.useKSP2` jest celowo wyłączone.
- `core:network` `NetworkModule` — `retrofit2.converter.kotlinx.serialization`
  (artefakt `converter-kotlinx-serialization`, wersja = wersja Retrofita).

## 3. Uzupełnienia do produkcyjnego builda (poza „kompiluje się")

- [ ] `signingConfigs` + keystore dla `release` (`keystore.properties`, w `.gitignore`).
      Dziś `release` jest niepodpisany.
- [ ] `productFlavor` / build config per klient dla `foodhub_api_base_url`
      (dziś jeden URL w `core/network/src/main/res/values/config.xml`).
- [ ] `CertificatePinner` w `NetworkModule` (TODO w kodzie, sekcja 12 arch-doca).
- [ ] `gradle/verification-metadata.xml` — weryfikacja zależności (supply-chain);
      generowane `./gradlew --write-verification-metadata sha256 help` po ustabilizowaniu wersji.
- [ ] Testy instrumentalne + `androidTest` (dziś tylko testy JVM).
- [ ] Konfiguracja `detekt` (dziś domyślna) — własny `config/detekt/detekt.yml`, jeśli
      domyślne reguły okażą się za luźne/za ostre.
- [ ] Ochrona brancha `main` na GitHubie: wymagany zielony `Android Quality`.

## 4. Faza 1 — walidacja kontraktu na żywym terminalu

Dopiero po zielonym buildzie: uruchomić na urządzeniu/emulatorze przeciw działającemu
`foodhub-api`, przejść ścieżkę PIN → stoliki → menu → checkout, potwierdzić każdy
endpoint z `docs/rest-contract.md`. Wtedy zamknąć „Faza 1" w
`ANDROID_POS_ARCHITECTURE.md`.
