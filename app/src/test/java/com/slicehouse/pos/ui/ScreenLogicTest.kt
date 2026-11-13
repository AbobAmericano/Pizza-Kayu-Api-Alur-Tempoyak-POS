package com.slicehouse.pos.ui

import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.OrderStatus
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.SampleData
import com.slicehouse.pos.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** The screen logic that moved out of the composables into ViewModels. */
class ScreenLogicTest {

    private val cart = PosState(
        settings = Settings(taxBasisPoints = 800),
        nextOrderNumber = 1006,
        cart = listOf(CartLine("l1", "p1", "4in1", "P", unitPrice = 2300, qty = 2)),
        orderType = OrderType.DINE_IN,
        table = "4",
        pickupTime = "19:30"
    ) // subtotal 46.00 + 8% 3.68 = 49.68

    // ------------------------------------------------------------- checkout

    @Test
    fun quickCashOffersExactThenRoundNotes() {
        assertEquals(listOf(4968L, 5000L, 6000L, 10000L), CheckoutViewModel.quickCashAmounts(4968))
        assertEquals(listOf(2000L), CheckoutViewModel.quickCashAmounts(2000).take(1))
        assertEquals(5, CheckoutViewModel.quickCashAmounts(1).size)
    }

    @Test
    fun cashShortOfTheTotalCantBePaid() {
        val state = CheckoutViewModel.checkoutState(cart, PaymentMethod.CASH, "40")
        assertEquals(4968L, state.due)
        assertEquals(968L, state.shortBy)
        assertEquals(0L, state.change)
        assertFalse(state.canPay)
    }

    @Test
    fun enoughCashShowsChange() {
        val state = CheckoutViewModel.checkoutState(cart, PaymentMethod.CASH, "50")
        assertEquals(32L, state.change)
        assertTrue(state.canPay)
    }

    @Test
    fun cardNeedsNoCashAndEmptyCartCantPay() {
        assertTrue(CheckoutViewModel.checkoutState(cart, PaymentMethod.CARD, "").canPay)
        assertFalse(CheckoutViewModel.checkoutState(PosState(), PaymentMethod.CARD, "").canPay)
    }

    @Test
    fun orderLabelListsTableAndPickup() {
        assertEquals(
            "Order #1006 · Dine-in · Table 4 · Pickup 19:30",
            CheckoutViewModel.checkoutState(cart, PaymentMethod.CARD, "").orderLabel
        )
    }

    // --------------------------------------------------------------- orders

    private val zone = ZoneId.of("Asia/Kuala_Lumpur")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun order(number: Int, time: String, total: Long, status: OrderStatus = OrderStatus.COMPLETED) =
        Order(id = "o$number", number = number, createdAt = at(time), lines = emptyList(),
            type = OrderType.TAKEAWAY, total = total, status = status)

    private val history = PosState(
        orders = listOf(
            order(4, "2026-11-13T21:30", 2348, OrderStatus.REFUNDED),
            order(3, "2026-11-13T19:00", 1566),
            order(2, "2026-11-13T15:05", 4899),
            order(1, "2026-11-12T21:55", 9999)
        )
    )

    @Test
    fun todaysStatsSkipYesterdayAndRefunds() {
        val start = OrdersViewModel.startOfDay(at("2026-11-13T22:00"), zone)
        val state = OrdersViewModel.ordersState(history, start)
        assertEquals(listOf(4, 3, 2), state.todayOrders.map { it.number })
        assertEquals(2, state.completedToday)
        assertEquals(6465L, state.salesToday)
        assertEquals(3233L, state.avgTicket) // 64.65 / 2 = 32.325, rounded half up
    }

    @Test
    fun todayRollsOverAtMidnight() {
        val justBefore = OrdersViewModel.startOfDay(at("2026-11-13T23:59"), zone)
        val justAfter = OrdersViewModel.startOfDay(at("2026-11-14T00:01"), zone)
        assertEquals(3, OrdersViewModel.ordersState(history, justBefore).todayOrders.size)
        assertEquals(0, OrdersViewModel.ordersState(history, justAfter).todayOrders.size)
        assertEquals(0L, OrdersViewModel.ordersState(history, justAfter).avgTicket)
    }

    // ------------------------------------------------------------- register

    @Test
    fun menuFilterHidesUnavailableAndSearchesEverything() {
        val menu = SampleData.menu.map { if (it.id == "p2") it.copy(available = false) else it }

        val pizzas = RegisterViewModel.filterMenu(menu, Category.PIZZA, "")
        assertEquals(12, pizzas.size)
        assertFalse(pizzas.any { it.id == "p2" })

        // Search ignores the chosen category and also matches descriptions ("garlic butter").
        assertEquals(listOf("Prawn", "Garlic Bread", "Cheesy Garlic Bread"),
            RegisterViewModel.filterMenu(menu, Category.DRINKS, "garlic").map { it.name })
        assertEquals(listOf("Prawn", "Seafood"),
            RegisterViewModel.filterMenu(menu, Category.DRINKS, "prawn").map { it.name })
    }
}
