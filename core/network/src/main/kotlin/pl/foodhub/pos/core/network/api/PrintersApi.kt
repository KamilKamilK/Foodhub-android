package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.PrinterDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PrintersApi {
    @GET("v1/places/{placeId}/printers")
    suspend fun printers(
        @Path("placeId") placeId: String,
    ): List<PrinterDto>
}
