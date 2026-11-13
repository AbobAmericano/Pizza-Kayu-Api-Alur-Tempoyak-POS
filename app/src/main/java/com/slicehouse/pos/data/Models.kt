package com.slicehouse.pos.data

import kotlinx.serialization.Serializable
import java.util.Locale

// All money fields below are whole sen (see Money.kt): 2550 = RM25.50.

@Serializable
enum class Category(val label: String) {
    PIZZA("Pizzas"),
    SIDES("Sides"),
    DRINKS("Drinks"),
    DESSERTS("Desserts")
}

@Serializable
enum class OrderType(val label: String, val emoji: String) {
    DINE_IN("Dine-in", "🍽️"),
    TAKEAWAY("Takeaway", "🛍️"),
    DELIVERY("Delivery", "🛵")
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
    SYSTEM("System", "🖥️"),
    LIGHT("Light", "☀️"),
    DARK("Dark", "🌙")
}

data class Settings(
    val shopName: String = "Pizza Kayu Api Dungun",
    /** Tax rate in hundredths of a percent: 800 = 8%. */
    val taxBasisPoints: BasisPoints = 800,
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

data class MenuItem(
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "🍕",
    val category: Category = Category.PIZZA,
    val price: Sen = 0,
    val available: Boolean = true,
    val isPizza: Boolean = false,
    /** Key of a bundled photo (see [com.slicehouse.pos.ui.menuPhotoOptions]); blank means no photo. */
    val imageKey: String = "",
    /** Whether this pizza can be ordered in the 12" size. */
    val twelveInchAvailable: Boolean = true,
    /** Extra added to [price] when the 12" size is chosen (1000 = RM30, 2000 = RM40). */
    val twelveInchDelta: Sen = 1_000
)

data class SizeOption(
    val id: String,
    val label: String,
    val detail: String,
    val delta: Sen
)

data class CrustOption(
    val id: String,
    val label: String,
    val delta: Sen
)

data class Topping(
    val id: String,
    val label: String,
    val emoji: String = "🍕",
    val price: Sen = 100
)

data class CartLine(
    val lineId: String,
    val itemId: String,
    val name: String,
    val emoji: String,
    val unitPrice: Sen,
    val qty: Int,
    val sizeLabel: String? = null,
    val crustLabel: String? = null,
    val toppings: List<String> = emptyList(),
    val note: String = ""
) {
    val lineTotal: Sen get() = unitPrice * qty

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
    val subtotal: Sen = 0,
    val tax: Sen = 0,
    val total: Sen = 0,
    val payment: PaymentMethod = PaymentMethod.CARD,
    val cashReceived: Sen = 0,
    val change: Sen = 0,
    val status: OrderStatus = OrderStatus.COMPLETED,
    val note: String = "",
    /** Pickup / ready time chosen on the register, e.g. "12:30" or "ASAP". */
    val pickupTime: String = "",
    /** When the order was marked refunded, or null. */
    val refundedAt: Long? = null
) {
    val itemCount: Int get() = lines.sumOf { it.qty }
}

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
