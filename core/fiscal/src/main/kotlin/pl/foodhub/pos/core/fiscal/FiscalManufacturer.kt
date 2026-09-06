package pl.foodhub.pos.core.fiscal

/**
 * Wire contract with `foodhub-api`'s `FiscalManufacturer` enum
 * (`PlaceContext\Domain\Model\FiscalDevice\FiscalManufacturer`) -- values must match
 * exactly, since [pl.foodhub.pos.core.network.model.FiscalDeviceDto.manufacturer]
 * deserializes the backend's string directly into this type.
 */
enum class FiscalManufacturer {
    NOVITUS,
    POSNET_ONLINE,
}
