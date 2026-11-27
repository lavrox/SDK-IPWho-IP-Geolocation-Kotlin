package com.ipwho.sdk

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ═══════════════════════════════════════════════════════════════════════════
// Exception
// ═══════════════════════════════════════════════════════════════════════════

class IpWhoApiException(message: String, val statusCode: Int = 0) : Exception(message)

// ═══════════════════════════════════════════════════════════════════════════
// Domain models — exact schema from ipwho-openapi.yaml
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class GeoLocation(
    @SerialName("continent") val continent: String? = null,
    @SerialName("continentCode") val continentCode: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("capital") val capital: String? = null,
    @SerialName("region") val region: String? = null,
    @SerialName("regionCode") val regionCode: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("postal_Code") val postalCode: String? = null,
    @SerialName("dial_code") val dialCode: String? = null,
    @SerialName("is_in_eu") val isInEu: Boolean? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("accuracy_radius") val accuracyRadius: Double? = null
)

@Serializable
data class Timezone(
    @SerialName("time_zone") val timeZone: String? = null,
    @SerialName("abbr") val abbr: String? = null,
    @SerialName("offset") val offset: Int? = null,
    @SerialName("is_dst") val isDst: Boolean? = null,
    @SerialName("utc") val utc: String? = null,
    @SerialName("current_time") val currentTime: String? = null
)

@Serializable
data class Flag(
    @SerialName("flag_Icon") val flagIcon: String? = null,
    @SerialName("flag_unicode") val flagUnicode: String? = null
)

@Serializable
data class Currency(
    @SerialName("code") val code: String? = null,
    @SerialName("symbol") val symbol: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("name_plural") val namePlural: String? = null,
    @SerialName("hex_unicode") val hexUnicode: String? = null
)

@Serializable
data class Connection(
    @SerialName("asn_number") val asnNumber: Int? = null,
    @SerialName("asn_org") val asnOrg: String? = null,
    @SerialName("isp") val isp: String? = null,
    @SerialName("org") val org: String? = null,
    @SerialName("domain") val domain: String? = null,
    @SerialName("connection_type") val connectionType: String? = null
)

@Serializable
data class Security(
    @SerialName("isVpn") val isVpn: Boolean? = null,
    @SerialName("isTor") val isTor: Boolean? = null,
    @SerialName("isThreat") val isThreat: String? = null // "low" | "medium" | "high"
)

@Serializable
data class Browser(
    @SerialName("name") val name: String? = null,
    @SerialName("version") val version: String? = null
)

@Serializable
data class Engine(
    @SerialName("name") val name: String? = null,
    @SerialName("version") val version: String? = null
)

@Serializable
data class OS(
    @SerialName("name") val name: String? = null,
    @SerialName("version") val version: String? = null
)

@Serializable
data class Device(
    @SerialName("type") val type: String? = null,
    @SerialName("vendor") val vendor: String? = null,
    @SerialName("model") val model: String? = null
)

@Serializable
data class CPU(
    @SerialName("architecture") val architecture: String? = null
)

@Serializable
data class UserAgent(
    @SerialName("browser") val browser: Browser? = null,
    @SerialName("engine") val engine: Engine? = null,
    @SerialName("os") val os: OS? = null,
    @SerialName("device") val device: Device? = null,
    @SerialName("cpu") val cpu: CPU? = null
)

/// The `data` payload inside a successful `IpGeoResponse`.
@Serializable
data class GeoData(
    /** Empty when a `fields` filter leaves it out. */
    @SerialName("ip") val ip: String = "",
    @SerialName("geoLocation") val geoLocation: GeoLocation? = null,
    @SerialName("timezone") val timezone: Timezone? = null,
    @SerialName("flag") val flag: Flag? = null,
    @SerialName("currency") val currency: Currency? = null,
    @SerialName("connection") val connection: Connection? = null,
    @SerialName("security") val security: Security? = null,
    @SerialName("userAgent") val userAgent: UserAgent? = null
)

