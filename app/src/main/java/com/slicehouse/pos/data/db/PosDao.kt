package com.slicehouse.pos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.slicehouse.pos.data.OrderStatus

/**
 * Every write the app makes. Methods marked @Transaction run all-or-nothing:
 * if the app dies halfway, the database rolls back to how it was before.
 */
@Dao
abstract class PosDao {

    // ------------------------------------------------------------------ reads

    @Query("SELECT * FROM menu_items ORDER BY position")
    abstract suspend fun menu(): List<MenuItemEntity>

    @Query("SELECT * FROM toppings ORDER BY position")
    abstract suspend fun toppings(): List<ToppingEntity>

    @Query("SELECT * FROM shop_settings WHERE id = 1")
    abstract suspend fun settings(): ShopSettingsEntity?

    @Query("SELECT * FROM orders ORDER BY createdAt DESC, number DESC")
    abstract suspend fun orders(): List<OrderEntity>

    @Query("SELECT * FROM order_lines ORDER BY orderId, position")
    abstract suspend fun orderLines(): List<OrderLineEntity>

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    abstract suspend fun meta(key: String): String?

    // ---------------------------------------------------------- single writes

    @Upsert
    abstract suspend fun upsertSettings(settings: ShopSettingsEntity)

    @Upsert
    abstract suspend fun putMeta(meta: MetaEntity)

    @Query("UPDATE orders SET status = :status, refundedAt = :refundedAt, refundedBy = :refundedBy WHERE id = :orderId")
    abstract suspend fun setOrderStatus(orderId: String, status: OrderStatus, refundedAt: Long?, refundedBy: String?)

    // ------------------------------------------------------------ batch parts

    @Insert
    protected abstract suspend fun insertMenu(items: List<MenuItemEntity>)

    @Insert
    protected abstract suspend fun insertToppings(items: List<ToppingEntity>)

    @Insert
    protected abstract suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert
    protected abstract suspend fun insertOrderLines(lines: List<OrderLineEntity>)

    @Query("DELETE FROM menu_items")
    protected abstract suspend fun clearMenu()

    @Query("DELETE FROM toppings")
    protected abstract suspend fun clearToppings()

    @Query("DELETE FROM order_lines")
    protected abstract suspend fun clearOrderLines()

    @Query("DELETE FROM orders")
    protected abstract suspend fun clearOrders()

    // ------------------------------------------------------- all-or-nothing

    /** Replaces the whole menu. The list order becomes the display order. */
    @Transaction
    open suspend fun replaceMenu(items: List<MenuItemEntity>) {
        clearMenu()
        insertMenu(items)
    }

    @Transaction
    open suspend fun replaceToppings(items: List<ToppingEntity>) {
        clearToppings()
        insertToppings(items)
    }

    /** Saves a paid order, its lines and the next order number together. */
    @Transaction
    open suspend fun saveOrder(order: OrderEntity, lines: List<OrderLineEntity>, nextOrderNumber: Int) {
        insertOrders(listOf(order))
        insertOrderLines(lines)
        putMeta(MetaEntity(KEY_NEXT_ORDER_NUMBER, nextOrderNumber.toString()))
    }

    /** Deletes every order and puts the starter menu back. Settings are kept. */
    @Transaction
    open suspend fun resetData(
        menu: List<MenuItemEntity>,
        toppings: List<ToppingEntity>,
        nextOrderNumber: Int
    ) {
        clearOrderLines()
        clearOrders()
        replaceMenu(menu)
        replaceToppings(toppings)
        putMeta(MetaEntity(KEY_NEXT_ORDER_NUMBER, nextOrderNumber.toString()))
    }

    /** First-launch setup: either imported data from v1.4 or the starter data. */
    @Transaction
    open suspend fun initialize(
        menu: List<MenuItemEntity>,
        toppings: List<ToppingEntity>,
        settings: ShopSettingsEntity,
        orders: List<OrderEntity>,
        lines: List<OrderLineEntity>,
        nextOrderNumber: Int,
        importWarning: String
    ) {
        replaceMenu(menu)
        replaceToppings(toppings)
        upsertSettings(settings)
        insertOrders(orders)
        insertOrderLines(lines)
        putMeta(MetaEntity(KEY_NEXT_ORDER_NUMBER, nextOrderNumber.toString()))
        putMeta(MetaEntity(KEY_IMPORT_WARNING, importWarning))
        putMeta(MetaEntity(KEY_INITIALIZED, "1"))
    }

    companion object {
        const val KEY_INITIALIZED = "initialized"
        const val KEY_NEXT_ORDER_NUMBER = "next_order_number"
        const val KEY_IMPORT_WARNING = "legacy_import_warning"
    }
}
