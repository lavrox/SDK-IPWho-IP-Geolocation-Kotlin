package com.ipwho.sdk

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val KEY = "sk.test_secret_value"

/**
 * Runs against a local stand-in server that replays recorded API responses.
 * No API key and no network access are needed.
 */
class IpWhoClientTest {
    private class Recorded(val target: String, val authorization: String?, val userAgent: String?)

    private lateinit var server: HttpServer
    private lateinit var url: String
    private lateinit var client: IpWhoClient
    private val requests = mutableListOf<Recorded>()
    private var status = 200
    private var body = ""

    @BeforeTest
    fun start() {
        reply(200, fixture("lookup"))
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(
                Recorded(
                    exchange.requestURI.toString(),
                    exchange.requestHeaders.getFirst("Authorization"),
                    exchange.requestHeaders.getFirst("User-Agent")
                )
            )
            val bytes = body.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        url = "http://127.0.0.1:${server.address.port}"
        client = clientFor(KEY, url)
    }

    @AfterTest
    fun stop() = server.stop(0)

    private fun clientFor(key: String, baseUrl: String) = IpWhoClient(
        key,
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
        baseUrl,
        Duration.ofSeconds(5)
    )

    private fun reply(status: Int, body: String) {
        this.status = status
        this.body = body
    }

    private fun fixture(name: String): String =
        javaClass.getResource("/$name.json")!!.readText()

    @Test
    fun `the key is in the header and never in the URL`() = runBlocking<Unit> {
        client.lookup("8.8.8.8")
        client.me()
        reply(200, fixture("bulk"))
        client.bulk(listOf("8.8.8.8", "1.1.1.1"))

        assertEquals(3, requests.size)
        for (request in requests) {
            assertEquals("Bearer $KEY", request.authorization)
            assertEquals("ipwho-kotlin-sdk/1.0.0", request.userAgent)
            assertFalse(KEY in request.target, request.target)
            assertFalse("apikey" in request.target.lowercase(), request.target)
        }
    }

    @Test
    fun `lookup maps the response`() = runBlocking<Unit> {
        val data = client.lookup("8.8.8.8").data!!
        assertEquals("/ip/8.8.8.8", requests[0].target)
        assertEquals("8.8.8.8", data.ip)
        assertEquals("United States", data.geoLocation?.country)
        assertEquals("US", data.geoLocation?.countryCode)
        assertEquals("+1", data.geoLocation?.dialCode)
        assertEquals(15169, data.connection?.asnNumber)
        assertEquals("America/Chicago", data.timezone?.timeZone)
        assertEquals("USD", data.currency?.code)
    }

    @Test
    fun `lookup accepts a null currency`() = runBlocking<Unit> {
        reply(200, fixture("lookup-null-currency"))
        val data = client.lookup("1.1.1.1").data!!
        assertEquals("1.1.1.1", data.ip)
        assertNull(data.currency)
    }

    @Test
    fun `bulk returns the data for every address`() = runBlocking<Unit> {
        reply(200, fixture("bulk"))
        val ips = listOf("8.8.8.8", "1.1.1.1", "192.168.0.1", "9.9.9.9")
        val rows = client.bulk(ips).data!!.responseArray!!
        assertEquals(ips, rows.map { it.data?.ip })

        assertTrue(rows[0].success)
        assertEquals("United States", rows[0].data?.geoLocation?.country)
        assertEquals(15169, rows[0].data?.connection?.asnNumber)

        assertTrue(rows[1].success)
        assertNull(rows[1].data?.currency)

        assertFalse(rows[2].success)
        assertNull(rows[2].data?.geoLocation)
        assertEquals("Reserved range/Invalid IP address", rows[2].message)

        assertFalse(rows[3].success)
        assertTrue("quota" in rows[3].message!!)
    }

    @Test
    fun `a fields filter without ip still decodes`() = runBlocking<Unit> {
        reply(200, """{"success":true,"data":{"geoLocation":{"country":"United States"}}}""")
        val data = client.lookup("8.8.8.8", fields = "geoLocation,timezone").data!!
        assertEquals("/ip/8.8.8.8?get=geoLocation%2Ctimezone", requests[0].target)
        assertEquals("United States", data.geoLocation?.country)
        assertEquals("", data.ip)
    }

    @Test
    fun `csv format returns the raw text`() = runBlocking<Unit> {
        reply(200, "ip,8.8.8.8")
        assertEquals("ip,8.8.8.8", client.lookup("8.8.8.8", format = "csv").data?.ip)
        assertEquals("/ip/8.8.8.8?format=csv", requests[0].target)
    }

    @Test
    fun `HTTP errors throw with the status code`() = runBlocking<Unit> {
        for ((code, text) in listOf(
            404 to """{"success":false,"message":"Reserved range/Invalid IP address"}""",
            429 to """{"error":"rate_limit_exceeded"}""",
            401 to """{"error":"invalid_api_key"}"""
        )) {
            reply(code, text)
            val error = assertFailsWith<IpWhoApiException> { client.lookup("8.8.8.8") }
            assertEquals(code, error.statusCode)
            assertFalse(KEY in error.message.orEmpty())
        }
    }

    @Test
    fun `a connection failure throws IOException without the key`() = runBlocking<Unit> {
        val error = assertFailsWith<IOException> { clientFor(KEY, "http://127.0.0.1:1").lookup("8.8.8.8") }
        assertFalse(KEY in error.toString())
    }

    @Test
    fun `a key with surrounding whitespace is trimmed`() = runBlocking<Unit> {
        clientFor("  $KEY\n", url).lookup("8.8.8.8")
        assertEquals("Bearer $KEY", requests[0].authorization)
    }

    @Test
    fun `a key with invalid characters is rejected without repeating it`() {
        val error = assertFailsWith<IllegalArgumentException> { IpWhoClient("sk.secret_part\nmore") }
        assertFalse("secret_part" in error.message.orEmpty())
        assertFailsWith<IllegalArgumentException> { IpWhoClient("  \n") }
    }
}
