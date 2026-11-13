package com.slicehouse.pos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.compactUrl
import com.slicehouse.pos.data.formatDateTime
import com.slicehouse.pos.data.instagramHandle
import com.slicehouse.pos.data.money

@Composable
fun categoryTint(category: Category): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) {
        when (category) {
            Category.PIZZA -> Color(0xFF3A201C)
            Category.SIDES -> Color(0xFF332A16)
            Category.DRINKS -> Color(0xFF172A38)
            Category.DESSERTS -> Color(0xFF331C28)
        }
    } else {
        when (category) {
            Category.PIZZA -> Color(0xFFFFE8E2)
            Category.SIDES -> Color(0xFFFFF1D5)
            Category.DRINKS -> Color(0xFFE2EFFA)
            Category.DESSERTS -> Color(0xFFFBE3EF)
        }
    }
}

fun categoryEmoji(category: Category): String = when (category) {
    Category.PIZZA -> "\uD83C\uDF55"
    Category.SIDES -> "\uD83C\uDF5F"
    Category.DRINKS -> "\uD83E\uDD64"
    Category.DESSERTS -> "\uD83C\uDF70"
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val photo = menuImageRes(item.imageKey)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = categoryTint(item.category),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {
            if (photo != null) {
                Box(Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(photo),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(96.dp)
                    )
                    ItemActionBadge(
                        item = item,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }
            }
            Column(Modifier.padding(14.dp)) {
                if (photo == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.emoji, fontSize = 24.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        ItemActionBadge(item)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        money(item.price, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    if (item.isPizza) {
                        Text(
                            "Customize",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemActionBadge(item: MenuItem, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
        Icon(
            imageVector = if (item.isPizza) Icons.Rounded.Tune else Icons.Rounded.Add,
            contentDescription = if (item.isPizza) "Customize" else "Add",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(5.dp).size(16.dp)
        )
    }
}

@Composable
fun QtyStepper(
    qty: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Remove, "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                qty.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Add, "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 52.sp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderTypeBadge(type: OrderType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            "${type.emoji} ${type.label}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReceiptBody(order: Order, settings: Settings, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            settings.shopName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            settings.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            settings.phone,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        ReceiptRow("Order", "#${order.number}")
        ReceiptRow("Date", formatDateTime(order.createdAt))
        ReceiptRow("Type", "${order.type.label}${if (order.table.isNotBlank()) " · Table ${order.table}" else ""}")
        if (order.pickupTime.isNotBlank()) ReceiptRow("Pickup", order.pickupTime)
        if (order.customerName.isNotBlank()) ReceiptRow("Customer", order.customerName)
        if (order.customerPhone.isNotBlank()) ReceiptRow("Phone", order.customerPhone)
        if (order.address.isNotBlank()) ReceiptRow("Address", order.address)
        if (order.takenBy.isNotBlank()) ReceiptRow("Staff", order.takenBy)
        order.refundedBy?.let { ReceiptRow("Refund approved by", it) }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))

        order.lines.forEach { line ->
            val label = if (line.modifiers().isNotBlank()) "${line.qty}×  ${line.name} ${line.modifiers()}" else "${line.qty}×  ${line.name}"
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    money(line.lineTotal, settings.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        ReceiptRow("Subtotal", money(order.subtotal, settings.currency), mono = true)
        ReceiptRow("Tax", money(order.tax, settings.currency), mono = true)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                money(order.total, settings.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        ReceiptRow(order.payment.label, money(if (order.payment.name == "CASH") order.cashReceived else order.total, settings.currency), mono = true)
        if (order.payment.name == "CASH" && order.change > 0) {
            ReceiptRow("Change", money(order.change, settings.currency), mono = true)
        }
        if (order.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Note: ${order.note}", style = MaterialTheme.typography.bodySmall)
        }
        if (settings.instagram.isNotBlank() || settings.tiktok.isNotBlank() || settings.locationUrl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            if (settings.instagram.isNotBlank()) ReceiptRow("Instagram", instagramHandle(settings.instagram))
            if (settings.tiktok.isNotBlank()) ReceiptRow("TikTok", settings.tiktok)
            if (settings.locationUrl.isNotBlank()) ReceiptRow("Maps", compactUrl(settings.locationUrl))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            settings.footer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}

fun buildReceiptText(order: Order, settings: Settings): String = buildString {
    appendLine(settings.shopName)
    appendLine(settings.address)
    appendLine(settings.phone)
    appendLine("--------------------------------")
    appendLine("Order #${order.number}  ·  ${formatDateTime(order.createdAt)}")
    appendLine("${order.type.label}${if (order.table.isNotBlank()) " · Table ${order.table}" else ""}")
    if (order.pickupTime.isNotBlank()) appendLine("Pickup: ${order.pickupTime}")
    if (order.customerName.isNotBlank()) appendLine("Customer: ${order.customerName}")
    if (order.customerPhone.isNotBlank()) appendLine("Phone: ${order.customerPhone}")
    if (order.address.isNotBlank()) appendLine("Address: ${order.address}")
    appendLine("--------------------------------")
    order.lines.forEach { line ->
        val label = if (line.modifiers().isNotBlank()) "${line.qty}× ${line.name} ${line.modifiers()}" else "${line.qty}× ${line.name}"
        appendLine("${label}  ${money(line.lineTotal, settings.currency)}")
    }
    appendLine("--------------------------------")
    appendLine("Subtotal  ${money(order.subtotal, settings.currency)}")
    appendLine("Tax       ${money(order.tax, settings.currency)}")
    appendLine("TOTAL     ${money(order.total, settings.currency)}")
    appendLine("Paid by ${order.payment.label}")
    if (order.payment.name == "CASH" && order.change > 0) {
        appendLine("Change    ${money(order.change, settings.currency)}")
    }
    if (settings.instagram.isNotBlank()) appendLine("Instagram: ${instagramHandle(settings.instagram)}")
    if (settings.tiktok.isNotBlank()) appendLine("TikTok: ${settings.tiktok}")
    if (settings.locationUrl.isNotBlank()) appendLine("Maps: ${compactUrl(settings.locationUrl)}")
    appendLine()
    appendLine(settings.footer)
}
