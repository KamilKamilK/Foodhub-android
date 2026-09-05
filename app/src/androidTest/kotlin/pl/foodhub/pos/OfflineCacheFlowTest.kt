package pl.foodhub.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.foodhub.pos.core.auth.PosSession
import pl.foodhub.pos.core.auth.TokenStore
import pl.foodhub.pos.core.database.MenuCacheDao
import pl.foodhub.pos.core.database.TableCacheDao
import pl.foodhub.pos.core.database.TableCacheEntity
import pl.foodhub.pos.core.designsystem.theme.FoodHubTheme
import pl.foodhub.pos.navigation.FoodHubNavHost
import pl.foodhub.pos.testing.HiltTestActivity
import pl.foodhub.pos.testing.RoutedDispatcher
import pl.foodhub.pos.testing.jsonResponse
import pl.foodhub.pos.testing.startOnLoopback
import javax.inject.Inject

/**
 * Covers the tables and menu screens' offline read cache: a Room cache with no rows at
 * all (fresh device, first launch offline, or cache cleared) shows an explicit
 * empty-state message instead of an unexplained blank screen, and the tables screen
 * additionally falls back to serving a previously cached table list -- with the same
 * "(dane offline)" staleness indicator the menu screen already has -- when a later
 * fetch fails. Hermetic against a fake `foodhub-api` via [MockWebServer]; "offline" is
 * simulated by shutting the server down after it has been assigned a port, which turns
 * every request into a genuine connection failure rather than a stubbed error response.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineCacheFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var tableCacheDao: TableCacheDao

    @Inject
    lateinit var menuCacheDao: MenuCacheDao

    private val dispatcher = RoutedDispatcher()

    @Before
    fun setUp() =
        runBlocking {
            hiltRule.inject()
            tableCacheDao.clearTables()
            menuCacheDao.clearGroups()
            menuCacheDao.clearItems()
            tokenStore.clear()
            tokenStore.save(accessToken = "test-access-token", refreshToken = "test-refresh-token")
            tokenStore.savePosSession(PosSession(placeId = PLACE_ID, placeName = "Test Place", posId = null))
            mockWebServer.dispatcher = dispatcher
            mockWebServer.startOnLoopback()
        }

    @After
    fun tearDown() {
        // Offline tests already shut the server down themselves to simulate a dropped
        // connection; MockWebServer throws on a second shutdown.
        runCatching { mockWebServer.shutdown() }
    }

    @Test
    fun tablesScreen_noCacheAndOffline_showsEmptyStateMessage() {
        mockWebServer.shutdown()

        composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = true) } }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(TABLES_EMPTY_OFFLINE_MESSAGE).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun menuScreen_noCacheAndOffline_showsEmptyStateMessage() {
        dispatcher.on("GET", "/v1/tables") {
            jsonResponse("""[{"id":"$TABLE_ID","name":"Stolik 1","number":"1","seats":4}]""")
        }
        dispatcher.on("GET", "/v1/occupied-tables") {
            jsonResponse("""[{"id":1,"orderId":"$ORDER_ID","tableId":"$TABLE_ID"}]""")
        }
        // /v1/pos-menus/current is deliberately left unstubbed (404) so the menu screen's
        // own refresh fails while the tables screen stays reachable.

        composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = true) } }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Stolik 1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Stolik 1").performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(MENU_EMPTY_OFFLINE_MESSAGE).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun tablesScreen_offlineWithPriorCache_showsStaleCachedListWithIndicator() {
        runBlocking {
            tableCacheDao.upsertTables(
                listOf(
                    TableCacheEntity(
                        id = TABLE_ID,
                        label = "Stolik 1",
                        seats = 4,
                        occupied = false,
                        openOrderId = null,
                        position = 0,
                    ),
                ),
            )
        }
        mockWebServer.shutdown()

        composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = true) } }

        // Waiting on the staleness indicator (rather than "Stolik 1", which the
        // Room-backed list can render before the failed refresh flags it stale) pins
        // the assertion to the state this test actually cares about.
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Wybierz stolik (dane offline)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Stolik 1").assertIsDisplayed()
    }

    private companion object {
        const val PLACE_ID = "place-1"
        const val TABLE_ID = "table-1"
        const val ORDER_ID = "order-1"
        const val TIMEOUT_MS = 15_000L
        const val TABLES_EMPTY_OFFLINE_MESSAGE =
            "Brak danych o stolikach — połącz się z internetem, aby pobrać listę po raz pierwszy."
        const val MENU_EMPTY_OFFLINE_MESSAGE =
            "Brak danych menu — połącz się z internetem, aby pobrać menu po raz pierwszy."
    }
}
