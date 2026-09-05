# Faza 1 — zakres szkieletu

Odpowiada Fazie 1 z `ANDROID_POS_ARCHITECTURE.md` sekcja 14: „logowanie + parowanie
urządzenia (reużycie istniejącego flow), odczyt menu/cennika, checkout online (bez
offline, bez druku) — walidacja całego kontraktu REST na żywym terminalu".

## W zakresie

| Obszar | Stan w szkielecie |
|---|---|
| Struktura modułów Gradle (`app`/`core:*`/`feature:*`) | pełna |
| Graf DI (Hilt), nawigacja (Navigation Compose) | pełny |
| Warstwa sieci: Retrofit/OkHttp, interceptor JWT, refresh na 401, mapowanie błędów | pełna |
| Logowanie PIN-em (`POST /v1/auth/pos-login`) + bezpieczny zapis tokenów | pełne |
| Odczyt menu/cennika (cache Room + odświeżanie z `/v1/pos-menus/*`) | pełne |
| Widok stolików (`/v1/tables`, `/v1/occupied-tables`), occupy/release zamówienia | pełny |
| Checkout online (`orders → lines → confirm → finalize → receipts/invoices`), NIP→faktura, picker atrybutów sprzedaży | pełny |
| placeId/posId w checkout, rozwiązywane z JWT sesji (`core:auth`) | pełne |
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
- Ekran logowania PIN-em nie ma pola na numer seryjny POS-a — pierwsze logowanie
  nowego urządzenia w lokalu z więcej niż jednym wolnym POS-em (parowanie
  jednoznaczne tylko przy dokładnie jednym, patrz `ANDROID_POS_ARCHITECTURE.md` 2.1
  pkt 3) nie ma dziś żadnej ścieżki w UI — sparowanie wymaga dziś ręcznego wywołania
  `POST /v1/auth/pos-login` z `posId` (UUID) albo `PUT /v1/devices/{mac}/pair-with-pos`
  przez administratora. Do zaadresowania razem z D2 (wybór modelu terminala).

## Faza 1 — zamknięta (2026-09-05)

Uruchomiona na emulatorze (Pixel 6, API 35) przeciw żywemu `foodhub-api`: PIN →
stoliki → menu → checkout (paragon i faktura) przechodzi end-to-end, z
potwierdzeniem każdego endpointu z `docs/rest-contract.md`. Weryfikacja odsłoniła i
naprawiła kilka realnych błędów po obu stronach (kontrakt menu bez nazwy/ceny,
crash klawiatury PIN w layoucie landscape, brakujący krok `confirm` w checkout,
brakujący fallback waluty linii przy wystawianiu dokumentu) — pełna lista w
`ANDROID_POS_ARCHITECTURE.md` sekcja 14.
