package com.slicehouse.pos.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The JSON the FireLord server's /pos/api/ speaks (see FireLord_Platform pos/schemas.py).
// Money is whole sen on both sides, so nothing is converted.

@Serializable
data class PairRequest(
    val code: String,
    @SerialName("device_name") val deviceName: String
)

@Serializable
data class PairResponse(
    @SerialName("device_key") val deviceKey: String,
    @SerialName("device_id") val deviceId: Int,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_slug") val shopSlug: String
)

@Serializable
data class MenuItemDto(
    val id: String,
    val name: String,
    val description: String = "",
    val emoji: String = "",
    val category: String,
    @SerialName("price_sen") val priceSen: Long,
    val available: Boolean = true,
    @SerialName("is_pizza") val isPizza: Boolean = false,
    @SerialName("image_key") val imageKey: String = "",
    @SerialName("twelve_inch_available") val twelveInchAvailable: Boolean = false,
    @SerialName("twelve_inch_delta_sen") val twelveInchDeltaSen: Long = 0
)

@Serializable
data class ToppingDto(
    val id: String,
    val label: String,
    val emoji: String = "",
    @SerialName("price_sen") val priceSen: Long
)

@Serializable
data class MenuUpload(
    @SerialName("base_version") val baseVersion: Long,
    val items: List<MenuItemDto>,
    val toppings: List<ToppingDto>
)

@Serializable
data class MenuResponse(
    val version: Long,
    val changed: Boolean,
    @SerialName("shop_name") val shopName: String,
    val currency: String,
    @SerialName("tax_basis_points") val taxBasisPoints: Int,
    val items: List<MenuItemDto> = emptyList(),
    val toppings: List<ToppingDto> = emptyList()
)

@Serializable
data class OrderLineDto(
    @SerialName("line_id") val lineId: String,
    @SerialName("item_id") val itemId: String,
    val name: String,
    val emoji: String,
    @SerialName("unit_price_sen") val unitPriceSen: Long,
    val qty: Int,
    @SerialName("size_label") val sizeLabel: String?,
    @SerialName("crust_label") val crustLabel: String?,
    val toppings: List<String>,
    val note: String
)

@Serializable
data class OrderDto(
    val id: String,
    val number: Int,
    @SerialName("created_at") val createdAt: String,
    val type: String,
    val table: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_phone") val customerPhone: String,
    val address: String,
    val note: String,
    @SerialName("pickup_time") val pickupTime: String,
    @SerialName("subtotal_sen") val subtotalSen: Long,
    @SerialName("tax_sen") val taxSen: Long,
    @SerialName("total_sen") val totalSen: Long,
    val payment: String,
    @SerialName("cash_received_sen") val cashReceivedSen: Long,
    @SerialName("change_sen") val changeSen: Long,
    val status: String,
    @SerialName("taken_by") val takenBy: String,
    @SerialName("refunded_at") val refundedAt: String?,
    @SerialName("refunded_by") val refundedBy: String?,
    val lines: List<OrderLineDto>
)

@Serializable
data class OrdersUpload(val orders: List<OrderDto>)

@Serializable
data class OrderResult(
    val id: String,
    /** "accepted", "duplicate" (already there: fine) or "rejected". */
    val result: String,
    val reason: String = ""
)

@Serializable
data class OrdersResponse(val results: List<OrderResult>)

@Serializable
data class RefundUpload(
    @SerialName("refunded_at") val refundedAt: String,
    @SerialName("refunded_by") val refundedBy: String
)

@Serializable
data class ErrorResponse(val detail: String = "")
