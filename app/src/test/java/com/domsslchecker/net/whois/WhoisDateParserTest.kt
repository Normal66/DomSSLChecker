package com.domsslchecker.net.whois

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoisDateParserTest {
    @Test
    fun parsesCommonRuPaidTillLine() {
        val text = """
            nserver:    ns1.example.ru.
            paid-till: 2025-12-11
        """.trimIndent()

        val c = WhoisDateParser.parse(text)
        assertEquals(1, c.size)
        assertTrue(c.first().line.contains("paid-till", ignoreCase = true))
    }

    @Test
    fun parsesFreeDateDottedRuFormat() {
        val text = """
            free-date: 1.1.2026
        """.trimIndent()
        val c = WhoisDateParser.parse(text)
        assertEquals(1, c.size)
    }

    @Test
    fun stripsPercentCoCCA() {
        val text = """
            %   paid-till:  2030-06-15
        """.trimIndent()
        val c = WhoisDateParser.parse(text)
        assertEquals(1, c.size)
    }
}
