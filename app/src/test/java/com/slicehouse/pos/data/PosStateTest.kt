package com.slicehouse.pos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosStateTest {

    private val pizza = CartLine("l1", "p1", "4in1", "P", unitPrice = 2300, qty = 2)
    private val drink = CartLine("l2", "d1", "Coca-Cola", "C", unitPrice = 250, qty = 1)

    private val state = PosState(
        settings = Settings(taxBasisPoints = 800),
        nextOrderNumber = 1010,
        cart = listOf(pizza, drink),
        pickupTime = "19:30"
    )

    @Test
    fun totalsAreExactSen() {
        assertEquals(4850L, state.subtotal) // 2 x 23.00 + 2.50
        assertEquals(388L, state.tax) // 8% = 3.88
        assertEquals(5238L, state.total)
    }

    @Test
    fun cashOrderRecordsChange() {
        val order = state.buildOrder(PaymentMethod.CASH, cashReceived = 6000, id = "o1", now = 42)!!
        assertEquals(1010, order.number)
        assertEquals(5238L, order.total)
        assertEquals(6000L, order.cashReceived)
        assertEquals(762L, order.change)
        assertEquals("19:30", order.pickupTime)
        assertEquals(42L, order.createdAt)
    }

    @Test
    fun cardOrderIsPaidExactly() {
        val order = state.buildOrder(PaymentMethod.CARD, cashReceived = 0, id = "o1", now = 0)!!
        assertEquals(order.total, order.cashReceived)
        assertEquals(0L, order.change)
    }

    @Test
    fun emptyCartMakesNoOrder() {
        assertNull(PosState().buildOrder(PaymentMethod.CARD, 0, "o1", 0))
    }
}
