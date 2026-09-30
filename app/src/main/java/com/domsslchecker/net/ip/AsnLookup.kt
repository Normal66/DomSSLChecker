package com.domsslchecker.net.ip

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class AsnInfo(
    val asn: Long,
    val name: String?,
    val description: String?,
)

object AsnLookup {
    private const val DUMMY_UA =
        "DomSSLChecker/1 (Android)"

    /**
     * ASN lookup by IP using public HTTPS API.
     *
     * Provider: bgpview.io (no API key).
     */
    fun tryLookup(ip: String): AsnInfo? {
        val u = URL("https://api.bgpview.io/ip/$ip")
        val conn = u.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", DUMMY_UA)
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 8_000
        conn.readTimeout = 10_000
        conn.useCaches = false
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        if (code !in 200..299) return null
        return parseBgpView(body)
    }

    fun parseBgpView(json: String): AsnInfo? {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: return null
            val asnObj = data.optJSONObject("asn") ?: return null
            val asn = asnObj.optLong("asn", -1L)
            if (asn <= 0) return null
            AsnInfo(
                asn = asn,
                name = asnObj.optString("name", "").takeIf { it.isNotBlank() },
                description = asnObj.optString("description_short", "").takeIf { it.isNotBlank() }
                    ?: asnObj.optString("description", "").takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            null
        }
    }
}

