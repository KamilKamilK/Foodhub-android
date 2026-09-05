package pl.foodhub.pos

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.foodhub.pos.core.auth.PosSession
import pl.foodhub.pos.core.auth.TokenStore
import pl.foodhub.pos.core.database.TransactionQueue
import pl.foodhub.pos.core.designsystem.theme.FoodHubTheme
import pl.foodhub.pos.navigation.FoodHubNavHost
import pl.foodhub.pos.testing.HiltTestActivity
import pl.foodhub.pos.testing.RoutedDispatcher
import pl.foodhub.pos.testing.jsonResponse
import pl.foodhub.pos.testing.startOnLoopback
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Exercises core:sync's offline write-ahead queue on-device: occupying a table writes
 * to a real Room DB and returns immediately (optimistic), and a real WorkManager +
 * `SyncWorker` drain it against a fake `foodhub-api` -- the newest, most complex logic in
 * the app, previously covered only by JVM-level `SyncProcessorTest`/`SyncQueueTest` with
 * `TransactionQueue`/`SalesApi`/`TablesApi` mocked out. Also doubles as the
 * screen-renders-without-crashing smoke check for the tables, menu and cart screens.
 *
 * `HiltTestRunner` boots the app under Hilt's stock `HiltTestApplication`, which does not
 * run `FoodHubPosApplication.onCreate()` (and therefore never wires WorkManager to the
 * Hilt worker factory the way the real app does), so this test performs that one-time
 * initialization itself from an injected [HiltWorkerFactory] before setting Compose
 * content -- otherwise `SyncScheduler.scheduleSync()` would fail with WorkManager
 * "not initialized".
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TableToCartFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var transactionQueue: TransactionQueue

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val dispatcher = RoutedDispatcher()

    @Before
    fun setUp() =
        runBlocking {
            hiltRule.inject()
            ensureWorkManagerInitialized()
            drainLeftoverQueue()
            tokenStore.clear()
            tokenStore.save(accessToken = "test-access-token", refreshToken = "test-refresh-token")
            tokenStore.savePosSession(PosSession(placeId = PLACE_ID, placeName = "Test Place", posId = null))
            mockWebServer.dispatcher = dispatcher
            mockWebServer.startOnLoopback()
        }

    private fun ensureWorkManagerInitialized() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            WorkManager.getInstance(context)
        } catch (unset: IllegalStateException) {
            WorkManager.initialize(context, Configuration.Builder().setWorkerFactory(workerFactory).build())
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun occupyingTable_syncsThroughRealRoomAndWorkManager_thenOpensMenuAndCart() =
        runBlocking {
            dispatcher.on("GET", "/v1/tables") {
                jsonResponse("""[{"id":"$TABLE_ID","name":"Stolik 1","number":"1","seats":4}]""")
            }
            dispatcher.on("GET", "/v1/occupied-tables") { jsonResponse("[]") }
            dispatcher.on("POST", "/v1/order/orders") {
                jsonResponse("""{"id":"order-server-1","placeId":"$PLACE_ID","status":"draft","totalGross":0}""")
            }
            dispatcher.on("POST", "/v1/tables/$TABLE_ID/occupy/[^/]+") {
                jsonResponse("""{"tableId":"$TABLE_ID","orderId":"order-server-1","conflict":false}""")
            }
            dispatcher.on("GET", "/v1/pos-menus/current") { jsonResponse("""{"id":1,"name":"Menu"}""") }
            dispatcher.on("GET", "/v1/pos-menus/1/groups") { jsonResponse("[]") }
            dispatcher.on("GET", "/v1/pos-menus/1/items") { jsonResponse("[]") }
            dispatcher.on("GET", "/v1/attributes") { jsonResponse("[]") }

            composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = true) } }

            composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
                composeRule.onAllNodesWithText("Stolik 1").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("/v1/tables", mockWebServer.takeRequest(TIMEOUT_MS, TimeUnit.MILLISECONDS)?.path)
            assertEquals("/v1/occupied-tables", mockWebServer.takeRequest(TIMEOUT_MS, TimeUnit.MILLISECONDS)?.path)

            composeRule.onNodeWithText("Stolik 1").performClick()

            // Optimistic UI: the Menu screen appears immediately, before the offline
            // queue has synced anything over the network.
            composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
                composeRule.onAllNodesWithText("Przejdź do koszyka").fetchSemanticsNodes().isNotEmpty()
            }

            withTimeout(TIMEOUT_MS) { transactionQueue.pendingCount().first { it == 0 } }
            assertEquals(0, transactionQueue.failedCount().first())

            composeRule.onNodeWithText("Przejdź do koszyka").performClick()

            composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
                composeRule.onAllNodesWithText("Koszyk").fetchSemanticsNodes().isNotEmpty()
            }
        }

    private suspend fun drainLeftoverQueue() {
        while (true) {
            val pending = transactionQueue.nextPending() ?: return
            transactionQueue.markSynced(pending.id)
        }
    }

    private companion object {
        const val TABLE_ID = "table-1"
        const val PLACE_ID = "place-1"
        const val TIMEOUT_MS = 15_000L
    }
}
