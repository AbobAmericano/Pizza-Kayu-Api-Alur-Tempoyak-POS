package com.slicehouse.pos.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slicehouse.pos.data.money
import com.slicehouse.pos.data.sync.OnlineOrderDto

@Composable
fun OnlineScreen(
    currency: String,
    onGoToSettings: () -> Unit,
    viewModel: OnlineViewModel = viewModel(factory = PosViewModelFactory)
) {
    val online by viewModel.orders.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirming by remember { mutableStateOf<Pair<OnlineOrderDto, OnlineAction>?>(null) }

    LaunchedEffect(status.connected) { if (status.connected) viewModel.refresh() }

    fun run(order: OnlineOrderDto, action: OnlineAction) {
        viewModel.act(order, action) { problem -> Toast.makeText(context, problem, Toast.LENGTH_LONG).show() }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Text(
                "ONLINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Online orders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Customers order on the web and pay when they pick up. The list updates every 15 seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            online.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        when {
            !status.connected -> Message(
                "Connect this tablet to FireLord to receive online orders.",
                button = "Open Settings",
                onClick = onGoToSettings
            )
            online.orders.isEmpty() -> Message(if (online.loaded) "No online orders right now." else "Loading…")
            else -> LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Waiting ones first, then by pickup time (the server already sorts by pickup).
                val sorted = online.orders.sortedBy { if (it.status == "PENDING") 0 else 1 }
                items(sorted, key = { it.id }) { order ->
                    OnlineOrderCard(order, currency, enabled = busy == null) { action ->
                        if (action.confirm != null) confirming = order to action else run(order, action)
                    }
                }
            }
        }
    }

    confirming?.let { (order, action) ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(action.label) },
            text = { Text(action.confirm.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    run(order, action)
                }) { Text(action.label, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Back") } }
        )
    }
}

@Composable
private fun Message(text: String, button: String? = null, onClick: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌐", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (button != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineOrderCard(
    order: OnlineOrderDto,
    currency: String,
    enabled: Boolean,
    onAction: (OnlineAction) -> Unit
) {
    val waiting = order.status == "PENDING"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (waiting) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.padding(start = 10.dp))
                Text(
                    "Pickup ${order.pickupTime}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (waiting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (waiting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        onlineStatusLabel(order.status),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                "${order.customerName} · ${order.customerPhone}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            order.lines.forEach { line ->
                val extras = listOfNotNull(
                    line.sizeLabel,
                    line.toppings.takeIf { it.isNotEmpty() }?.joinToString(", ") { "+$it" },
                    line.note.takeIf { it.isNotBlank() }?.let { "“$it”" }
                ).joinToString(" · ")
                Text(
                    "${line.qty}× ${line.emoji} ${line.name}" + if (extras.isNotEmpty()) "  ($extras)" else "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (order.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Note: ${order.note}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${money(order.totalSen, currency)} to pay at pickup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                onlineActions(order.status, order.label).forEach { action ->
                    if (action.primary) {
                        Button(onClick = { onAction(action) }, enabled = enabled, shape = RoundedCornerShape(14.dp)) {
                            Text(action.label, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(onClick = { onAction(action) }, enabled = enabled, shape = RoundedCornerShape(14.dp)) {
                            Text(action.label)
                        }
                    }
                }
            }
        }
    }
}
