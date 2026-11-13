package com.slicehouse.pos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyImportTest {

    @Test
    fun convertsAV14FileToSen() {
        val data = LegacyImport.parse(LegacySamples.V14_FILE)

        val custom = data.menu.single { it.id == "custom1" }
        assertEquals(735L, custom.price)
        assertEquals(Category.SIDES, custom.category)
        assertEquals(1000L, data.menu.single { it.id == "p1" }.twelveInchDelta)
        assertEquals(300L, data.toppings.single().price)

        assertEquals(650, data.settings.taxBasisPoints)
        assertEquals(ThemeMode.DARK, data.settings.themeMode)
        assertEquals(80, data.settings.paperWidthMm)

        val order = data.orders.single()
        assertEquals(1004, order.number)
        assertEquals(4600L, order.subtotal)
        assertEquals(299L, order.tax)
        assertEquals(4899L, order.total)
        assertEquals(5000L, order.cashReceived)
        assertEquals(101L, order.change)
        assertEquals(PaymentMethod.CASH, order.payment)
        assertEquals("19:30", order.pickupTime)
        assertEquals(2300L, order.lines.single().unitPrice)
        assertEquals(listOf("Extra cheese"), order.lines.single().toppings)
        assertEquals(1005, data.nextOrderNumber)
    }

    @Test
    fun nextNumberNeverReusesAnExistingOrderNumber() {
        val broken = LegacySamples.V14_FILE.replace("\"nextOrderNumber\":1005", "\"nextOrderNumber\":1001")
        assertEquals(1005, LegacyImport.parse(broken).nextOrderNumber)
    }

    @Test
    fun oldDemoMenuGetsTheShopUpgrade() {
        val old = """
            {"menu":[{"id":"p1","name":"Margherita","price":12.5,"isPizza":true},
                     {"id":"s1","name":"Garlic Bread","category":"SIDES","price":4.5}],
             "toppings":[{"id":"bacon","label":"Bacon","price":2.0}],
             "settings":{"shopName":"Slice House","phone":"(555) 012-3456","taxPercent":8.0},
             "orders":[],"nextOrderNumber":1001}
        """.trimIndent()

        val data = LegacyImport.parse(old)

        assertEquals("4in1", data.menu.first().name)
        assertTrue(data.menu.filter { it.isPizza }.all { it.price == 2000L })
        assertEquals(SampleData.toppings, data.toppings)
        assertEquals("Pizza Kayu Api Dungun", data.settings.shopName)
    }

    @Test(expected = Exception::class)
    fun unreadableFileThrows() {
        LegacyImport.parse("{ this is not json")
    }
}

object LegacySamples {
    /** A v1.4 file with a custom item, 6.5% tax and one cash order. */
    val V14_FILE = """
        {"menu":[
            {"id":"p1","name":"4in1","price":20.0,"isPizza":true,"imageKey":"pizza_4in1",
             "twelveInchAvailable":true,"twelveInchDelta":10.0},
            {"id":"custom1","name":"Satay","category":"SIDES","price":7.35}],
         "toppings":[{"id":"cheese","label":"Extra cheese","price":3.0}],
         "settings":{"shopName":"Pizza Kayu Api Dungun","taxPercent":6.5,"themeMode":"DARK","paperWidthMm":80},
         "orders":[{"id":"o1","number":1004,"createdAt":1731400000000,
            "lines":[{"lineId":"l1","itemId":"p1","name":"4in1","emoji":"P","unitPrice":23.0,"qty":2,
                      "sizeLabel":"10\"","toppings":["Extra cheese"]}],
            "type":"TAKEAWAY","subtotal":46.0,"tax":2.99,"total":48.99,"payment":"CASH",
            "cashReceived":50.0,"change":1.01,"pickupTime":"19:30"}],
         "nextOrderNumber":1005,"schemaVersion":3}
    """.trimIndent()
}
