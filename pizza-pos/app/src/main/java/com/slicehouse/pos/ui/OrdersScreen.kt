package com.slicehouse.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.OrderStatus
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.formatClock
import com.slicehouse.pos.data.itemCountLabel
import com.slicehouse.pos.data.money
import java.util.Calendar

@Composable
fun OrdersScreen(
    state: PosState,
    store: PosStore,
    onGoToRegister: () -> Unit
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayOrders = remember(state.orders, startOfToday) {
        state.orders.filter { it.createdAt >= startOfToday }
    }
    val visible = if (showAll) state.orders else todayOrders
    val salesToday = todayOrders.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.total }
    val avgTicket = if (todayOrders.isEmpty()) 0.0 else salesToday / todayOrders.count { it.status == OrderStatus.COMPLETED }.coerceAtLeast(1)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Text(
                "ORDERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Order history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "Sales today",
                    value = money(salesToday, state.settings.currency),
                    emoji = "💰",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Orders today",
                    value = todayOrders.size.toString(),
                    emoji = "🧾",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Avg ticket",
                    value = money(avgTicket, state.settings.currency),
                    emoji = "📈",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showAll,
                    onClick = { showAll = false },
                    label = { Text("Today · ${todayOrders.size}") },
                    shape = RoundedCornerShape(50)
                )
                FilterChip(
                    selected = showAll,
                    onClick = { showAll = true },
                    label = { Text("All · ${state.orders.size}") },
                    shape = RoundedCornerShape(50)
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        if (visible.isEmpty()) {
            EmptyState(
                emoji = "🧾",
                title = if (showAll) "No orders yet" else "No orders today",
                subtitle = "Completed sales will show up here with a full receipt.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible, key = { it.id }) { order ->
                    OrderCard(order, state.settings.currency) { selectedId = order.id }
                }
            }
        }
    }

    selectedId?.let { id ->
        val live = state.orders.firstOrNull { it.id == id }
        if (live != null) {
            OrderDetailDialog(
                order = live,
                settings = state.settings,
                onDismiss = { selectedId = null },
                onReorder = {
                    store.reorder(live)
                    selectedId = null
                    onGoToRegister()
                },
                onRefund = { store.refund(live.id) }
            )
        } else {
            selectedId = null
        }
    }
}

@Composable
private fun OrderCard(order: Order, currency: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${order.number}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (order.status == OrderStatus.REFUNDED) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                "Refunded",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatClock(order.createdAt)} · ${itemCountLabel(order.itemCount)} · ${order.payment.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OrderTypeBadge(order.type)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    money(order.total, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (order.status == OrderStatus.REFUNDED) "—" else "View",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun OrderDetailDialog(
    order: Order,
    settings: Settings,
    onDismiss: () -> Unit,
    onReorder: () -> Unit,
    onRefund: () -> Unit
) {
    var confirmRefund by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Order #${order.number}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (order.status == OrderStatus.REFUNDED) "Refunded" else "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (order.status == OrderStatus.REFUNDED) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        ReceiptBody(order, settings, Modifier.padding(16.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    PrintReceiptButton(
                        order = order,
                        settings = settings,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        filled = true
                    )
                    Spacer(Modifier.height(10.dp))

                    if (confirmRefund) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "Refund ${money(order.total, settings.currency)}?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "This marks the order as refunded. It does not move money.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { confirmRefund = false }, modifier = Modifier.weight(1f)) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            onRefund()
                                            confirmRefund = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Refund") }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (order.status == OrderStatus.COMPLETED) {
                            OutlinedButton(
                                onClick = { confirmRefund = true },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refund")
                            }
                        }
                        Button(
                            onClick = onReorder,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reorder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
