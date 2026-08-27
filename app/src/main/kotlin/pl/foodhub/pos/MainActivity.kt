package pl.foodhub.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pl.foodhub.pos.core.auth.SessionState
import pl.foodhub.pos.core.designsystem.theme.FoodHubTheme
import pl.foodhub.pos.navigation.FoodHubNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            FoodHubTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val session by viewModel.session.collectAsStateWithLifecycle()

                FoodHubNavHost(
                    startAuthenticated = session is SessionState.Authenticated,
                )
            }
        }
    }
}
