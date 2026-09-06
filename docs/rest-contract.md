# Kontrakt REST konsumowany przez terminal (Faza 1)

Wycinek API `foodhub-api` używany przez appkę. Wyekstrahowany z przeglądarkowego POS-a
(`foodhub-app/src/features/sales/api/pos-runtime.ts`, `.../menu/api/menus.ts`) i z
`ANDROID_POS_ARCHITECTURE.md`. Bazowy URL: jeden per wdrożenie
(`core/network/src/main/res/values/config.xml`, `foodhub_api_base_url`).

## Auth

| Metoda | Ścieżka | Uwagi |
|---|---|---|
| POST | `/v1/auth/pos-login` | `{ pin, device: { macAddress, name, model, platform, version }, posId? }` → `{ token, refreshToken, mercureToken? }`. Nowy authenticator w `foodhub-api` (`PosPinAuthenticator`). 401 = zły PIN / urządzenie niesparowane / niejednoznaczny PIN. |
| POST | `/v1/auth/refresh-token` | `{ refreshToken, device? }` → `{ token, refreshToken, mercureToken? }`. Wywoływane przez `TokenRefreshAuthenticator` na 401; `device` musi być dołączone, żeby backend odtworzył kontekst miejsca i odświeżył `mercureToken` (patrz Faza 4 niżej) -- bez niego odpowiedź nie niesie żadnego z dwóch opcjonalnych pól. |

## Menu (tylko odczyt)

| Metoda | Ścieżka |
|---|---|
| GET | `/v1/pos-menus/current` |
| GET | `/v1/pos-menus/{menuId}/groups` |
| GET | `/v1/pos-menus/{menuId}/items` |

## Sala

| Metoda | Ścieżka |
|---|---|
| GET | `/v1/tables` |
| GET | `/v1/occupied-tables` |
| GET | `/v1/places/{placeId}/rooms` |
| POST / DELETE | `/v1/tables/{tableId}/occupy/{orderId}` |

## Sprzedaż

| Metoda | Ścieżka |
|---|---|
| GET | `/v1/payment-methods` |
| GET | `/v1/attributes?occurrence=sales_documents` |
| POST | `/v1/order/orders` `{ placeId }` |
| POST | `/v1/order/orders/{orderId}/lines` |
| PUT | `/v1/order/orders/{orderId}/confirm` |
| PUT | `/v1/order/orders/{orderId}/finalize` `{ paymentMethod }` |
| POST | `/v1/order/receipts` |
| POST | `/v1/order/invoices` |

`paymentMethod`: `cash` \| `card` \| `bank_transfer`. Kwoty w groszach (minor units),
tak jak w kontrakcie DDD zamówień.

## Druk (Faza 3)

| Metoda | Ścieżka |
|---|---|
| GET | `/v1/places/{placeId}/printers` |

Zwraca `[{ id, name, ip, port, role: "KITCHEN"\|"RECEIPT", orderDirectionIds }]`. Pobierane
świeżo (bez cache'a) przez `core:printing`'s `PrintRouter` przy każdym wydruku — KITCHEN
routuje linie po `orderDirectionId` (z `PosMenuItemDto`), RECEIPT dostaje pełny dokument
niezależnie od kierunku.

## Real-time (Faza 4)

Nie REST, ale konsumuje `mercureToken` z sekcji Auth powyżej. `core:realtime`'s
`MercureSubscriber` (`com.launchdarkly:okhttp-eventsource`, `BackgroundEventSource`)
otwiera SSE do `{foodhub_mercure_url}?topic=places/{placeId}/pos-state` z nagłówkiem
`Authorization: Bearer {mercureToken}`. Payload to wyłącznie `{ resource, placeId }`
(`resource` ∈ `occupied-tables | receipt-issued | invoice-issued | order-created`) —
sygnał "coś się zmieniło", nigdy dane encji; odbiorca (dziś: `TablesViewModel`) reaguje
zwykłym REST-owym `load()`. `RealtimeSessionController` startuje/zatrzymuje subskrypcję
reaktywnie na `AuthRepository.sessionState`/`posSession`/`mercureToken`, więc logowanie i
wylogowanie nie wymagają osobnego wywołania.
