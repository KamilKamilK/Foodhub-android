# Faza 1 — zakres szkieletu

Odpowiada Fazie 1 z `ANDROID_POS_ARCHITECTURE.md` sekcja 14: „logowanie + parowanie
urządzenia (reużycie istniejącego flow), odczyt menu/cennika, checkout online (bez
offline, bez druku) — walidacja całego kontraktu REST na żywym terminalu".

## W zakresie

| Obszar | Stan w szkielecie |
|---|---|
| Struktura modułów Gradle (`app`/`build-logic`/`core:*`/`feature:*`) | pełna |
| Graf DI (Hilt), nawigacja (Navigation Compose) | pełny |
| Warstwa sieci: Retrofit/OkHttp, interceptor JWT, refresh na 401, mapowanie błędów | pełna |
| Logowanie PIN-em (`POST /v1/auth/pos-login`) + bezpieczny zapis tokenów | pełne |
| Odczyt menu/cennika (cache Room + odświeżanie z `/v1/pos-menus/*`) | pełne |
| Widok stolików (`/v1/tables`, `/v1/occupied-tables`) | ekran + ViewModel |
| Checkout online (`orders → lines → finalize → receipts`) | ścieżka gotówkowa, TODO: NIP/faktura, atrybuty sprzedaży |
| Motyw Compose spójny z paletą `foodhub-app` | pełny |
| Hooki `.githooks/`, CI `android-quality.yml` | pełne |

## Poza zakresem (Fazy 2–6)

- Kolejka write-ahead + `WorkManager` sync + rozwiązywanie konfliktów (`core:sync`) —
  interfejs `TransactionQueue` to jedyny ślad
- Druk ESC/POS przez LAN/USB (`core:printing`)
- Mercure/SSE „poke" dla stolików i nowych zamówień (`core:realtime`)
- Powiadomienia FCM (`feature:notifications`)
- Fiskalizacja (Novitus/Elzab)
- Dystrybucja floty (MDM / Managed Google Play / `GET /apk`)

## Otwarte, zależne od decyzji użytkownika

- **D2** — model terminala i jego SDK: `DeviceIdentityProvider` wysyła dziś `ANDROID_ID`
  jako `device.macAddress`; do zmiany na sprzętowy numer seryjny po wyborze urządzenia.
- **placeId w checkout** — `CartViewModel.checkout()` przyjmuje `placeId` z zewnątrz;
  docelowo pochodzi z kontekstu POS-a sparowanego urządzenia, wystawionego przez
  `core:auth` po zalogowaniu. Dziś przekazywany jako pusty string (TODO).
