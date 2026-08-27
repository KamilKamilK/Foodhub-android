package pl.foodhub.pos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.foodhub.pos.feature.auth.PinLoginRoute
import pl.foodhub.pos.feature.menu.MenuBrowseRoute
import pl.foodhub.pos.feature.sales.CartRoute
import pl.foodhub.pos.feature.tables.TablesRoute

object Routes {
    const val LOGIN = "login"
    const val TABLES = "tables"
    const val MENU = "menu"
    const val CART = "cart"
}

/**
 * Faza 1 nawigacja: logowanie PIN -> mapa stolików -> menu -> koszyk/checkout.
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
                onOpenMenu = { navController.navigate(Routes.MENU) },
            )
        }
        composable(Routes.MENU) {
            MenuBrowseRoute(
                onOpenCart = { navController.navigate(Routes.CART) },
            )
        }
        composable(Routes.CART) {
            CartRoute(
                onCheckoutComplete = {
                    navController.popBackStack(Routes.TABLES, inclusive = false)
                },
            )
        }
    }
}
