package com.slicehouse.pos.data.sync

import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.OrderStatus
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.Topping
import com.slicehouse.pos.data.db.OrderEntity
import com.slicehouse.pos.data.db.OrderLineEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

// Converting between the tablet's data and the server's JSON. Text is cut to
// the server's limits, so one long note can't block an order from uploading.

fun MenuItem.toDto() = MenuItemDto(
    id = id,
    name = name.take(100),
    description = description.take(300),
    emoji = emoji.take(16),
    category = category.name,
    priceSen = price,
    available = available,
    isPizza = isPizza,
    imageKey = imageKey.take(64),
    twelveInchAvailable = twelveInchAvailable,
    twelveInchDeltaSen = twelveInchDelta
)

fun MenuItemDto.toModel() = MenuItem(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    category = Category.entries.firstOrNull { it.name == category } ?: Category.PIZZA,
    price = priceSen,
    available = available,
    isPizza = isPizza,
    imageKey = imageKey,
    twelveInchAvailable = twelveInchAvailable,
    twelveInchDelta = twelveInchDeltaSen
)

fun Topping.toDto() = ToppingDto(id = id, label = label.take(60), emoji = emoji.take(16), priceSen = price)

fun ToppingDto.toModel() = Topping(id = id, label = label, emoji = emoji, price = priceSen)

/**
 * The id the server knows this order by. The server needs a UUID; orders made
 * by this app already have one. Any older id gets a fixed UUID made from it,
 * so the same order always maps to the same server id.
 */
fun serverOrderId(id: String): String =
    runCatching { UUID.fromString(id).toString() }.getOrNull()
        ?: UUID.nameUUIDFromBytes("pizza-pos-order:$id".toByteArray()).toString()

fun isoTime(millis: Long): String = Instant.ofEpochMilli(millis).toString()

fun OrderEntity.toDto(lines: List<OrderLineEntity>): OrderDto {
    val refunded = status == OrderStatus.REFUNDED
    return OrderDto(
        id = serverOrderId(id),
        number = number.coerceAtLeast(1),
        createdAt = isoTime(createdAt),
        type = type.name,
        table = tableLabel.take(20),
        customerName = customerName.take(100),
        customerPhone = customerPhone.take(30),
        address = address.take(300),
        note = note.take(300),
        pickupTime = pickupTime.take(20),
        subtotalSen = subtotalSen,
        taxSen = taxSen,
        totalSen = totalSen,
        payment = payment.name,
        cashReceivedSen = cashReceivedSen.coerceAtLeast(0),
        changeSen = changeSen.coerceAtLeast(0),
        status = status.name,
        takenBy = takenBy.take(60),
        refundedAt = if (refunded) isoTime(refundedAt ?: createdAt) else null,
        refundedBy = if (refunded) refundedBy?.take(60) else null,
        lines = lines.sortedBy { it.position }.map { line ->
            OrderLineDto(
                lineId = line.lineId.take(64),
                itemId = line.itemId.take(64),
                name = line.name.take(100).ifBlank { "Item" },
                emoji = line.emoji.take(16),
                unitPriceSen = line.unitPriceSen,
                qty = line.qty,
                sizeLabel = line.sizeLabel?.take(20),
                crustLabel = line.crustLabel?.take(40),
                toppings = line.toppings.take(30),
                note = line.note.take(300)
            )
        }
    )
}

fun parseTime(value: String?): Long? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

/** A picked-up online order as a tablet [Order], so it shows in Orders and the day's totals. */
fun OnlineOrderDto.toOrder(fallbackTime: Long): Order {
    val paid = PaymentMethod.entries.firstOrNull { it.name == payment } ?: PaymentMethod.CASH
    return Order(
        id = id,
        number = number,
        createdAt = parseTime(createdAt) ?: fallbackTime,
        lines = lines.map { line ->
            CartLine(
                lineId = line.lineId, itemId = line.itemId, name = line.name, emoji = line.emoji,
                unitPrice = line.unitPriceSen, qty = line.qty, sizeLabel = line.sizeLabel,
                crustLabel = line.crustLabel, toppings = line.toppings, note = line.note
            )
        },
        type = OrderType.TAKEAWAY,
        customerName = customerName,
        customerPhone = customerPhone,
        subtotal = subtotalSen,
        tax = taxSen,
        total = totalSen,
        payment = paid,
        cashReceived = if (paid == PaymentMethod.CASH) totalSen else 0,
        change = 0,
        status = OrderStatus.COMPLETED,
        note = note,
        pickupTime = pickupTime,
        online = true
    )
}
