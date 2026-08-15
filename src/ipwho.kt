// ---------------------------------------------------------------------------
// build.gradle.kts
// ---------------------------------------------------------------------------
// plugins {
//     kotlin("jvm") version "1.9.22"
//     kotlin("plugin.serialization") version "1.9.22"
//     `maven-publish`
// }
//
// group = "com.ipwho"
// version = "1.0.0"
//
// repositories { mavenCentral() }
//
// dependencies {
//     implementation("io.ktor:ktor-client-core:2.3.8")
//     implementation("io.ktor:ktor-client-cio:2.3.8")
//     implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
//     implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
//     implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
//     implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
//     testImplementation(kotlin("test"))
// }
//
// java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
// ---------------------------------------------------------------------------

package com.ipwho.sdk

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    @SerialName("ip") val ip: String,
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

/// Bulk response wrapping a list of per-IP results.
@Serializable
data class BulkData(
    @SerialName("responseArray") val responseArray: List<IpGeoResponse>? = null
)

@Serializable
data class BulkResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: BulkData? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// Client
// ═══════════════════════════════════════════════════════════════════════════

class IpWhoClient(
    private val apiKey: String,
    private val client: HttpClient = defaultHttpClient()
) {
    companion object {
        private const val BASE_URL = "https://api.ipwho.org"

        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Look up geolocation data for a specific IP address.
     *
     * @param ip    IPv4 or IPv6 address.
     * @param format Response format ("json", "xml", "csv").
     * @param fields Optional comma-separated field filter (e.g. "geoLocation,timezone").
     * @return IpGeoResponse with nested data.
     */
    suspend fun lookup(
        ip: String,
        format: String = "json",
        fields: String? = null
    ): IpGeoResponse {
        return request("/ip/$ip", format, fields)
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
     * @return BulkResponse wrapping an array of IpGeoResponse objects.
     */
    suspend fun bulk(ips: List<String>): BulkResponse {
        require(ips.isNotEmpty()) { "IP list must not be empty" }
        val bulkParam = ips.joinToString(",")
        val response: HttpResponse = client.get("$BASE_URL/bulk/$bulkParam") {
            parameter("apiKey", apiKey)
        }

        if (response.status == HttpStatusCode.TooManyRequests) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw IpWhoApiException(err?.message ?: "Rate limit exceeded", response.status.value)
        }
        if (!response.status.isSuccess()) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw IpWhoApiException(
                err?.message ?: "Request failed: ${response.status}",
                response.status.value
            )
        }

        return try {
            response.body()
        } catch (e: Exception) {
            throw IpWhoApiException("Failed to parse bulk response: ${e.message}", response.status.value)
        }
    }

    // ── Internal ───────────────────────────────────────────────────────

    private suspend fun request(
        path: String,
        format: String,
        fields: String?
    ): IpGeoResponse {
        val response: HttpResponse = client.get("$BASE_URL$path") {
            parameter("apiKey", apiKey)
            if (format != "json") parameter("format", format)
            if (!fields.isNullOrBlank()) parameter("get", fields)
        }

        if (response.status == HttpStatusCode.TooManyRequests) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw IpWhoApiException(err?.message ?: "Rate limit exceeded", response.status.value)
        }
        if (response.status == HttpStatusCode.NotFound) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw IpWhoApiException(err?.message ?: "IP not found", response.status.value)
        }
        if (!response.status.isSuccess()) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw IpWhoApiException(
                err?.message ?: "Request failed: ${response.status}",
                response.status.value
            )
        }

        return try {
            val wrapper = response.body<IpGeoResponse>()
            if (!wrapper.success) {
                throw IpWhoApiException(wrapper.message ?: "API returned success=false", response.status.value)
            }
            wrapper
        } catch (e: IpWhoApiException) {
            throw e
        } catch (e: Exception) {
            throw IpWhoApiException("Failed to parse response: ${e.message}", response.status.value)
        }
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
