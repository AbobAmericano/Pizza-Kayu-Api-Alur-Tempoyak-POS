package com.slicehouse.pos.data

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.round

@Serializable
enum class Category(val label: String) {
    PIZZA("Pizzas"),
    SIDES("Sides"),
    DRINKS("Drinks"),
    DESSERTS("Desserts")
}

@Serializable
enum class OrderType(val label: String, val emoji: String) {
    DINE_IN("Dine-in", "\uD83C\uDF7D\uFE0F"),
    TAKEAWAY("Takeaway", "\uD83D\uDECD\uFE0F"),
    DELIVERY("Delivery", "\uD83D\uDEF5")
}

@Serializable
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    CARD("Card")
}

@Serializable
enum class OrderStatus(val label: String) {
    COMPLETED("Completed"),
    REFUNDED("Refunded")
}

@Serializable
enum class ThemeMode(val label: String, val emoji: String) {
    SYSTEM("System", "\uD83D\uDDA5\uFE0F"),
    LIGHT("Light", "\u2600\uFE0F"),
    DARK("Dark", "\uD83C\uDF19")
}

@Serializable
data class Settings(
    val shopName: String = "Pizza Kayu Api Dungun",
    val taxPercent: Double = 8.0,
    val currency: String = "RM",
    val phone: String = "601114927780",
    val address: String = "Dungun, Terengganu",
    val footer: String = "Terima kasih! Jumpa lagi.",
    val instagram: String = "https://www.instagram.com/pizza.alorpoyok",
    val tiktok: String = "@nazalinaalias",
    val locationUrl: String = "https://maps.app.goo.gl/wVPagLXnoqLt8s279",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val printerAddress: String = "",
    val printerName: String = "",
    val paperWidthMm: Int = 58
)

@Serializable
data class MenuItem(
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "\uD83C\uDF55",
    val category: Category = Category.PIZZA,
    val price: Double = 0.0,
    val available: Boolean = true,
    val isPizza: Boolean = false,
    /** Key of a bundled photo (see [com.slicehouse.pos.ui.menuPhotoOptions]); blank means no photo. */
    val imageKey: String = "",
    /** Whether this pizza can be ordered in the 12" size. */
    val twelveInchAvailable: Boolean = true,
    /** Extra added to [price] when the 12" size is chosen (10.0 = RM30, 20.0 = RM40). */
    val twelveInchDelta: Double = 10.0
)

@Serializable
data class SizeOption(
    val id: String,
    val label: String,
    val detail: String,
    val delta: Double
)

@Serializable
data class CrustOption(
    val id: String,
    val label: String,
    val delta: Double
)

@Serializable
data class Topping(
    val id: String,
    val label: String,
    val emoji: String = "\uD83C\uDF55",
    val price: Double = 1.0
)

@Serializable
data class CartLine(
    val lineId: String,
    val itemId: String,
    val name: String,
    val emoji: String,
    val unitPrice: Double,
    val qty: Int,
    val sizeLabel: String? = null,
    val crustLabel: String? = null,
    val toppings: List<String> = emptyList(),
    val note: String = ""
) {
    val lineTotal: Double get() = unitPrice * qty

    /** Human readable modifier summary, e.g. "Large · Stuffed · +Bacon, +Olives". */
    fun modifiers(): String {
        val parts = mutableListOf<String>()
        sizeLabel?.let { parts += it }
        crustLabel?.let { parts += it }
        if (toppings.isNotEmpty()) parts += toppings.joinToString(", ") { "+$it" }
        if (note.isNotBlank()) parts += "“$note”"
        return parts.joinToString(" · ")
    }
}

@Serializable
data class Order(
    val id: String,
    val number: Int,
    val createdAt: Long,
    val lines: List<CartLine>,
    val type: OrderType,
    val table: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val payment: PaymentMethod = PaymentMethod.CARD,
    val cashReceived: Double = 0.0,
    val change: Double = 0.0,
    val status: OrderStatus = OrderStatus.COMPLETED,
    val note: String = "",
    /** Pickup / ready time chosen on the register, e.g. "12:30" or "ASAP". */
    val pickupTime: String = ""
) {
    val itemCount: Int get() = lines.sumOf { it.qty }
}

@Serializable
data class PersistedData(
    val menu: List<MenuItem>,
    val toppings: List<Topping>,
    val settings: Settings,
    val orders: List<Order>,
    val nextOrderNumber: Int,
    /** Bumped when the on-device menu or toppings need a one-time migration. */
    val schemaVersion: Int = 0
)

data class Totals(val subtotal: Double, val tax: Double, val total: Double)

fun round2(value: Double): Double = round(value * 100.0) / 100.0

fun money(value: Double, currency: String = "RM"): String =
    currency + String.format(Locale.US, "%.2f", value)

/** Formats a price change, e.g. "+RM2.50" or "-RM2.50". */
fun signedMoney(value: Double, currency: String = "RM"): String =
    if (value < 0) "-" + money(-value, currency) else "+" + money(value, currency)

fun itemCountLabel(count: Int): String = "$count ${if (count == 1) "item" else "items"}"

/** Turns an Instagram profile URL into "@handle" for receipts. */
fun instagramHandle(value: String): String {
    val trimmed = value.trim().removeSuffix("/")
    if (trimmed.startsWith("@")) return trimmed
    val handle = trimmed.substringAfterLast("/").substringBefore("?")
    return if (handle.isBlank()) value else "@$handle"
}

/** Shortens a URL for narrow receipt paper. */
fun compactUrl(value: String): String =
    value.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")

fun formatClock(time: Long): String =
    java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(time))

fun formatDateTime(time: Long): String =
    java.text.SimpleDateFormat("d MMM · HH:mm", Locale.US).format(java.util.Date(time))
