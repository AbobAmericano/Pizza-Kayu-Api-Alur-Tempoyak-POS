package com.slicehouse.pos.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.Topping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Admin sign-in state shown in Settings → Firebase. */
data class FirebaseAdminState(
    val signedIn: Boolean = false,
    val admin: Boolean = false,
    val email: String = "",
    val message: String? = null
)

/**
 * The Cloud Firestore half of online ordering. The web app writes orders into
 * `online_orders`; this manager listens in real time, writes staff status changes
 * back to the same document, and publishes the tablet's menu/availability to
 * `menus`, `toppings` and `settings`.
 *
 * Money is whole sen everywhere, matching Money.kt and the web app. Status
 * strings are the POS vocabulary (PENDING / RECEIVED / PREPARING / BAKING /
 * READY / COMPLETED / CANCELLED) so the Online tab, badge and beep keep working.
 *
 * Firestore writes need an authenticated admin: a Firebase Auth user with the
 * `admin: true` custom claim. See firestore.rules and README.md.
 */
class FirebaseSyncManager(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope
) {
    private val _orders = MutableStateFlow<List<OnlineOrderDto>>(emptyList())
    /** Live web orders, already mapped to the DTO the Online screen renders. */
    val orders: StateFlow<List<OnlineOrderDto>> = _orders.asStateFlow()

    private val _state = MutableStateFlow(FirebaseAdminState())
    val state: StateFlow<FirebaseAdminState> = _state.asStateFlow()

    /** Called when a brand new PENDING order appears (the app plays its beep). */
    var onNewPendingOrder: () -> Unit = {}

    private var listener: ListenerRegistration? = null
    private var seenPending: Set<String>? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                seenPending = null
                _orders.value = emptyList()
                stopListening()
                _state.value = FirebaseAdminState()
            } else {
                _state.update { it.copy(signedIn = true, email = user.email.orEmpty()) }
                refreshClaims()
            }
        }
        refreshClaims()
    }

    /**
     * Starts the real-time listener. Listing all orders requires the admin claim, so
     * this attaches only after a successful admin sign-in and detaches otherwise.
     */
    fun startListening() {
        if (_state.value.admin) attachListener() else detachListener()
    }

    private fun attachListener() {
        if (listener != null) return
        listener = firestore.collection(COLLECTION_ORDERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update { it.copy(message = "Firestore: ${error.message}") }
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { it.toOnlineOrderDto() } ?: emptyList()
                val pending = orders.filter { it.status == "PENDING" }.map { it.id }.toSet()
                val seen = seenPending
                if (seen != null && (pending - seen).isNotEmpty()) onNewPendingOrder()
                seenPending = pending
                _orders.value = orders
            }
    }

    private fun detachListener() {
        listener?.remove()
        listener = null
    }

    fun stopListening() {
        detachListener()
        seenPending = null
    }

    suspend fun signInAdmin(email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            Tasks.await(auth.signInWithEmailAndPassword(email.trim(), password))
            val user = auth.currentUser ?: error("No signed-in user")
            val result = Tasks.await(user.getIdToken(true))
            val admin = result.claims[CLAIM_ADMIN] == true
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        signedIn = true,
                        admin = admin,
                        email = auth.currentUser?.email.orEmpty(),
                        message = if (admin) "Signed in as an admin." else "This account is not an admin. Set the admin claim with the Admin SDK."
                    )
                }
                if (admin) attachListener() else detachListener()
            }
            if (admin) null else "This account is not an admin. Ask the owner to set the admin claim."
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { _state.update { it.copy(message = e.message ?: "Sign-in failed") } }
            e.message ?: "Sign-in failed"
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        auth.signOut()
        withContext(Dispatchers.Main) {
            detachListener()
            _state.value = FirebaseAdminState()
        }
    }

    private fun refreshClaims() {
        val user = auth.currentUser ?: return
        user.getIdToken(true).addOnSuccessListener { result ->
            val admin = result.claims[CLAIM_ADMIN] == true
            _state.update { it.copy(signedIn = true, admin = admin, email = user.email.orEmpty()) }
            if (admin) attachListener() else detachListener()
        }.addOnFailureListener { error ->
            _state.update { it.copy(message = error.message) }
        }
    }

    /** Mirrors a staff action to the customer's tracking document. */
    suspend fun updateStatus(
        order: OnlineOrderDto,
        status: String,
        payment: String,
        by: String,
        reason: String
    ): String? = withContext(Dispatchers.IO) {
        if (!_state.value.admin) return@withContext "Sign in as an admin in Settings → Firebase first."
        val changes = mutableMapOf<String, Any>(
            FIELD_STATUS to status,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            FIELD_HISTORY to FieldValue.arrayUnion(
                mapOf(FIELD_STATUS to status, "at" to Timestamp.now(), "by" to by.take(60))
            ),
            FIELD_PAID_BY to by.take(60),
            FIELD_CANCEL_REASON to reason
        )
        if (payment.isNotBlank()) changes[FIELD_PAYMENT] = payment
        try {
            Tasks.await(firestore.collection(COLLECTION_ORDERS).document(order.id).update(changes))
            // Keep our local copy in step even before the snapshot listener fires.
            _orders.update { list -> list.map { if (it.id == order.id) it.copy(status = status, payment = payment.ifBlank { it.payment }, cancelReason = reason) else it } }
            null
        } catch (e: Exception) {
            e.message ?: "Firestore refused the change."
        }
    }

    /** Full menu publish from the POS Menu Manager (prices, photos, sizes, taxes). */
    suspend fun publishMenu(
        items: List<MenuItem>,
        toppings: List<Topping>,
        taxBasisPoints: Int,
        shopName: String,
        currency: String
    ): String? = withContext(Dispatchers.IO) {
        if (!_state.value.admin) return@withContext null // FireLord-only setup: nothing to publish.
        try {
            val batch = firestore.batch()
            items.forEachIndexed { index, item ->
                batch.set(firestore.collection(COLLECTION_MENUS).document(item.id), mapOf(
                    "id" to item.id,
                    "name" to item.name.take(100),
                    "description" to item.description.take(300),
                    "emoji" to item.emoji.take(16),
                    "imageKey" to item.imageKey.take(64),
                    "category" to item.category.name,
                    "priceSen" to item.price,
                    "available" to item.available,
                    "isPizza" to item.isPizza,
                    "twelveInchAvailable" to item.twelveInchAvailable,
                    "twelveInchDeltaSen" to item.twelveInchDelta,
                    "sortIndex" to index,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                ))
            }
            toppings.forEach { topping ->
                batch.set(firestore.collection(COLLECTION_TOPPINGS).document(topping.id), mapOf(
                    "id" to topping.id,
                    "label" to topping.label.take(60),
                    "emoji" to topping.emoji.take(16),
                    "priceSen" to topping.price
                ))
            }
            batch.set(firestore.collection(COLLECTION_SETTINGS).document("general"), mapOf(
                "shopName" to shopName,
                "currency" to currency,
                "taxBasisPoints" to taxBasisPoints,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp()
            ), SetOptions.merge())
            Tasks.await(batch.commit())
            null
        } catch (e: Exception) {
            e.message ?: "Could not publish the menu to Firestore."
        }
    }

    /** True when this order belongs to Firestore rather than the FireLord REST server. */
    fun isFirestoreOrder(orderId: String): Boolean = _orders.value.any { it.id == orderId }

    private fun DocumentSnapshot.toOnlineOrderDto(): OnlineOrderDto? {
        val data = data ?: return null
        fun text(name: String): String = data[name] as? String ?: ""
        fun sen(name: String): Long = (data[name] as? Number)?.toLong() ?: 0L
        fun time(value: Any?): String = when (value) {
            is Timestamp -> isoTime(value.toInstant().toEpochMilli())
            is Number -> isoTime(value.toLong())
            is String -> value
            else -> isoTime(0L)
        }

        val lines = (data["items"] as? List<*>)?.mapNotNull { entry ->
            val line = entry as? Map<*, *> ?: return@mapNotNull null
            OrderLineDto(
                lineId = line["lineId"] as? String ?: "",
                itemId = line["itemId"] as? String ?: "",
                name = line["name"] as? String ?: "",
                emoji = line["emoji"] as? String ?: "",
                unitPriceSen = (line["unitPriceSen"] as? Number)?.toLong() ?: 0L,
                qty = (line["qty"] as? Number)?.toInt() ?: 1,
                sizeLabel = line["sizeLabel"] as? String,
                crustLabel = null,
                toppings = (line["toppings"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                note = line["note"] as? String ?: ""
            )
        } ?: emptyList()

        return OnlineOrderDto(
            id = id,
            number = sen("orderNumber").toInt(),
            label = text("label").ifBlank { "W${sen("orderNumber")}" },
            status = text("status"),
            createdAt = time(data["createdAt"]),
            pickupAt = data["pickupAt"]?.let(::time),
            pickupTime = text("pickupTime"),
            customerName = text("customerName"),
            customerPhone = text("customerPhone"),
            note = text("note"),
            subtotalSen = sen("subtotalSen"),
            taxSen = sen("taxSen"),
            totalSen = sen("totalSen"),
            payment = text("payment"),
            cancelReason = text("cancelReason"),
            lines = lines
        )
    }

    companion object {
        const val COLLECTION_ORDERS = "online_orders"
        const val COLLECTION_MENUS = "menus"
        const val COLLECTION_TOPPINGS = "toppings"
        const val COLLECTION_SETTINGS = "settings"
        const val CLAIM_ADMIN = "admin"
        const val FIELD_STATUS = "status"
        const val FIELD_PAYMENT = "payment"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_HISTORY = "statusHistory"
        const val FIELD_PAID_BY = "pickedUpBy"
        const val FIELD_CANCEL_REASON = "cancelReason"
    }
}
