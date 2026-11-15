package com.slicehouse.pos.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Online screen offers the same moves as FireLord's web board. */
class OnlineActionsTest {

    @Test
    fun eachStatusOffersTheNextStep() {
        assertEquals(listOf("RECEIVED", "CANCELLED"), onlineActions("PENDING", "W1").map { it.status })
        assertEquals(listOf("PREPARING", "CANCELLED"), onlineActions("RECEIVED", "W1").map { it.status })
        assertEquals(listOf("BAKING", "CANCELLED"), onlineActions("PREPARING", "W1").map { it.status })
        assertEquals(listOf("READY"), onlineActions("BAKING", "W1").map { it.status }) // no cancelling in the oven
        assertEquals(
            listOf("COMPLETED" to "CASH", "COMPLETED" to "CARD", "CANCELLED" to ""),
            onlineActions("READY", "W1").map { it.status to it.payment }
        )
        assertTrue(onlineActions("COMPLETED", "W1").isEmpty())
    }

    @Test
    fun cancellingAlwaysAsksFirst() {
        listOf("PENDING", "RECEIVED", "PREPARING", "READY").forEach { status ->
            onlineActions(status, "W9").filter { it.status == "CANCELLED" }.forEach {
                assertTrue(it.confirm!!.contains("W9"))
            }
        }
    }
}
