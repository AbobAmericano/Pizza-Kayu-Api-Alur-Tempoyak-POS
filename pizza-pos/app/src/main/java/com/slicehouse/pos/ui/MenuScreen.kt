package com.slicehouse.pos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Topping
import com.slicehouse.pos.data.itemCountLabel
import com.slicehouse.pos.data.money
import com.slicehouse.pos.data.round2
import java.util.UUID

@Composable
fun MenuScreen(state: PosState, store: PosStore) {
    var itemDraft by remember { mutableStateOf<MenuItem?>(null) }
    var toppingDraft by remember { mutableStateOf<Topping?>(null) }
    var showToppings by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Text(
                "MENU",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Menu manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${itemCountLabel(state.menu.size)} · ${state.menu.count { it.available }} available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { showToppings = true },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Toppings") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        itemDraft = MenuItem(
                            id = UUID.randomUUID().toString(),
                            name = "",
                            category = Category.PIZZA,
                            price = 0.0,
                            isPizza = true,
                            emoji = "🍕"
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add item")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Category.entries.forEach { category ->
                val categoryItems = state.menu.filter { it.category == category }
                if (categoryItems.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoryEmoji(category),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                category.label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${categoryItems.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(categoryItems, key = { it.id }) { menuItem ->
                        MenuItemRow(
                            item = menuItem,
                            currency = state.settings.currency,
                            onToggle = { store.toggleAvailable(menuItem.id) },
                            onEdit = { itemDraft = menuItem }
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Unavailable items stay in the menu but are hidden from the register.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    itemDraft?.let { draft ->
        MenuItemEditor(
            initial = draft,
            currency = state.settings.currency,
            isNew = state.menu.none { it.id == draft.id },
            onDismiss = { itemDraft = null },
            onSave = {
                store.saveMenuItem(it)
                itemDraft = null
            },
            onDelete = {
                store.deleteMenuItem(draft.id)
                itemDraft = null
            }
        )
    }

    if (showToppings) {
        ToppingsManager(
            toppings = state.toppings,
            currency = state.settings.currency,
            onDismiss = { showToppings = false },
            onEdit = { toppingDraft = it },
            onAdd = {
                toppingDraft = Topping(id = UUID.randomUUID().toString(), label = "", price = 1.0)
            }
        )
    }

    toppingDraft?.let { draft ->
        ToppingEditor(
            initial = draft,
            currency = state.settings.currency,
            isNew = state.toppings.none { it.id == draft.id },
            onDismiss = { toppingDraft = null },
            onSave = {
                store.saveTopping(it)
                toppingDraft = null
            },
            onDelete = {
                store.deleteTopping(draft.id)
                toppingDraft = null
            }
        )
    }
}

@Composable
private fun MenuItemRow(
    item: MenuItem,
    currency: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val photo = menuImageRes(item.imageKey)
            Box(
                Modifier.size(42.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (photo != null) {
                    Image(
                        painter = painterResource(photo),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = categoryTint(item.category),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(item.emoji, fontSize = 20.sp) }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${money(item.price, currency)} · ${item.category.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = item.available, onCheckedChange = { onToggle() })
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${item.name}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuItemEditor(
    initial: MenuItem,
    currency: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var description by remember { mutableStateOf(initial.description) }
    var imageKey by remember { mutableStateOf(initial.imageKey) }
    var priceText by remember { mutableStateOf(if (initial.price > 0) trimNumber(initial.price) else "") }
    var category by remember { mutableStateOf(initial.category) }
    var isPizza by remember { mutableStateOf(initial.isPizza) }
    var available by remember { mutableStateOf(initial.available) }
    var twelveInch by remember { mutableStateOf(initial.twelveInchAvailable) }
    var price12Text by remember { mutableStateOf(if (initial.twelveInchAvailable) trimNumber(initial.price + initial.twelveInchDelta) else "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val price = priceText.toDoubleOrNull()
    val price12 = price12Text.toDoubleOrNull()
    val valid = name.isNotBlank() && price != null && price > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isNew) "New menu item" else "Edit item",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if (it.length <= 4) emoji = it },
                            label = { Text("Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(84.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Item name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { text ->
                            priceText = text.replace(',', '.').filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Price") },
                        prefix = { Text(currency) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    SectionLabel("Photo")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (imageKey.isBlank()) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(14.dp)
                                        )
                                    } else Modifier
                                )
                                .clickable { imageKey = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("None", style = MaterialTheme.typography.labelSmall)
                        }
                        menuPhotoOptions.forEach { option ->
                            val res = menuImageRes(option.key) ?: return@forEach
                            Image(
                                painter = painterResource(res),
                                contentDescription = option.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .then(
                                        if (imageKey == option.key) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(14.dp)
                                            )
                                        } else Modifier
                                    )
                                    .clickable { imageKey = option.key }
                            )
                        }
                    }

                    SectionLabel("Category")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Category.entries.forEach { option ->
                            FilterChip(
                                selected = option == category,
                                onClick = { category = option },
                                label = { Text(option.label) },
                                leadingIcon = { Text(categoryEmoji(option), fontSize = 13.sp) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (isPizza) {
                        ToggleRow(
                            title = "Available in 12 inch",
                            subtitle = "This pizza can be ordered in the 12 inch size.",
                            checked = twelveInch,
                            onChecked = { twelveInch = it }
                        )
                        if (twelveInch) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = price12Text,
                                onValueChange = { price12Text = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("12 inch price") },
                                prefix = { Text(currency) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ToggleRow(
                        title = "Build your own",
                        subtitle = "Opens size and topping options on the register.",
                        checked = isPizza,
                        onChecked = { isPizza = it }
                    )
                    ToggleRow(
                        title = "Available",
                        subtitle = "Show this item on the register.",
                        checked = available,
                        onChecked = { available = it }
                    )

                    if (confirmDelete) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "Delete ${initial.name}?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = onDelete,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Delete") }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!isNew) {
                            OutlinedButton(
                                onClick = { confirmDelete = true },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onSave(
                                    initial.copy(
                                        name = name.trim(),
                                        emoji = emoji.ifBlank { "🍕" },
                                        description = description.trim(),
                                        price = price ?: 0.0,
                                        category = category,
                                        isPizza = isPizza,
                                        available = available,
                                        imageKey = imageKey,
                                        twelveInchAvailable = if (isPizza) twelveInch else false,
                                        twelveInchDelta = if (twelveInch && price12 != null) round2(price12 - (price ?: 0.0)) else initial.twelveInchDelta
                                    )
                                )
                            },
                            enabled = valid,
                            modifier = Modifier.weight(1.2f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(if (isNew) "Add item" else "Save", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ToppingsManager(
    toppings: List<Topping>,
    currency: String,
    onDismiss: () -> Unit,
    onEdit: (Topping) -> Unit,
    onAdd: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Extra toppings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${toppings.size} options offered on pizzas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(toppings, key = { it.id }) { topping ->
                            Surface(
                                onClick = { onEdit(topping) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(topping.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        topping.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "+${money(topping.price, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add topping", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToppingEditor(
    initial: Topping,
    currency: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Topping) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(initial.label) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var priceText by remember { mutableStateOf(trimNumber(initial.price)) }
    val price = priceText.toDoubleOrNull()
    val valid = label.isNotBlank() && price != null && price >= 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        if (isNew) "New topping" else "Edit topping",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if (it.length <= 4) emoji = it },
                            label = { Text("Icon") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(84.dp)
                        )
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { text ->
                            priceText = text.replace(',', '.').filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Extra price") },
                        prefix = { Text(currency) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!isNew) {
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete")
                            }
                        }
                        Button(
                            onClick = {
                                onSave(
                                    initial.copy(
                                        label = label.trim(),
                                        emoji = emoji.ifBlank { "🍕" },
                                        price = price ?: 0.0
                                    )
                                )
                            },
                            enabled = valid,
                            modifier = Modifier.weight(1.4f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
