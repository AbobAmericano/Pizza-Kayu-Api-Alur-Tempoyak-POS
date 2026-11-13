package com.slicehouse.pos.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.OrderStatus
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.ThemeMode

// Database tables. Every money column is whole sen (Long) and named *Sen.

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: String,
    /** Display order on the register. */
    val position: Int,
    val name: String,
    val description: String,
    val emoji: String,
    val category: Category,
    val priceSen: Long,
    val available: Boolean,
    val isPizza: Boolean,
    val imageKey: String,
    val twelveInchAvailable: Boolean,
    val twelveInchDeltaSen: Long
)

@Entity(tableName = "toppings")
data class ToppingEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val label: String,
    val emoji: String,
    val priceSen: Long
)

/** Always a single row with [id] = 1. */
@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String,
    val taxBasisPoints: Int,
    val currency: String,
    val phone: String,
    val address: String,
    val footer: String,
    val instagram: String,
    val tiktok: String,
    val locationUrl: String,
    val themeMode: ThemeMode,
    val printerAddress: String,
    val printerName: String,
    val paperWidthMm: Int
)

@Entity(
    tableName = "orders",
    indices = [Index("createdAt"), Index("number")]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val createdAt: Long,
    val type: OrderType,
    val tableLabel: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val subtotalSen: Long,
    val taxSen: Long,
    val totalSen: Long,
    val payment: PaymentMethod,
    val cashReceivedSen: Long,
    val changeSen: Long,
    val status: OrderStatus,
    val note: String,
    val pickupTime: String,
    val refundedAt: Long?
)

/** One cart line of an order. Name and price are copied at the time of sale. */
@Entity(
    tableName = "order_lines",
    primaryKeys = ["orderId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderLineEntity(
    val orderId: String,
    val position: Int,
    val lineId: String,
    val itemId: String,
    val name: String,
    val emoji: String,
    val unitPriceSen: Long,
    val qty: Int,
    val sizeLabel: String?,
    val crustLabel: String?,
    val toppings: List<String>,
    val note: String
)

/** Small key/value table for counters and one-off flags. */
@Entity(tableName = "app_meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
