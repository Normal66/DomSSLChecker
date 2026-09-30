package com.domsslchecker.net.whois

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * WHOIS с сайта [whois.ru](https://whois.ru/nlp-tech.com) — HTML по HTTPS, без TCP :43.
 */
object WhoisRuWebClient {

    private const val BASE = "https://whois.ru/"

    private const val DUMMY_UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    @Throws(IOException::class)
    fun fetchWhoisPlainText(asciiDomain: String): String? {
        val d = asciiDomain.trim().lowercase()
        if (d.isEmpty() || d.any { it in "/\\?#%" }) {
            return null
        }
        val u = URL(BASE + d)
        val conn = u.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", DUMMY_UA)
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
        conn.connectTimeout = 15_000
        conn.readTimeout = 25_000
        conn.useCaches = false
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        if (code !in 200..299) return null
        return htmlToWhoisLikeText(body)
    }

    fun htmlToWhoisLikeText(html: String): String? {
        var t = html.replace(Regex("(?i)<(script|style)[^>]*>[\\s\\S]*?</\\1>"), " ")
        t = t.replace(Regex("(?i)<br\\s*/?>"), "\n")
        t = t.replace(Regex("(?i)</(p|h[1-6]|tr|li|/table|/section|/article|/div|h3)>"), "\n")
        t = t.replace(Regex("<[^>]+>"), "\n")
        t = t.replace(Regex("(?m) +$"), "")
        t = t.replace(Regex("(?m)^ +"), "")
        t = t.replace(Regex("\n{3,}"), "\n\n")
        t = t.trim()
        if (t.length < 20) return null
        if (!t.contains("domain", ignoreCase = true) && !t.contains("реестр", ignoreCase = true)) {
            return null
        }
        val from = t.indexOf("domain", 0, ignoreCase = true)
        if (from >= 0) {
            var end = t.length
            for (m in listOf("Популярные запросы", "Популярные", "For more information", "TERMS OF USE: You are not")) {
                val i = t.indexOf(m, from, ignoreCase = true)
                if (i > from) {
                    end = minOf(end, i)
                }
            }
            if (end > from) {
                t = t.substring(from, end).trim()
            }
        }
        return t.ifBlank { null }
    }
}
