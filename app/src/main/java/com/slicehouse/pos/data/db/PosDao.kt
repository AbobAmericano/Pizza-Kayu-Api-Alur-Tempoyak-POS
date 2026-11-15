package com.slicehouse.pos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.slicehouse.pos.data.OrderStatus
import kotlinx.coroutines.flow.Flow

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

    /** Also marks the refund as not yet uploaded, so the sync sends it. */
    @Query(
        "UPDATE orders SET status = :status, refundedAt = :refundedAt, refundedBy = :refundedBy, " +
            "refundSyncState = 0 WHERE id = :orderId"
    )
    abstract suspend fun setOrderStatus(orderId: String, status: OrderStatus, refundedAt: Long?, refundedBy: String?)

    @Query("DELETE FROM app_meta WHERE `key` = :key")
    abstract suspend fun deleteMeta(key: String)

    // ------------------------------------------------------------------- sync

    /** Orders not uploaded yet, oldest first. */
    @Query("SELECT * FROM orders WHERE syncState = 0 ORDER BY createdAt, number LIMIT :limit")
    abstract suspend fun ordersToUpload(limit: Int): List<OrderEntity>

    @Query("SELECT * FROM order_lines WHERE orderId IN (:orderIds) ORDER BY orderId, position")
    abstract suspend fun linesFor(orderIds: List<String>): List<OrderLineEntity>

    /** Refunds of orders that are already on the server but whose refund isn't. */
    @Query("SELECT * FROM orders WHERE syncState = 1 AND status = 'REFUNDED' AND refundSyncState = 0")
    abstract suspend fun refundsToUpload(): List<OrderEntity>

    @Query("UPDATE orders SET syncState = :state WHERE id = :orderId")
    abstract suspend fun setSyncState(orderId: String, state: Int)

    /** Only if the order is still refunded at [refundedAt], so a newer change isn't marked as sent. */
    @Query("UPDATE orders SET refundSyncState = :state WHERE id = :orderId AND refundedAt IS :refundedAt")
    abstract suspend fun setRefundSyncState(orderId: String, refundedAt: Long?, state: Int)

    /** Orders and refunds still waiting to go up. */
    @Query(
        "SELECT COUNT(*) FROM orders WHERE syncState = 0 OR " +
            "(syncState = 1 AND status = 'REFUNDED' AND refundSyncState = 0)"
    )
    abstract fun waitingToUpload(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE syncState = 2")
    abstract fun rejectedUploads(): Flow<Int>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertOrderIfNew(order: OrderEntity): Long

    /** Keeps a copy of an online order once it's picked up. Saving it twice changes nothing. */
    @Transaction
    open suspend fun saveOnlineOrder(order: OrderEntity, lines: List<OrderLineEntity>) {
        if (insertOrderIfNew(order) != -1L) insertOrderLines(lines)
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
