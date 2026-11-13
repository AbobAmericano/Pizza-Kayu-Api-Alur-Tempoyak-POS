@file:OptIn(ExperimentalMaterial3Api::class)

package com.slicehouse.pos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.PosState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slicehouse.pos.data.SampleData
import com.slicehouse.pos.data.money
import com.slicehouse.pos.data.percentLabel
import com.slicehouse.pos.data.itemCountLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    wide: Boolean,
    onCheckout: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = PosViewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            MenuBrowser(state, viewModel, Modifier.weight(1f).fillMaxHeight())
            Surface(
                modifier = Modifier.width(410.dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                CartPanel(state, viewModel, onCheckout, Modifier.fillMaxSize())
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            MenuBrowser(state, viewModel, Modifier.weight(1f).fillMaxWidth())
            if (state.cart.isNotEmpty()) {
                CompactCartBar(state) { sheetOpen = true }
            }
        }
        if (sheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { sheetOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                CartPanel(
                    state = state,
                    viewModel = viewModel,
                    onCheckout = {
                        sheetOpen = false
                        onCheckout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .imePadding()
                )
            }
        }
    }
}

@Composable
private fun MenuBrowser(state: PosState, viewModel: RegisterViewModel, modifier: Modifier = Modifier) {
    var selectedCategory by rememberSaveable { mutableStateOf(Category.PIZZA) }
    var query by rememberSaveable { mutableStateOf("") }
    var pizzaItem by remember { mutableStateOf<MenuItem?>(null) }

    Column(modifier) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "NEW ORDER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.settings.shopName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Next #${state.nextOrderNumber}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search the menu") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Category.entries.toList(), key = { it.name }) { category ->
                FilterChip(
                    selected = query.isBlank() && category == selectedCategory,
                    onClick = {
                        query = ""
                        selectedCategory = category
                    },
                    label = { Text(category.label) },
                    leadingIcon = { Text(categoryEmoji(category), fontSize = 14.sp) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = query.isBlank() && category == selectedCategory,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val filtered = remember(state.menu, selectedCategory, query) {
            RegisterViewModel.filterMenu(state.menu, selectedCategory, query)
        }

        if (filtered.isEmpty()) {
            EmptyState(
                emoji = "🔎",
                title = "Nothing on the menu",
                subtitle = "Try another category or search term.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { item ->
                    MenuItemCard(item, state.settings.currency) {
                        if (item.isPizza) {
                            pizzaItem = item
                        } else {
                            viewModel.addLine(
                                CartLine(
                                    lineId = "",
                                    itemId = item.id,
                                    name = item.name,
                                    emoji = item.emoji,
                                    unitPrice = item.price,
                                    qty = 1
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    pizzaItem?.let { item ->
        PizzaDialog(
            item = item,
            toppings = state.toppings,
            sizes = SampleData.sizes,
            currency = state.settings.currency,
            onDismiss = { pizzaItem = null },
            onAdd = { line ->
                viewModel.addLine(line)
                pizzaItem = null
            }
        )
    }
}

@Composable
private fun CompactCartBar(state: PosState, onOpen: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🛒", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${itemCountLabel(state.itemCount)} · ${state.orderType.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    money(state.total, state.settings.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Text("View order", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartPanel(
    state: PosState,
    viewModel: RegisterViewModel,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Current order", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (state.cart.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OrderTypeSelector(state.orderType) { viewModel.setOrderType(it) }
        Spacer(Modifier.height(10.dp))

        when (state.orderType) {
            OrderType.DINE_IN -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CartField("Table number", state.table, viewModel::setTable)
                CartField("Phone number (optional)", state.customerPhone, viewModel::setCustomerPhone, KeyboardType.Phone)
            }
            OrderType.TAKEAWAY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CartField("Customer name (optional)", state.customerName, viewModel::setCustomerName)
                CartField("Phone number (optional)", state.customerPhone, viewModel::setCustomerPhone, KeyboardType.Phone)
            }
            OrderType.DELIVERY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CartField("Customer name", state.customerName, viewModel::setCustomerName)
                CartField("Phone number", state.customerPhone, viewModel::setCustomerPhone, KeyboardType.Phone)
                CartField("Delivery address", state.address, viewModel::setAddress)
            }
        }

        if (state.orderType != OrderType.DINE_IN) {
            Spacer(Modifier.height(8.dp))
            CartField("Pickup time", state.pickupTime, viewModel::setPickupTime)
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        if (state.cart.isEmpty()) {
            EmptyState(
                emoji = "🍕",
                title = "No items yet",
                subtitle = "Tap a pizza or add a side to start the order.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.cart, key = { it.lineId }) { line ->
                    CartLineRow(
                        line = line,
                        currency = state.settings.currency,
                        onMinus = { viewModel.changeQty(line.lineId, -1) },
                        onPlus = { viewModel.changeQty(line.lineId, 1) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                SummaryRow("Subtotal", money(state.subtotal, state.settings.currency))
                SummaryRow("Tax (${percentLabel(state.settings.taxBasisPoints)}%)", money(state.tax, state.settings.currency))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        money(state.total, state.settings.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val kitchenTicket = remember(
            state.cart,
            state.nextOrderNumber,
            state.orderType,
            state.table,
            state.customerName,
            state.customerPhone,
            state.address,
            state.orderNote,
            state.pickupTime
        ) { state.kitchenTicket() }
        kitchenTicket?.let { ticket ->
            PrintKitchenButton(
                order = ticket,
                settings = state.settings,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = onCheckout,
            enabled = state.cart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Rounded.ShoppingBag, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.cart.isEmpty()) "Charge" else "Charge ${money(state.total, state.settings.currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CartField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    )
}

@Composable
fun OrderTypeSelector(current: OrderType, onSelect: (OrderType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OrderType.entries.forEach { type ->
            val selected = type == current
            Surface(
                onClick = { onSelect(type) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(type.emoji, fontSize = 15.sp)
                    Text(type.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    currency: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(Modifier.fillMaxWidth().animateContentSize(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(line.emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                line.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (line.modifiers().isNotBlank()) {
                Text(
                    line.modifiers(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                money(line.unitPrice, currency) + " each",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            QtyStepper(line.qty, onMinus, onPlus)
            Spacer(Modifier.height(4.dp))
            Text(
                money(line.lineTotal, currency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
