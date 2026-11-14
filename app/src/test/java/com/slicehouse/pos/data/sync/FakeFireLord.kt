package com.slicehouse.pos.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy

/**
 * A small stand-in for FireLord's /pos/api/, enough to test the tablet's sync:
 * it keeps a menu with a version, stores uploaded orders and refunds, and can
 * be told to go "offline", revoke the key or refuse orders.
 */
class FakeFireLord : Dispatcher() {
    private val json = Json { ignoreUnknownKeys = true }

    val code = "123456"
    val key = "pos_test-key"
    var shopName = "Pizza Kayu Api"
    var tax = 800
    var version = 0L
    var items = listOf<MenuItemDto>()
    var toppings = listOf<ToppingDto>()

    val orders = linkedMapOf<String, OrderDto>()
    val refunds = mutableMapOf<String, RefundUpload>()
    val requests = mutableListOf<String>()

    /** Answer every request with a dropped connection. */
    @Volatile var offline = false
    @Volatile var revoked = false
    /** Order numbers the server refuses with "rejected". */
    val rejectNumbers = mutableSetOf<Int>()
    /** Order numbers that make the whole request fail with 422, like a field Ninja can't validate. */
    val invalidNumbers = mutableSetOf<Int>()

    // MockWebServer asks peek() before reading a request; dropping the connection there
    // looks to the app exactly like having no internet.
    override fun peek(): MockResponse =
        if (offline) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START) else super.peek()

    @Synchronized
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (offline) return MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
        val path = request.requestUrl!!.encodedPath.removePrefix("/pos/api/")
        val body = request.body.readUtf8()
        requests += "${request.method} $path"

        if (path == "pair") {
            val code = json.parseToJsonElement(body).jsonObject["code"]!!.jsonPrimitive.content
            if (code != this.code) return error(400, "That code is wrong or has expired.")
            return ok(json.encodeToString(PairResponse.serializer(), PairResponse(key, 1, shopName, "pizza-kayu-api")))
        }
        if (revoked || request.getHeader("Authorization") != "Bearer $key") return error(401, "Unauthorized")

        return when {
            path == "menu" && request.method == "GET" -> {
                val have = request.requestUrl!!.queryParameter("version")?.toLong()
                ok(menuJson(changed = have != version))
            }
            path == "menu" && request.method == "PUT" -> {
                val upload = json.decodeFromString(MenuUpload.serializer(), body)
                if (upload.baseVersion != version) return error(409, "The menu changed (now version $version).")
                items = upload.items
                toppings = upload.toppings
                version += 1
                ok(menuJson(changed = true))
            }
            path == "orders" -> {
                val upload = json.decodeFromString(OrdersUpload.serializer(), body)
                if (upload.orders.any { it.number in invalidNumbers }) return error(422, "Invalid order")
                val results = upload.orders.map { order ->
                    when {
                        order.number in rejectNumbers -> OrderResult(order.id, "rejected", "Totals don't add up")
                        order.id in orders -> OrderResult(order.id, "duplicate")
                        else -> OrderResult(order.id, "accepted").also { orders[order.id] = order }
                    }
                }
                ok(json.encodeToString(OrdersResponse.serializer(), OrdersResponse(results)))
            }
            path.startsWith("orders/") && path.endsWith("/refund") -> {
                val id = path.removePrefix("orders/").removeSuffix("/refund")
                if (id !in orders) return error(404, "No such order")
                refunds[id] = json.decodeFromString(RefundUpload.serializer(), body)
                ok("""{"id": "$id", "status": "REFUNDED", "refunded_at": null, "refunded_by": ""}""")
            }
            else -> error(404, "Not found")
        }
    }

    /** Pretend the owner changed the menu on the web. */
    @Synchronized
    fun editOnWeb(block: (List<MenuItemDto>) -> List<MenuItemDto>) {
        items = block(items)
        version += 1
    }

    private fun menuJson(changed: Boolean): String = json.encodeToString(
        MenuResponse.serializer(),
        MenuResponse(
            version = version, changed = changed, shopName = shopName, currency = "RM", taxBasisPoints = tax,
            items = if (changed) items else emptyList(), toppings = if (changed) toppings else emptyList()
        )
    )

    private fun ok(body: String) = MockResponse().setResponseCode(200).setBody(body)
        .setHeader("Content-Type", "application/json")

    private fun error(code: Int, detail: String) = MockResponse().setResponseCode(code)
        .setBody(JsonObject(mapOf("detail" to kotlinx.serialization.json.JsonPrimitive(detail))).toString())
}
