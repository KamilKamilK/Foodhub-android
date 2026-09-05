package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.OccupiedTableDto
import pl.foodhub.pos.core.network.model.OccupyTableResponseDto
import pl.foodhub.pos.core.network.model.TableDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TablesApi {
    @GET("v1/tables")
    suspend fun tables(): List<TableDto>

    @GET("v1/occupied-tables")
    suspend fun occupiedTables(): List<OccupiedTableDto>

    @POST("v1/tables/{tableId}/occupy/{orderId}")
    suspend fun occupy(
        @Path("tableId") tableId: String,
        @Path("orderId") orderId: String,
    ): OccupyTableResponseDto

    @DELETE("v1/tables/{tableId}/occupy/{orderId}")
    suspend fun release(
        @Path("tableId") tableId: String,
        @Path("orderId") orderId: String,
    )
}
