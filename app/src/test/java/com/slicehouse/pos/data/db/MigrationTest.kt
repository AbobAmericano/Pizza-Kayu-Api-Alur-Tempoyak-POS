package com.slicehouse.pos.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Upgrades real older databases (version 1 from PR #2, version 2 from PR #4) and
 * checks nothing is lost. Room also checks each result matches its schemas/N.json.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PosDatabase::class.java)

    @Test
    fun version1OrdersSurviveTheUpgrade() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                """INSERT INTO orders (id, number, createdAt, type, tableLabel, customerName, customerPhone,
                   address, subtotalSen, taxSen, totalSen, payment, cashReceivedSen, changeSen, status,
                   note, pickupTime, refundedAt)
                   VALUES ('o1', 1004, 1731400000000, 'TAKEAWAY', '', 'Aina', '', '', 4600, 299, 4899,
                   'CASH', 5000, 101, 'COMPLETED', '', '19:30', NULL)"""
            )
            execSQL("INSERT INTO app_meta (`key`, value) VALUES ('next_order_number', '1005')")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true)

        db.query("SELECT number, totalSen, customerName, takenBy, refundedBy FROM orders").use { row ->
            assertTrue(row.moveToFirst())
            assertEquals(1004, row.getInt(0))
            assertEquals(4899L, row.getLong(1))
            assertEquals("Aina", row.getString(2))
            assertEquals("", row.getString(3))
            assertTrue(row.isNull(4))
        }
        db.query("SELECT value FROM app_meta WHERE `key` = 'next_order_number'").use { row ->
            assertTrue(row.moveToFirst())
            assertEquals("1005", row.getString(0))
        }
        db.query("SELECT COUNT(*) FROM staff").use { row ->
            assertTrue(row.moveToFirst())
            assertEquals(0, row.getInt(0)) // owner is created at first launch, not by the upgrade
        }
        db.close()
    }

    @Test
    fun version2OrdersAreWaitingToUploadAfterTheUpgrade() {
        helper.createDatabase(DB, 2).apply {
            execSQL(
                """INSERT INTO orders (id, number, createdAt, type, tableLabel, customerName, customerPhone,
                   address, subtotalSen, taxSen, totalSen, payment, cashReceivedSen, changeSen, status,
                   note, pickupTime, refundedAt, takenBy, refundedBy)
                   VALUES ('o2', 1010, 1731400000000, 'DINE_IN', 'T3', '', '', '', 2300, 184, 2484,
                   'CARD', 2484, 0, 'REFUNDED', '', '', 1731400100000, 'Aina', 'Nasran')"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true)

        db.query("SELECT number, takenBy, refundedBy, syncState, refundSyncState FROM orders").use { row ->
            assertTrue(row.moveToFirst())
            assertEquals(1010, row.getInt(0))
            assertEquals("Aina", row.getString(1))
            assertEquals("Nasran", row.getString(2))
            assertEquals(0, row.getInt(3)) // not uploaded yet
            assertEquals(0, row.getInt(4))
        }
        db.close()
    }

    @Test
    fun version3OrdersAreNotOnlineOrders() {
        helper.createDatabase(DB, 3).apply {
            execSQL(
                """INSERT INTO orders (id, number, createdAt, type, tableLabel, customerName, customerPhone,
                   address, subtotalSen, taxSen, totalSen, payment, cashReceivedSen, changeSen, status,
                   note, pickupTime, refundedAt, takenBy, refundedBy, syncState, refundSyncState)
                   VALUES ('o3', 1020, 1731400000000, 'TAKEAWAY', '', '', '', '', 2000, 160, 2160,
                   'CASH', 2160, 0, 'COMPLETED', '', '', NULL, 'Aina', NULL, 1, 0)"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true)

        db.query("SELECT number, syncState, online FROM orders").use { row ->
            assertTrue(row.moveToFirst())
            assertEquals(1020, row.getInt(0))
            assertEquals(1, row.getInt(1))
            assertEquals(0, row.getInt(2))
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
