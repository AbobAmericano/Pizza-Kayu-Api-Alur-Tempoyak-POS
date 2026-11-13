package com.slicehouse.pos.data.db

import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.Topping

// Converts between database rows and the models the screens use.

fun MenuItem.toEntity(position: Int) = MenuItemEntity(
    id = id,
    position = position,
    name = name,
    description = description,
    emoji = emoji,
    category = category,
    priceSen = price,
    available = available,
    isPizza = isPizza,
    imageKey = imageKey,
    twelveInchAvailable = twelveInchAvailable,
    twelveInchDeltaSen = twelveInchDelta
)

fun MenuItemEntity.toModel() = MenuItem(
    id = id,
    name = name,
    description = description,
    emoji = emoji,
    category = category,
    price = priceSen,
    available = available,
    isPizza = isPizza,
    imageKey = imageKey,
    twelveInchAvailable = twelveInchAvailable,
    twelveInchDelta = twelveInchDeltaSen
)

fun List<MenuItem>.toMenuEntities() = mapIndexed { index, item -> item.toEntity(index) }

fun Topping.toEntity(position: Int) = ToppingEntity(
    id = id,
    position = position,
    label = label,
    emoji = emoji,
    priceSen = price
)

fun ToppingEntity.toModel() = Topping(id = id, label = label, emoji = emoji, price = priceSen)

fun List<Topping>.toToppingEntities() = mapIndexed { index, topping -> topping.toEntity(index) }

fun Settings.toEntity() = ShopSettingsEntity(
    shopName = shopName,
    taxBasisPoints = taxBasisPoints,
    currency = currency,
    phone = phone,
    address = address,
    footer = footer,
    instagram = instagram,
    tiktok = tiktok,
    locationUrl = locationUrl,
    themeMode = themeMode,
    printerAddress = printerAddress,
    printerName = printerName,
    paperWidthMm = paperWidthMm
)

fun ShopSettingsEntity.toModel() = Settings(
    shopName = shopName,
    taxBasisPoints = taxBasisPoints,
    currency = currency,
    phone = phone,
    address = address,
    footer = footer,
    instagram = instagram,
    tiktok = tiktok,
    locationUrl = locationUrl,
    themeMode = themeMode,
    printerAddress = printerAddress,
    printerName = printerName,
    paperWidthMm = paperWidthMm
)

fun Order.toEntity() = OrderEntity(
    id = id,
    number = number,
    createdAt = createdAt,
    type = type,
    tableLabel = table,
    customerName = customerName,
    customerPhone = customerPhone,
    address = address,
    subtotalSen = subtotal,
    taxSen = tax,
    totalSen = total,
    payment = payment,
    cashReceivedSen = cashReceived,
    changeSen = change,
    status = status,
    note = note,
    pickupTime = pickupTime,
    refundedAt = refundedAt,
    takenBy = takenBy,
    refundedBy = refundedBy
)

fun Order.lineEntities() = lines.mapIndexed { index, line ->
    OrderLineEntity(
        orderId = id,
        position = index,
        lineId = line.lineId,
        itemId = line.itemId,
        name = line.name,
        emoji = line.emoji,
        unitPriceSen = line.unitPrice,
        qty = line.qty,
        sizeLabel = line.sizeLabel,
        crustLabel = line.crustLabel,
        toppings = line.toppings,
        note = line.note
    )
}

fun OrderLineEntity.toModel() = CartLine(
    lineId = lineId,
    itemId = itemId,
    name = name,
    emoji = emoji,
    unitPrice = unitPriceSen,
    qty = qty,
    sizeLabel = sizeLabel,
    crustLabel = crustLabel,
    toppings = toppings,
    note = note
)

/** Joins order rows with their lines, keeping the order list's sort order. */
fun toOrders(orders: List<OrderEntity>, lines: List<OrderLineEntity>): List<Order> {
    val linesByOrder = lines.groupBy { it.orderId }
    return orders.map { row ->
        Order(
            id = row.id,
            number = row.number,
            createdAt = row.createdAt,
            lines = linesByOrder[row.id].orEmpty().sortedBy { it.position }.map { it.toModel() },
            type = row.type,
            table = row.tableLabel,
            customerName = row.customerName,
            customerPhone = row.customerPhone,
            address = row.address,
            subtotal = row.subtotalSen,
            tax = row.taxSen,
            total = row.totalSen,
            payment = row.payment,
            cashReceived = row.cashReceivedSen,
            change = row.changeSen,
            status = row.status,
            note = row.note,
            pickupTime = row.pickupTime,
            refundedAt = row.refundedAt,
            takenBy = row.takenBy,
            refundedBy = row.refundedBy
        )
    }
}
