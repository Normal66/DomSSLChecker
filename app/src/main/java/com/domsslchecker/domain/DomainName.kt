package com.domsslchecker.domain

import java.net.URI
import java.net.URISyntaxException
import java.net.IDN
import java.util.Locale

@JvmInline
value class NormalizedDomainName(val value: String)

enum class DomainParseError {
    EMPTY,
    INVALID,
}

object DomainName {
    private val ASCII_LABEL = Regex("""^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$""")

    sealed class ParseResult {
        data class Ok(val domain: NormalizedDomainName) : ParseResult()
        data class Invalid(val error: DomainParseError) : ParseResult()
    }

    fun parseUserInput(input: String): ParseResult {
        val raw = input.trim()
        if (raw.isEmpty()) {
            return ParseResult.Invalid(DomainParseError.EMPTY)
        }

        val host = extractHost(raw) ?: return ParseResult.Invalid(DomainParseError.INVALID)
        val ascii = toAsciiHost(host) ?: return ParseResult.Invalid(DomainParseError.INVALID)
        if (!isValidAsciiDomain(ascii)) {
            return ParseResult.Invalid(DomainParseError.INVALID)
        }
        return ParseResult.Ok(NormalizedDomainName(ascii))
    }

    private fun extractHost(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.any { it.isWhitespace() }) return null

        // URL form: use URI parser (handles trailing slash, query, etc.).
        if ("://" in trimmed) {
            val uri = try {
                URI(trimmed)
            } catch (_: URISyntaxException) {
                return null
            }
            val h = uri.host ?: return null
            // Disallow userinfo and ports in user input for MVP.
            if (uri.userInfo != null) return null
            if (uri.port != -1) return null
            return h.trim().trimEnd('.')
        }

        // Bare domain form (can be unicode): cut at path/query/fragment.
        val cut = trimmed.indexOfAny(charArrayOf('/', '?', '#'))
        val hostOnly = (if (cut >= 0) trimmed.substring(0, cut) else trimmed).trim()
        if (hostOnly.isEmpty()) return null
        if ('@' in hostOnly) return null
        if (':' in hostOnly) return null // no ports / ipv6 in MVP input
        return hostOnly.trimEnd('.')
    }

    private fun toAsciiHost(host: String): String? {
        return try {
            IDN.toASCII(host.trim(), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidAsciiDomain(ascii: String): Boolean {
        if (ascii.isBlank()) return false
        if (ascii.length > 253) return false
        if (ascii.contains("..")) return false
        if (ascii.any { it == '_' }) return false
        val labels = ascii.split('.')
        if (labels.size < 2) return false
        if (labels.any { it.isEmpty() || it.length > 63 }) return false
        if (!labels.all { ASCII_LABEL.matches(it) }) return false
        val tld = labels.last()
        if (tld.length < 2) return false
        return true
    }
}
