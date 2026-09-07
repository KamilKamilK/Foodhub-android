package pl.foodhub.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pl.foodhub.pos.core.auth.SessionState
import pl.foodhub.pos.core.designsystem.theme.FoodHubTheme
import pl.foodhub.pos.navigation.FoodHubNavHost
import pl.foodhub.pos.update.AppUpdateHost
import pl.foodhub.pos.update.AppUpdateViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            FoodHubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val session by viewModel.session.collectAsStateWithLifecycle()
                    val isAuthenticated = session is SessionState.Authenticated

                    val updateViewModel: AppUpdateViewModel = hiltViewModel()
                    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

                    // Once per session, right after login -- a nice-to-have check that
                    // must never delay or block a terminal trying to take a sale.
                    LaunchedEffect(isAuthenticated) {
                        if (isAuthenticated) updateViewModel.checkForUpdateOnce()
                    }

                    FoodHubNavHost(
                        startAuthenticated = isAuthenticated,
                    )

                    AppUpdateHost(
                        state = updateState,
                        onInstallRequested = updateViewModel::startUpdate,
                        onDismiss = updateViewModel::dismiss,
                        onInstallLaunched = updateViewModel::installRequestLaunched,
                    )
                }
            }
        }
    }
}
