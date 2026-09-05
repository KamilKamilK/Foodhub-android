package pl.foodhub.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.foodhub.pos.core.auth.TokenStore
import pl.foodhub.pos.core.designsystem.theme.FoodHubTheme
import pl.foodhub.pos.navigation.FoodHubNavHost
import pl.foodhub.pos.testing.HiltTestActivity
import pl.foodhub.pos.testing.RoutedDispatcher
import pl.foodhub.pos.testing.fakeJwt
import pl.foodhub.pos.testing.jsonResponse
import pl.foodhub.pos.testing.startOnLoopback
import javax.inject.Inject

/**
 * Covers the terminal's only login path (PIN entry on a real Compose layout) end to end
 * against a hermetic fake `foodhub-api`, and the smoke coverage `testDebugUnitTest`
 * structurally cannot provide: a real device pass through `PinPad` inside the login
 * screen's actual layout constraints.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PinLoginFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var mockWebServer: MockWebServer

    @Inject
    lateinit var tokenStore: TokenStore

    private val dispatcher = RoutedDispatcher()

    @Before
    fun setUp() {
        hiltRule.inject()
        tokenStore.clear()
        mockWebServer.dispatcher = dispatcher
        mockWebServer.startOnLoopback()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun pinLoginScreen_rendersWithoutCrashing() {
        composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = false) } }

        composeRule.onNodeWithText("Zaloguj się PIN-em").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("Zaloguj").assertIsDisplayed()
    }

    @Test
    fun successfulPinLogin_navigatesToTablesScreen() {
        dispatcher.on("POST", "/v1/auth/pos-login") {
            jsonResponse("""{"token":"${fakeJwt(placeId = PLACE_ID)}","refreshToken":"refresh-1"}""")
        }
        dispatcher.on("GET", "/v1/tables") { jsonResponse("[]") }
        dispatcher.on("GET", "/v1/occupied-tables") { jsonResponse("[]") }

        composeRule.setContent { FoodHubTheme { FoodHubNavHost(startAuthenticated = false) } }

        enterPin("1234")
        composeRule.onNodeWithText("Zaloguj").performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Wybierz stolik").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun enterPin(pin: String) {
        pin.forEach { digit -> composeRule.onNodeWithText(digit.toString()).performClick() }
    }

    private companion object {
        const val PLACE_ID = "place-1"
        const val TIMEOUT_MS = 10_000L
    }
}
