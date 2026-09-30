package com.domsslchecker.net.whois

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WhoisRuWebClientTest {

    @Test
    fun htmlToText_parsesComSample() {
        val html = """
            <html><body>
            <h2>Информация о домене: <b>nlp-tech.com</b></h2>
            <p>Domain Name: NLP-TECH.COM</p>
            <p>Registry Expiry Date: 2026-09-24T13:46:40Z</p>
            <p>Updated Date: 2025-08-13T05:17:46Z</p>
            <p>Дата окончания: 2026.09.24 16:46:40 MSK</p>
            <p>Популярные запросы</p>
            </body></html>
        """.trimIndent()
        val text = WhoisRuWebClient.htmlToWhoisLikeText(html)
        assertNotNull(text)
        val c = WhoisDateParser.parse(text!!)
        assert(c.isNotEmpty())
        val latest = c.maxBy { it.instantUtc }
        // Позднее обновления 2025-08, ожидаем сентябрь 2026 (реестр / кириллица)
        assertEquals(2026, java.time.Instant.ofEpochMilli(latest.instantUtc)
            .atZone(java.time.ZoneOffset.UTC).year)
    }
}
