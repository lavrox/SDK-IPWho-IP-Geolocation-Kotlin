# IPWho ([ipwho.org](https://www.ipwho.org)) Kotlin SDK

[![Kotlin](https://img.shields.io/badge/kotlin-1.9-7F52FF.svg)](https://kotlinlang.org/) [![license](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin/blob/main/LICENSE)

Official Kotlin client for the [IPWho](https://www.ipwho.org) IP Geolocation API. One call returns the **full** payload: geolocation, timezone, flag, currency, connection (ASN/ISP), security, and user-agent when present.

- Product: [ipwho.org](https://www.ipwho.org)
- API docs: [ipwho.org/docs](https://www.ipwho.org/docs)
- Get an API key: [ipwho.org/free-plan](https://www.ipwho.org/free-plan) (free [Lavrox](https://lavrox.com) account)
- Live API host: `https://api.ipwho.org`

## API key

Open a free [Lavrox](https://lavrox.com) account to get an API key for [IPWho](https://www.ipwho.org). Create your key at [ipwho.org/free-plan](https://www.ipwho.org/free-plan) — no credit card required.

## Installation

```kotlin
implementation("com.lavrox:ipwho-kotlin:1.0.0")
```

JDK 17+. HTTP uses Java's built-in `HttpClient`; the library dependencies are kotlinx.serialization (JSON) and kotlinx.coroutines. Apply `kotlin("plugin.serialization")`. Package: `com.ipwho.sdk`.

## Quick Start

```kotlin
val client = IpWhoClient(System.getenv("IPWHO_API_KEY"))

val resp = client.lookup("8.8.8.8")                    // GET /ip/{ip}
val me = client.me()                                   // GET /me
val bulk = client.bulk(listOf("8.8.8.8", "1.1.1.1"))   // BulkResponse
```

All public methods are `suspend`.

```
IpGeoResponse
├── success
├── message
└── data: GeoData?
    ├── ip
    ├── geoLocation
    ├── timezone
    ├── flag
    ├── currency
    ├── connection
    ├── security
    └── userAgent
```

Bulk: `BulkResponse.data.responseArray: List<IpGeoResponse>?`, one per address. Check each row's `success`. A failed row has a `message`, and its `data` holds only the `ip`.

The client sends your API key in the `Authorization` header, so it isn't part of the request URL.

## Reading the full response (8.8.8.8)

Live [IPWho](https://www.ipwho.org) values: United States, ASN 15169, America/Chicago, dial code +1.

```kotlin
val data = client.lookup("8.8.8.8").data
println(data?.ip) // 8.8.8.8

val geo = data?.geoLocation
println(geo?.country)       // United States
println(geo?.countryCode)   // US
println(geo?.continent)
println(geo?.continentCode)
println(geo?.capital)
println(geo?.region)
println(geo?.regionCode)
println(geo?.city)
println(geo?.postalCode)
println(geo?.dialCode)      // +1
println(geo?.isInEu)
println(geo?.latitude)
println(geo?.longitude)
println(geo?.accuracyRadius)

val tz = data?.timezone
println(tz?.timeZone) // America/Chicago
println(tz?.abbr)
println(tz?.offset)
println(tz?.isDst)
println(tz?.utc)
println(tz?.currentTime)

println(data?.flag?.flagIcon)    // 🇺🇸
println(data?.flag?.flagUnicode)

println(data?.currency?.code)
println(data?.currency?.namePlural) // US dollars

val conn = data?.connection
println(conn?.asnNumber)      // 15169
println(conn?.asnOrg)         // Google LLC
println(conn?.isp)
println(conn?.org)
println(conn?.domain)
println(conn?.connectionType) // Corporate

println(data?.security?.isVpn)
println(data?.security?.isTor)
println(data?.security?.isThreat)

println(data?.userAgent?.browser?.name)

println(client.me().data?.ip)

val bulk = client.bulk(listOf("8.8.8.8", "1.1.1.1"))
for (row in bulk.data?.responseArray.orEmpty()) {
    println("${row.data?.ip} ${if (row.success) row.data?.geoLocation?.country else row.message}")
}
```

`lookup` / `me` take `format` (`json`/`xml`/`csv`) and optional `fields`. For `xml` and `csv`, `data.ip` holds the raw response text.

## API Reference

### `IpWhoClient(apiKey, client = defaultHttpClient(), baseUrl, requestTimeout)`

The key is sent as `Authorization: Bearer`. `client` is a `java.net.http.HttpClient`. `baseUrl` defaults to `https://api.ipwho.org`; `requestTimeout` to 30 seconds.

### Errors

`IpWhoApiException(message, statusCode)` for API errors, including HTTP 429. Connection failures and timeouts throw `java.io.IOException`.

## Type Definitions

```kotlin
data class GeoLocation(
    val continent: String? = null,
    val continentCode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val capital: String? = null,
    val region: String? = null,
    val regionCode: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val dialCode: String? = null,
    val isInEu: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyRadius: Double? = null
)
```

Also `Timezone`, `Flag`, `Currency` (`namePlural`), `Connection` (`asnNumber`, `asnOrg`, `connectionType`), `Security` (`isVpn`, `isTor`, `isThreat`), `UserAgent`. `@SerialName` matches the live wire (`postal_Code`, `flag_Icon`, `isVpn`).

## Troubleshooting

- Key: [ipwho.org](https://www.ipwho.org).
- HTTP 403: the API rejects a blank User-Agent. The SDK sends `ipwho-kotlin-sdk/1.0.0`.
- Null nested objects on some IPs.

## Testing

```bash
./gradlew test
```

These tests need no API key: they run against a local stand-in server. `examples/test_ipwho.kt` is a live check, which uses lookups from your quota.

## Changelog

### v1.0.0

- `lookup`, `me`, `bulk` matching [api.ipwho.org](https://api.ipwho.org)

## License

MIT License — see [LICENSE](LICENSE).

## Support

- Documentation: [ipwho.org/docs](https://www.ipwho.org/docs)
- Contact: [ipwho.org/contact](https://www.ipwho.org/contact)
- GitHub Issues: [lavrox/SDK-IPWho-IP-Geolocation-Kotlin](https://github.com/lavrox/SDK-IPWho-IP-Geolocation-Kotlin/issues)
- Website: [ipwho.org](https://www.ipwho.org)

---

[IPWho](https://www.ipwho.org) — a [Lavrox](https://lavrox.com) network API.

[Lavrox](https://lavrox.com) — Independent API infrastructure. Lower latency, lower cost.
