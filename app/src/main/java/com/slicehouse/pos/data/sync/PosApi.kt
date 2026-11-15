package com.slicehouse.pos.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Why a call to the server didn't work. */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No connection, or the server didn't answer. Try again later. */
    class Offline(cause: Throwable) : ApiException("No internet connection", cause)

    /** The device key is wrong or was disconnected on FireLord. */
    class Unauthorized : ApiException("This tablet is no longer connected to FireLord")

    /** The menu changed on the server since this tablet last downloaded it. */
    class Conflict(detail: String) : ApiException(detail)

    /** The server refused the request or failed (4xx/5xx). */
    class Http(val code: Int, detail: String) : ApiException(detail)
}

/**
 * The FireLord tablet API, under `<server>/pos/api/`. Every call runs on the
 * IO dispatcher and either returns the parsed answer or throws [ApiException].
 */
class PosApi(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = true }
) {

    suspend fun pair(server: HttpUrl, code: String, deviceName: String): PairResponse =
        call(server, "pair", key = null, method = "POST", body = encode(PairRequest.serializer(), PairRequest(code, deviceName)), PairResponse.serializer())

    /** Pass the version you have; the answer leaves the items out when it's current. */
    suspend fun menu(server: HttpUrl, key: String, version: Long?): MenuResponse {
        val path = if (version == null) "menu" else "menu?version=$version"
        return call(server, path, key, "GET", null, MenuResponse.serializer())
    }

    suspend fun putMenu(server: HttpUrl, key: String, upload: MenuUpload): MenuResponse =
        call(server, "menu", key, "PUT", encode(MenuUpload.serializer(), upload), MenuResponse.serializer())

    suspend fun uploadOrders(server: HttpUrl, key: String, orders: List<OrderDto>): OrdersResponse =
        call(server, "orders", key, "POST", encode(OrdersUpload.serializer(), OrdersUpload(orders)), OrdersResponse.serializer())

    suspend fun refund(server: HttpUrl, key: String, orderId: String, upload: RefundUpload) {
        call(server, "orders/$orderId/refund", key, "POST", encode(RefundUpload.serializer(), upload), ErrorResponse.serializer())
    }

    /** Online orders that still need the shop, oldest pickup first. */
    suspend fun onlineOrders(server: HttpUrl, key: String): OnlineOrdersResponse =
        call(server, "online-orders", key, "GET", null, OnlineOrdersResponse.serializer())

    /** Moves an online order on. A 409 ([ApiException.Conflict]) means the move isn't allowed any more. */
    suspend fun setOnlineStatus(server: HttpUrl, key: String, orderId: String, upload: StatusUpload): OnlineOrderDto =
        call(server, "online-orders/$orderId/status", key, "POST", encode(StatusUpload.serializer(), upload), OnlineOrderDto.serializer())

    private fun <T> encode(serializer: KSerializer<T>, value: T) = json.encodeToString(serializer, value)

    private suspend fun <T> call(
        server: HttpUrl,
        path: String,
        key: String?,
        method: String,
        body: String?,
        serializer: KSerializer<T>
    ): T = withContext(Dispatchers.IO) {
        val url = apiUrl(server, path)
        val request = Request.Builder()
            .url(url)
            .method(method, body?.toRequestBody(JSON))
            .header("Accept", "application/json")
            .apply { if (key != null) header("Authorization", "Bearer $key") }
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw ApiException.Offline(e)
        }
        response.use {
            val text = try {
                it.body?.string().orEmpty()
            } catch (e: IOException) {
                throw ApiException.Offline(e)
            }
            when {
                it.isSuccessful -> try {
                    json.decodeFromString(serializer, text)
                } catch (e: IllegalArgumentException) {
                    throw ApiException.Http(it.code, "The server sent an answer this app doesn't understand")
                }
                it.code == 401 -> throw ApiException.Unauthorized()
                it.code == 409 -> throw ApiException.Conflict(detail(text))
                else -> throw ApiException.Http(it.code, detail(text).ifBlank { "Server error ${it.code}" })
            }
        }
    }

    /** The server's reason, from {"detail": "..."} (a string, or a list of field errors for 422). */
    private fun detail(text: String): String =
        runCatching { json.decodeFromString(ErrorResponse.serializer(), text).detail }.getOrNull()
            ?: text.take(200)

    companion object {
        private val JSON = "application/json".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        internal fun apiUrl(server: HttpUrl, path: String): HttpUrl =
            server.resolve("pos/api/$path") ?: error("Bad API path $path")

        /**
         * Turns what the owner typed ("firelord.my", "https://firelord.my/") into the
         * server's root URL, or null if it isn't a usable address. Without a scheme,
         * https is assumed. Plain http is only allowed when [allowHttp] (debug builds).
         */
        fun parseServer(input: String, allowHttp: Boolean): HttpUrl? {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull() ?: return null
            if (!url.isHttps && !allowHttp) return null
            return url.newBuilder().encodedPath("/").query(null).fragment(null).build()
        }
    }
}
