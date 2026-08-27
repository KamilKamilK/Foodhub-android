package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.PosMenuDto
import pl.foodhub.pos.core.network.model.PosMenuGroupDto
import pl.foodhub.pos.core.network.model.PosMenuItemDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Read-only menu/price surface. The terminal never edits these -- configuration
 * lives in foodhub-app (ANDROID_POS_ARCHITECTURE.md section 1).
 */
interface MenuApi {
    @GET("v1/pos-menus/current")
    suspend fun currentMenu(): PosMenuDto

    @GET("v1/pos-menus/{menuId}/groups")
    suspend fun groups(
        @Path("menuId") menuId: Long,
    ): List<PosMenuGroupDto>

    @GET("v1/pos-menus/{menuId}/items")
    suspend fun items(
        @Path("menuId") menuId: Long,
    ): List<PosMenuItemDto>
}
