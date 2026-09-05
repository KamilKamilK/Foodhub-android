package pl.foodhub.pos.feature.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.foodhub.pos.core.designsystem.component.PrimaryButton

@Composable
fun CartRoute(
    onCheckoutComplete: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products by viewModel.availableProducts.collectAsStateWithLifecycle()

    LaunchedEffect(state.completedDocumentId) {
        if (state.completedDocumentId != null) onCheckoutComplete()
    }

    CartScreen(
        state = state,
        products = products,
        onAdd = viewModel::addProduct,
        onRemove = viewModel::removeLine,
        onInvoiceRequestedChange = viewModel::setInvoiceRequested,
        onBuyerNameChange = viewModel::onBuyerNameChange,
        onBuyerNipChange = viewModel::onBuyerNipChange,
        onToggleAttributeValue = viewModel::toggleAttributeValue,
        onCheckout = viewModel::checkout,
    )
}

@Composable
internal fun CartScreen(
    state: CartUiState,
    products: List<PickerProduct>,
    onAdd: (PickerProduct) -> Unit,
    onRemove: (String) -> Unit,
    onInvoiceRequestedChange: (Boolean) -> Unit,
    onBuyerNameChange: (String) -> Unit,
    onBuyerNipChange: (String) -> Unit,
    onToggleAttributeValue: (Int) -> Unit,
    onCheckout: () -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Produkty", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(products, key = { it.productId }) { product ->
                    Surface(
                        onClick = { onAdd(product) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(product.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                product.unitPriceGross.formatPln(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outline),
        )

        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Koszyk", style = MaterialTheme.typography.titleLarge)
            state.lines.forEach { line ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${line.quantity}×  ${line.productName}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(line.lineGross.formatPln(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemove(line.productId) }) {
                        Text("Usuń", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            HorizontalDivider()
            Text(
                "Razem: ${state.total.formatPln()}",
                style = MaterialTheme.typography.titleMedium,
            )

            state.availableAttributes.forEach { attribute ->
                Text(attribute.name, style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    attribute.values.forEach { value ->
                        FilterChip(
                            selected = value.id in state.selectedAttributeValueIds,
                            onClick = { onToggleAttributeValue(value.id) },
                            label = { Text(value.name) },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = state.invoiceRequested, onCheckedChange = onInvoiceRequestedChange)
                Text("Wystaw fakturę", Modifier.padding(start = 8.dp))
            }
            if (state.invoiceRequested) {
                OutlinedTextField(
                    value = state.buyerName,
                    onValueChange = onBuyerNameChange,
                    label = { Text("Nabywca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.buyerNip,
                    onValueChange = onBuyerNipChange,
                    label = { Text("NIP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.error) {
                Text("Nie udało się wystawić dokumentu.", color = MaterialTheme.colorScheme.error)
            }
            if (state.submitting) {
                CircularProgressIndicator()
            } else {
                PrimaryButton(
                    text = if (state.invoiceRequested) "Wystaw fakturę" else "Wystaw paragon",
                    onClick = onCheckout,
                    enabled = state.canCheckout,
                )
            }
        }
    }
}
