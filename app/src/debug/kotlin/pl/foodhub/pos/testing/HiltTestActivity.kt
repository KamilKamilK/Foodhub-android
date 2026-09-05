package pl.foodhub.pos.testing

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Minimal Hilt entry-point activity instrumented tests set Compose content on directly
 * (`composeRule.setContent { ... }`), instead of launching the real `MainActivity` through
 * an intent. `hiltViewModel()` inside a composable requires its host activity to be a Hilt
 * entry point, which a plain `androidx.activity.ComponentActivity` is not -- this one is,
 * and setting content explicitly inside a test method (after `@Before` has started the
 * fake backend and seeded any session state) avoids racing the real app's own launch
 * sequence.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
