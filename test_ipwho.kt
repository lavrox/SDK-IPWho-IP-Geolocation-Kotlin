import com.ipwho.sdk.IpWhoClient
import com.ipwho.sdk.IpWhoApiException
import kotlinx.coroutines.runBlocking

var pass = 0
var fail = 0
fun ok(c: Boolean, m: String) {
    if (c) { pass++; println("  PASS $m") } else { fail++; println("  FAIL $m") }
}

fun main() = runBlocking {
    val key = System.getenv("IPWHO_API_KEY")
    val c = IpWhoClient(key)

    // 1. lookup
    val r = c.lookup("8.8.8.8")
    val d = r.data!!
    val gl = d.geoLocation!!
    val tz = d.timezone!!
    val fl = d.flag!!
    val cu = d.currency!!
    val cn = d.connection!!
    ok(d.ip == "8.8.8.8", "lookup ip == 8.8.8.8")
    ok(gl.country == "United States", "country == United States (got ${gl.country})")
    ok(cn.asnNumber == 15169, "asn_number == 15169 (got ${cn.asnNumber})")
    ok(gl.dialCode != null, "dial_code captured (${gl.dialCode})")
    ok(gl.isInEu != null, "is_in_eu captured")
    ok(tz.timeZone != null, "time_zone captured (${tz.timeZone})")
    ok(fl.flagIcon != null, "flag_Icon captured (${fl.flagIcon})")
    ok(fl.flagUnicode != null, "flag_unicode captured (${fl.flagUnicode})")
    ok(cu.namePlural != null, "name_plural captured (${cu.namePlural})")
    ok(cn.asnOrg != null, "asn_org captured (${cn.asnOrg})")
    ok(cn.connectionType != null, "connection_type captured (${cn.connectionType})")

    // 2. me
    val me = c.me()
    ok(me.data?.ip?.isNotEmpty() == true, "me ip captured (${me.data?.ip})")

    // 3. bulk
    val b = c.bulk(listOf("8.8.8.8", "1.1.1.1"))
    val n = b.data?.responseArray?.size ?: 0
    ok(n == 2, "bulk returns 2 (got $n)")

    // 4. bad key
    try {
        IpWhoClient("sk.invalid_test_key").lookup("8.8.8.8")
        ok(false, "bad key should raise")
    } catch (e: IpWhoApiException) {
        ok(true, "bad key raised ${e.javaClass.simpleName}")
    } catch (e: Exception) {
        ok(true, "bad key raised ${e.javaClass.simpleName}")
    }

    println("\nKOTLIN RESULT: $pass passed, $fail failed")
    if (fail > 0) System.exit(1)
}
