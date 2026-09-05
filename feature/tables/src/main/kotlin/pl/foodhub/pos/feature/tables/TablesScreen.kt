package pl.foodhub.pos.feature.tables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TablesRoute(
    onOpenMenu: (orderId: String, tableId: String) -> Unit,
    viewModel: TablesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.openedTable.collect { session -> onOpenMenu(session.orderId, session.tableId) }
    }

    TablesScreen(state = state, onSelectTable = viewModel::selectTable)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TablesScreen(
    state: TablesUiState,
    onSelectTable: (TableRow) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Wybierz stolik", style = MaterialTheme.typography.headlineSmall)

        when {
            state.loading ->
                CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            state.error ->
                Text("Nie udało się wczytać stolików.", color = MaterialTheme.colorScheme.error)
            else ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    items(state.tables, key = { it.id }) { table ->
                        val danger = MaterialTheme.colorScheme.error
                        Card(
                            onClick = { onSelectTable(table) },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                if (table.occupied) {
                                    CardDefaults.cardColors(containerColor = danger.copy(alpha = 0.08f))
                                } else {
                                    CardDefaults.cardColors()
                                },
                            border =
                                BorderStroke(
                                    width = 1.dp,
                                    color =
                                        if (table.occupied) {
                                            danger.copy(alpha = 0.35f)
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                ),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(table.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${table.seats} miejsc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (table.occupied) {
                                    Text(
                                        "Zajęty",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = danger,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
