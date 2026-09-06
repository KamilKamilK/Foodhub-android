package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.FiscalDeviceDto
import retrofit2.http.GET
import retrofit2.http.Path

interface FiscalDeviceApi {
    /** 404 is a normal, expected response for a Pos with no fiscal device configured yet. */
    @GET("v1/places/{placeId}/pos/{posId}/fiscal-device")
    suspend fun fiscalDevice(
        @Path("placeId") placeId: String,
        @Path("posId") posId: String,
    ): FiscalDeviceDto
}
