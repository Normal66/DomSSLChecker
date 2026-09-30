package com.domsslchecker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainNameTest {
    @Test
    fun acceptsPlainDomain() {
        val r = DomainName.parseUserInput("Example.COM")
        assertTrue(r is DomainName.ParseResult.Ok)
        assertEquals("example.com", (r as DomainName.ParseResult.Ok).domain.value)
    }

    @Test
    fun stripsHttpsScheme() {
        val r = DomainName.parseUserInput("  https://example.com  ")
        assertTrue(r is DomainName.ParseResult.Ok)
        assertEquals("example.com", (r as DomainName.ParseResult.Ok).domain.value)
    }

    @Test
    fun acceptsCyrillicIdnDomain() {
        val r = DomainName.parseUserInput("найдись-ка.рф")
        assertTrue(r is DomainName.ParseResult.Ok)
        // Normalize to punycode ASCII
        assertEquals("xn----7sbboueks4b6g.xn--p1ai", (r as DomainName.ParseResult.Ok).domain.value)
    }

    @Test
    fun acceptsPunycodeUrlWithTrailingSlash() {
        val r = DomainName.parseUserInput("http://xn----7sbboueks4b6g.xn--p1ai/")
        assertTrue(r is DomainName.ParseResult.Ok)
        assertEquals("xn----7sbboueks4b6g.xn--p1ai", (r as DomainName.ParseResult.Ok).domain.value)
    }

    @Test
    fun rejectsPort() {
        val r = DomainName.parseUserInput("example.com:443")
        assertTrue(r is DomainName.ParseResult.Invalid)
    }
}
