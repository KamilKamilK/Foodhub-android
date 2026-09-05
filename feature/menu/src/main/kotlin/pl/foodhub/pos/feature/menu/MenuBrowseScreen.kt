package pl.foodhub.pos.feature.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.foodhub.pos.core.designsystem.component.PrimaryButton
import pl.foodhub.pos.core.designsystem.theme.FoodHubWarning

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
            TopAppBar(
                title = {
                    Text(
                        if (state.stale) "Menu (dane offline)" else "Menu",
                        color = if (state.stale) FoodHubWarning else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.emptyOffline) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Brak danych menu — połącz się z internetem, aby pobrać menu po raz pierwszy.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                val products = state.menu.items.filter { it.productId.isNotBlank() }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.weight(1f).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(products, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.unitPriceGross.formatPln(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