/// Top-level API response.
@Serializable
data class IpGeoResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: GeoData? = null,
    @SerialName("message") val message: String? = null
)

/// Error payload returned on non-200 responses.
@Serializable
data class ErrorResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null
)

/**
 * Reads one bulk result. The API sends each row flat:
 * `{success, ip, geoLocation, ...}` or `{success: false, ip, message}`.
 * A row already nested under `data` is accepted too.
 */
internal object BulkRowSerializer :
    JsonTransformingSerializer<IpGeoResponse>(IpGeoResponse.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val row = element as? JsonObject ?: return element
        if ("data" in row) return row
        val wrapped = mutableMapOf<String, JsonElement>("data" to row)
        row["success"]?.let { wrapped["success"] = it }
        row["message"]?.let { wrapped["message"] = it }
        return JsonObject(wrapped)
    }
}

/// Bulk response wrapping a list of per-IP results.
@Serializable
data class BulkData(
    @SerialName("responseArray")
    val responseArray: List<@Serializable(with = BulkRowSerializer::class) IpGeoResponse>? = null
)

@Serializable
data class BulkResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: BulkData? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// Client
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Client for the IPWho API. HTTP uses Java's built-in [HttpClient].
 *
 * API errors throw [IpWhoApiException]. Connection failures and timeouts throw
 * `java.io.IOException`.
 *
 * @param apiKey         Your IPWho API key.
 * @param client         A custom [HttpClient], for example to use a proxy.
 * @param baseUrl        Override the API base URL.
 * @param requestTimeout How long to wait for each response.
 */
