package pl.foodhub.pos.feature.menu

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.common.Money
import pl.foodhub.pos.core.database.MenuCacheDao
import pl.foodhub.pos.core.database.MenuGroupEntity
import pl.foodhub.pos.core.database.MenuItemEntity
import pl.foodhub.pos.core.network.api.MenuApi
import pl.foodhub.pos.core.network.apiCall
import javax.inject.Inject

data class MenuGroup(val id: Long, val name: String)

data class MenuItem(
    val id: Long,
    val groupId: Long?,
    val productId: String,
    val name: String,
    val productType: String,
    val unitPriceGross: Money,
)

data class Menu(val groups: List<MenuGroup>, val items: List<MenuItem>)

/**
 * Read-through: the UI observes the Room cache, [refresh] pulls a fresh snapshot from
 * `foodhub-api` and swaps it in atomically. Faza 4 will trigger [refresh] from a
 * Mercure poke instead of only on screen open.
 */
class MenuRepository
    @Inject
    constructor(
        private val menuApi: MenuApi,
        private val menuCacheDao: MenuCacheDao,
        private val dispatchers: DispatcherProvider,
    ) {
        val menu: Flow<Menu> =
            combine(menuCacheDao.observeGroups(), menuCacheDao.observeItems()) { groups, items ->
                Menu(
                    groups = groups.map { MenuGroup(it.id, it.name) },
                    items =
                        items.map {
                            MenuItem(
                                id = it.id,
                                groupId = it.groupId,
                                productId = it.productId,
                                name = it.productName,
                                productType = it.productType,
                                unitPriceGross = Money(it.unitPriceGrossMinor),
                            )
                        },
                )
            }

        suspend fun refresh(): ApiResult<Unit> =
            withContext(dispatchers.io) {
                when (val current = apiCall { menuApi.currentMenu() }) {
                    is ApiResult.Success -> {
                        val menuId = current.value.id
                        val groups = apiCall { menuApi.groups(menuId) }
                        val items = apiCall { menuApi.items(menuId) }
                        if (groups is ApiResult.Success && items is ApiResult.Success) {
                            menuCacheDao.replaceSnapshot(
                                groups = groups.value.map { MenuGroupEntity(it.id, it.name, it.position) },
                                items =
                                    items.value.map {
                                        MenuItemEntity(
                                            id = it.id,
                                            groupId = it.groupId,
                                            productId = it.productId,
                                            productName = it.productName,
                                            productType = it.productType,
                                            position = it.position,
                                            unitPriceGrossMinor = it.unitPriceGross,
                                            taxRateValue = it.taxRateValue,
                                        )
                                    },
                            )
                            ApiResult.Success(Unit)
                        } else {
                            (groups as? ApiResult.HttpError)
                                ?: (items as? ApiResult.HttpError)
                                ?: ApiResult.NetworkError(IllegalStateException("menu refresh failed"))
                        }
                    }
                    is ApiResult.HttpError -> current
                    is ApiResult.NetworkError -> current
                }
            }

        suspend fun hasCachedMenu(): Boolean = menuCacheDao.itemCount() > 0
    }
