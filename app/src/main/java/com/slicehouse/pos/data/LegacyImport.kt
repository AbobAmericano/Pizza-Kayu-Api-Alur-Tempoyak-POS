package com.slicehouse.pos.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads the `slicehouse_pos.json` file written by v1.4 and earlier, where money
 * was a Double in RM. Used once, on the first launch of the Room-based version.
 */
object LegacyImport {

    const val FILE_NAME = "slicehouse_pos.json"
    const val IMPORTED_FILE_NAME = "slicehouse_pos.imported.json"

    /** v1.4's own schema version; files below this needed its menu upgrade. */
    private const val LEGACY_SCHEMA_VERSION = 3

    data class Data(
        val menu: List<MenuItem>,
        val toppings: List<Topping>,
        val settings: Settings,
        val orders: List<Order>,
        val nextOrderNumber: Int
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Throws if the text isn't a readable v1.4 file. */
    fun parse(text: String): Data {
        val file = json.decodeFromString<LegacyFile>(text)
        val needsUpgrade = file.schemaVersion < LEGACY_SCHEMA_VERSION
        val menu = file.menu.map { it.toModel() }
        return Data(
            menu = if (needsUpgrade) migrateMenuV3(menu) else menu,
            toppings = if (needsUpgrade) SampleData.toppings else file.toppings.map { it.toModel() },
            settings = migrateSettings(file.settings.toModel()),
            orders = file.orders.map { it.toModel() },
            nextOrderNumber = maxOf(file.nextOrderNumber, (file.orders.maxOfOrNull { it.number } ?: 0) + 1)
        )
    }

    /**
     * Same upgrade v1.4 applied to older files: swaps the old demo pizzas for the
     * shop's photographed menu and applies the shop's pizza pricing.
     */
    private fun migrateMenuV3(menu: List<MenuItem>): List<MenuItem> {
        val oldPizzaIds = (1..8).map { "p$it" }.toSet()
        val hasOldDemo = menu.any { it.id in oldPizzaIds && it.imageKey.isBlank() }
        val base = if (hasOldDemo) {
            val kept = menu.filterNot { it.id in oldPizzaIds }
            val pizzas = SampleData.menu.filter { it.category == Category.PIZZA }
            (pizzas + kept).distinctBy { it.id }
        } else {
            menu
        }
        val templates = SampleData.menu.filter { it.category == Category.PIZZA }.associateBy { it.id }
        return base.map { item ->
            val template = templates[item.id] ?: return@map item
            item.copy(
                price = template.price,
                twelveInchAvailable = template.twelveInchAvailable,
                twelveInchDelta = template.twelveInchDelta
            )
        }
    }

    /** Replaces the old "Slice House" demo shop details with this shop's. */
    private fun migrateSettings(settings: Settings): Settings {
        if (settings.shopName != "Slice House" || settings.phone != "(555) 012-3456") return settings
        val defaults = Settings()
        return settings.copy(
            shopName = defaults.shopName,
            address = defaults.address,
            phone = defaults.phone,
            footer = defaults.footer,
            currency = defaults.currency
        )
    }

    // ------------------------------------------------ v1.4 file format (RM Doubles)

    @Serializable
    private data class LegacyFile(
        val menu: List<LegacyMenuItem>,
        val toppings: List<LegacyTopping>,
        val settings: LegacySettings,
        val orders: List<LegacyOrder>,
        val nextOrderNumber: Int,
        val schemaVersion: Int = 0
    )

    @Serializable
    private data class LegacySettings(
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
    ) {
        fun toModel() = Settings(
            shopName = shopName,
            taxBasisPoints = toBasisPoints(taxPercent),
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
    }

    @Serializable
    private data class LegacyMenuItem(
        val id: String,
        val name: String,
        val description: String = "",
        val emoji: String = "🍕",
        val category: Category = Category.PIZZA,
        val price: Double = 0.0,
        val available: Boolean = true,
        val isPizza: Boolean = false,
        val imageKey: String = "",
        val twelveInchAvailable: Boolean = true,
        val twelveInchDelta: Double = 10.0
    ) {
        fun toModel() = MenuItem(
            id = id,
            name = name,
            description = description,
            emoji = emoji,
            category = category,
            price = toSen(price),
            available = available,
            isPizza = isPizza,
            imageKey = imageKey,
            twelveInchAvailable = twelveInchAvailable,
            twelveInchDelta = toSen(twelveInchDelta)
        )
    }

    @Serializable
    private data class LegacyTopping(
        val id: String,
        val label: String,
        val emoji: String = "🍕",
        val price: Double = 1.0
    ) {
        fun toModel() = Topping(id = id, label = label, emoji = emoji, price = toSen(price))
    }

    @Serializable
    private data class LegacyCartLine(
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
        fun toModel() = CartLine(
            lineId = lineId,
            itemId = itemId,
            name = name,
            emoji = emoji,
            unitPrice = toSen(unitPrice),
            qty = qty,
            sizeLabel = sizeLabel,
            crustLabel = crustLabel,
            toppings = toppings,
            note = note
        )
    }

    @Serializable
    private data class LegacyOrder(
        val id: String,
        val number: Int,
        val createdAt: Long,
        val lines: List<LegacyCartLine>,
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
        val pickupTime: String = ""
    ) {
        // Totals are copied as recorded, not recalculated, so old receipts stay identical.
        fun toModel() = Order(
            id = id,
            number = number,
            createdAt = createdAt,
            lines = lines.map { it.toModel() },
            type = type,
            table = table,
            customerName = customerName,
            customerPhone = customerPhone,
            address = address,
            subtotal = toSen(subtotal),
            tax = toSen(tax),
            total = toSen(total),
            payment = payment,
            cashReceived = toSen(cashReceived),
            change = toSen(change),
            status = status,
            note = note,
            pickupTime = pickupTime
        )
    }
}