class IpWhoClient(
    apiKey: String,
    private val client: HttpClient = defaultHttpClient(),
    baseUrl: String = BASE_URL,
    private val requestTimeout: Duration = DEFAULT_TIMEOUT
) {
    private val apiKey: String = apiKey.trim()
    private val baseUrl: String = baseUrl.trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        require(this.apiKey.isNotEmpty()) { "apiKey is required" }
        // The key is sent in a header. Reject it here, without repeating it,
        // rather than let the HTTP client quote it in an error message.
        require(this.apiKey.all { it in ' '..'~' }) { "apiKey contains characters that are not allowed" }
    }

    companion object {
        private const val BASE_URL = "https://api.ipwho.org"
        private const val VERSION = "1.0.0"
        private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(30)

        fun defaultHttpClient(): HttpClient =
            HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build()
    }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Look up geolocation data for a specific IP address.
     *
     * @param ip    IPv4 or IPv6 address.
     * @param format Response format ("json", "xml", "csv"). For "xml" and "csv",
     *               `data.ip` holds the raw response text.
     * @param fields Optional comma-separated field filter (e.g. "geoLocation,timezone").
     * @return IpGeoResponse with nested data.
     */
    suspend fun lookup(
        ip: String,
        format: String = "json",
        fields: String? = null
    ): IpGeoResponse {
        return request("/ip/${encode(ip)}", format, fields)
    }

    /**
     * Look up geolocation data for the caller's own IP address.
     *
     * @param format Response format.
     * @param fields Optional comma-separated field filter.
     * @return IpGeoResponse with nested data.
     */
    suspend fun me(
        format: String = "json",
        fields: String? = null
    ): IpGeoResponse {
        return request("/me", format, fields)
    }

    /**
     * Perform a bulk IP lookup.
     *
     * @param ips List of IPv4/IPv6 addresses.
     * @return BulkResponse with one IpGeoResponse per address. Check each row's
     *         `success`. A failed row has a `message`, and its `data` holds only the `ip`.
     */
    suspend fun bulk(ips: List<String>): BulkResponse {
        require(ips.isNotEmpty()) { "IP list must not be empty" }
        val bulkParam = ips.joinToString(",") { encode(it) }
        val response = get("/bulk/$bulkParam", emptyList())
        throwOnError(response)

        val result = try {
            json.decodeFromString<BulkResponse>(response.body())
        } catch (e: Exception) {
            throw IpWhoApiException("Failed to parse bulk response: ${e.message}", response.statusCode())
        }
        if (!result.success) {
            throw IpWhoApiException("API returned success=false for bulk request", response.statusCode())
        }
        return result
    }

    // ── Internal ───────────────────────────────────────────────────────

    private suspend fun request(
        path: String,
        format: String,
        fields: String?
    ): IpGeoResponse {
        val query = mutableListOf<Pair<String, String>>()
        if (format != "json") query.add("format" to format)
        if (!fields.isNullOrBlank()) query.add("get" to fields)
        val response = get(path, query)
        throwOnError(response)

        if (format != "json") {
            return IpGeoResponse(success = true, data = GeoData(ip = response.body()))
        }

        val wrapper = try {
            json.decodeFromString<IpGeoResponse>(response.body())
        } catch (e: Exception) {
            throw IpWhoApiException("Failed to parse response: ${e.message}", response.statusCode())
        }
        if (!wrapper.success) {
            throw IpWhoApiException(wrapper.message ?: "API returned success=false", response.statusCode())
        }
        return wrapper
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /** Sends one GET request. The API key goes in a header, not in the URL. */
    private suspend fun get(path: String, query: List<Pair<String, String>>): HttpResponse<String> {
        val queryString =
            if (query.isEmpty()) ""
            else query.joinToString("&", prefix = "?") { (name, value) -> "$name=${encode(value)}" }
        val request = HttpRequest.newBuilder(URI.create(baseUrl + path + queryString))
            .timeout(requestTimeout)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", "ipwho-kotlin-sdk/$VERSION")
            .GET()
            .build()

        return suspendCancellableCoroutine { continuation ->
            val pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            continuation.invokeOnCancellation { pending.cancel(true) }
            pending.whenComplete { response, error ->
                if (error != null) {
                    val cause = if (error is CompletionException) error.cause ?: error else error
                    continuation.resumeWithException(cause)
                } else {
                    continuation.resume(response)
                }
            }
        }
    }

    private fun throwOnError(response: HttpResponse<String>) {
        val status = response.statusCode()
        if (status in 200..299) return
        val message = try {
            json.decodeFromString<ErrorResponse>(response.body()).message
        } catch (_: Exception) {
            null
        }
        val fallback = when (status) {
            429 -> "Rate limit exceeded"
            404 -> "IP not found"
            else -> "Request failed: HTTP $status"
        }
        throw IpWhoApiException(message ?: fallback, status)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Example usage
// ═══════════════════════════════════════════════════════════════════════════

// import kotlinx.coroutines.runBlocking
//
// fun main() {
//     val apiKey = System.getenv("IPWHO_API_KEY") ?: "sk.xxxx"
//     val client = IpWhoClient(apiKey)
//
//     runBlocking {
//         // Single IP lookup
//         try {
//             val result = client.lookup(ip = "8.8.8.8")
//             println("IP: ${result.data?.ip}")
//             println("City: ${result.data?.geoLocation?.city}")
//             println("Country: ${result.data?.geoLocation?.country}")
//             println("Currency: ${result.data?.currency?.code} (${result.data?.currency?.symbol})")
//         } catch (e: IpWhoApiException) {
//             println("Error [${e.statusCode}]: ${e.message}")
//         }
//
//         // Self lookup
//         try {
//             val me = client.me()
//             println("\nMy IP: ${me.data?.ip ?: "unknown"}")
//         } catch (e: IpWhoApiException) {
//             println("Error [${e.statusCode}]: ${e.message}")
//         }
//
//         // Bulk lookup
//         try {
//             val bulk = client.bulk(ips = listOf("8.8.8.8", "1.1.1.1"))
//             bulk.data?.responseArray?.forEach { item ->
//                 println("Bulk: ${item.data?.ip} → ${item.data?.geoLocation?.country}")
//             }
//         } catch (e: IpWhoApiException) {
//             println("Error [${e.statusCode}]: ${e.message}")
//         }
//     }
// }
