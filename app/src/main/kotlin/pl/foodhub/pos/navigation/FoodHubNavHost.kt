package pl.foodhub.pos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.foodhub.pos.feature.auth.PinLoginRoute
import pl.foodhub.pos.feature.menu.MenuBrowseRoute
import pl.foodhub.pos.feature.sales.CartRoute
import pl.foodhub.pos.feature.tables.TablesRoute

object Routes {
    const val LOGIN = "login"
    const val TABLES = "tables"
    const val MENU_PATTERN = "menu/{orderId}/{tableId}"
    const val CART_PATTERN = "cart/{orderId}/{tableId}"

    fun menu(
        orderId: String,
        tableId: String,
    ) = "menu/$orderId/$tableId"

    fun cart(
        orderId: String,
        tableId: String,
    ) = "cart/$orderId/$tableId"
}

private val orderTableArguments =
    listOf(
        navArgument("orderId") { type = NavType.StringType },
        navArgument("tableId") { type = NavType.StringType },
    )

/**
 * Faza 1 nawigacja: logowanie PIN -> mapa stolików -> menu -> koszyk/checkout. Wybór
 * stolika otwiera (lub wznawia) jedno zamówienie, którego id towarzyszy ekranom
 * menu/koszyka aż do zapłaty (sekcja 9 -- "zajętość stolików, otwarte rachunki").
 * Offline, druk, powiadomienia i podgląd rachunków dochodzą w kolejnych fazach.
 */
@Composable
fun FoodHubNavHost(
    startAuthenticated: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (startAuthenticated) Routes.TABLES else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            PinLoginRoute(
                onLoggedIn = {
                    navController.navigate(Routes.TABLES) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TABLES) {
            TablesRoute(
                onOpenMenu = { orderId, tableId ->
                    navController.navigate(Routes.menu(orderId, tableId))
                },
            )
        }
        composable(Routes.MENU_PATTERN, arguments = orderTableArguments) { backStackEntry ->
            val orderId = checkNotNull(backStackEntry.arguments?.getString("orderId"))
            val tableId = checkNotNull(backStackEntry.arguments?.getString("tableId"))
            MenuBrowseRoute(
                onOpenCart = { navController.navigate(Routes.cart(orderId, tableId)) },
            )
        }
        composable(Routes.CART_PATTERN, arguments = orderTableArguments) {
            CartRoute(
                onCheckoutComplete = {
                    navController.popBackStack(Routes.TABLES, inclusive = false)
                },
            )
        }
    }
}
