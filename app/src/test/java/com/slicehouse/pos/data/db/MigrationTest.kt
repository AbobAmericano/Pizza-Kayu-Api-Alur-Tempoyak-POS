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
 * Upgrades a real version 1 database (as shipped in PR #2) to version 2 and
 * checks nothing is lost. Room also checks the result matches schemas/2.json.
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

    private companion object {
        const val DB = "migration-test.db"
    }
}
