package com.slicehouse.pos.data

/** What happened to a menu change made on the tablet. */
sealed interface MenuChange {
    data object Saved : MenuChange

    /** Connected to FireLord but there's no internet, so nothing was changed. */
    data object Offline : MenuChange

    /** Someone changed the menu on FireLord first. The tablet now shows that menu. */
    data object Conflict : MenuChange

    data class Failed(val reason: String) : MenuChange
}

/**
 * Where the master copy of the menu lives. While the tablet is connected to
 * FireLord the server is the master (see [com.slicehouse.pos.data.sync.SyncManager]),
 * so menu edits go there first. Not connected: [PosStore] changes it locally.
 */
interface MenuMaster {
    val isConnected: Boolean

    /** Sends the whole new menu to the server. On [MenuChange.Saved] the store already shows it. */
    suspend fun publish(menu: List<MenuItem>, toppings: List<Topping>): MenuChange

    /** A sold-out switch changed on the tablet. It works offline and is sent on the next sync. */
    fun availabilityChanged(itemId: String, available: Boolean)
}
