package pl.foodhub.pos.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.foodhub.pos.core.designsystem.component.PrimaryButton

@Composable
fun MenuBrowseRoute(
    onOpenCart: () -> Unit,
    viewModel: MenuBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MenuBrowseScreen(state = state, onOpenCart = onOpenCart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MenuBrowseScreen(
    state: MenuBrowseUiState,
    onOpenCart: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (state.stale) "Menu (dane offline)" else "Menu") })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.weight(1f).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.menu.items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                item.unitPriceGross.formatPln(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            PrimaryButton(
                text = "Przejdź do koszyka",
                onClick = onOpenCart,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
