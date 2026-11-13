package com.slicehouse.pos.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.db.PosDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Paying through the real ViewModel and store. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckoutViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: PosDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var store: PosStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PosStore(db.dao(), folder.newFolder("files"), scope)
        runBlocking { withTimeout(10_000) { store.state.first { it.loaded } } }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun payingRecordsTheOrderAndClearsTheForm() {
        store.addLine(CartLine("", "d1", "Coca-Cola", "C", unitPrice = 250, qty = 2)) // 5.00 + 8% = 5.40
        val viewModel = CheckoutViewModel(store)

        viewModel.selectMethod(PaymentMethod.CASH)
        viewModel.setCashText("5")
        assertEquals(40L, viewModel.state.value.shortBy)
        assertNull(viewModel.pay())
        assertTrue(store.state.value.orders.isEmpty())

        viewModel.setCashText("10")
        val order = viewModel.pay()!!
        assertEquals(540L, order.total)
        assertEquals(460L, order.change)
        assertEquals(order, store.state.value.orders.single())
        assertTrue(store.state.value.cart.isEmpty())
        assertEquals(PaymentMethod.CARD, viewModel.state.value.method)
        assertEquals("", viewModel.state.value.cashText)
    }
}
