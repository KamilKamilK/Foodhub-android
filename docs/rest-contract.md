# Kontrakt REST konsumowany przez terminal (Faza 1)

Wycinek API `foodhub-api` używany przez appkę. Wyekstrahowany z przeglądarkowego POS-a
(`foodhub-app/src/features/sales/api/pos-runtime.ts`, `.../menu/api/menus.ts`) i z
`ANDROID_POS_ARCHITECTURE.md`. Bazowy URL: jeden per wdrożenie
(`core/network/src/main/res/values/config.xml`, `foodhub_api_base_url`).

## Auth

| Metoda | Ścieżka | Uwagi |
|---|---|---|
| POST | `/v1/auth/pos-login` | `{ pin, device: { macAddress, name, model, platform, version }, posId? }` → `{ token, refreshToken }`. Nowy authenticator w `foodhub-api` (`PosPinAuthenticator`). 401 = zły PIN / urządzenie niesparowane / niejednoznaczny PIN. |
| POST | `/v1/auth/refresh-token` | `{ refreshToken }` → `{ token, refreshToken }`. Wywoływane przez `TokenRefreshAuthenticator` na 401. |

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
