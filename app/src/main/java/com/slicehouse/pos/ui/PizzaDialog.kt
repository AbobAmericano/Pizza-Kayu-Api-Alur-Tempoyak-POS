package com.slicehouse.pos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.SizeOption
import com.slicehouse.pos.data.Topping
import com.slicehouse.pos.data.money
import com.slicehouse.pos.data.Sen
import com.slicehouse.pos.data.parseMoney
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PizzaDialog(
    item: MenuItem,
    toppings: List<Topping>,
    sizes: List<SizeOption>,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (CartLine) -> Unit
) {
    val availableSizes = if (item.twelveInchAvailable) sizes else sizes.filter { it.id != "s12" }
    var sizeId by remember { mutableStateOf(availableSizes.first().id) }
    var selectedToppings by remember { mutableStateOf(setOf<String>()) }
    var qty by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf<Topping?>(null) }
    var customName by remember { mutableStateOf("") }
    var customPriceText by remember { mutableStateOf("") }
    var customEditorOpen by remember { mutableStateOf(false) }

    val size = availableSizes.firstOrNull { it.id == sizeId } ?: availableSizes.first()
    val sizeDelta = if (size.id == "s12") item.twelveInchDelta else size.delta
    val chosen = selectedToppings.mapNotNull { key ->
        when {
            key.startsWith("custom-") -> custom?.takeIf { it.id == key }?.takeIf { it.label.isNotBlank() }
            else -> toppings.firstOrNull { it.id == key }
        }
    }
    val unitPrice = item.price + sizeDelta + chosen.sumOf { it.price }
    val total = unitPrice * qty

    fun applyCustom(name: String, price: Sen) {
        val key = custom?.id ?: return
        if (name.isBlank() || price < 0) {
            custom = null
            selectedToppings = selectedToppings.filterNot { it == key }.toSet()
        } else {
            custom = custom?.copy(label = name.trim(), price = price)
        }
    }

    fun cancelCustom() {
        val key = custom?.id ?: return
        custom = null
        selectedToppings = selectedToppings.filterNot { it == key }.toSet()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp
            ) {
                if (!customEditorOpen) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photo = menuImageRes(item.imageKey)
                        Box(
                            Modifier.size(58.dp).clip(CircleShape)
                                .background(categoryTint(item.category)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photo != null) {
                                Image(
                                    painter = painterResource(photo),
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(item.emoji, fontSize = 30.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${money(item.price, currency)} base · ${item.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    SectionLabel("Choose a size")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableSizes.forEach { option ->
                            val selected = option.id == sizeId
                            val optionDelta = if (option.id == "s12") item.twelveInchDelta else option.delta
                            Surface(
                                onClick = { sizeId = option.id },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        option.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        money(item.price + optionDelta, currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    SectionLabel("Extra toppings")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = custom != null,
                            onClick = {
                                val key = "custom-${UUID.randomUUID()}"
                                custom = Topping(id = key, label = "", emoji = "\uD83E\uDDC0", price = 0)
                                customName = ""
                                customPriceText = ""
                                selectedToppings = selectedToppings + key
                                customEditorOpen = true
                            },
                            label = { Text("🥑 Custom topping") },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        toppings.forEach { topping ->
                            val selected = topping.id in selectedToppings
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedToppings = if (selected) selectedToppings - topping.id
                                    else selectedToppings + topping.id
                                },
                                label = { Text("${topping.emoji} ${topping.label}") },
                                trailingIcon = {
                                    Text(
                                        "+${money(topping.price, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    SectionLabel("Kitchen note")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("e.g. well done, no basil") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QtyStepper(qty, { if (qty > 1) qty-- }, { qty++ })
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Item total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                money(total, currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onAdd(
                                    CartLine(
                                        lineId = "",
                                        itemId = item.id,
                                        name = item.name,
                                        emoji = item.emoji,
                                        unitPrice = unitPrice,
                                        qty = qty,
                                        sizeLabel = size.label,
                                        toppings = chosen.map { it.label },
                                        note = note.trim()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1.4f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Add · ${money(total, currency)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Custom topping",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Use any topping this pizza does not list yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = { cancelCustom(); customEditorOpen = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Topping name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customPriceText,
                            onValueChange = { text ->
                                customPriceText = text.replace(',', '.').filter { it.isDigit() || it == '.' }
                            },
                            label = { Text("Extra price") },
                            prefix = { Text(currency) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { cancelCustom(); customEditorOpen = false },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Cancel") }
                            Button(
                                onClick = { applyCustom(customName, parseMoney(customPriceText) ?: 0L); customEditorOpen = false },
                                modifier = Modifier.weight(1.4f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Add topping", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
