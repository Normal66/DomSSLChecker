package com.domsslchecker.net.whois

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

data class WhoisDateCandidate(
    val line: String,
    val instantUtc: Long,
)

object WhoisDateParser {
    // Examples:
    // paid-till: 2025-12-11
    // free-date: 2025-12-11
    // Registry Expiry Date: 2025-12-11T00:00:00Z
    // Expiry date: 11-Dec-2025
    // paid-till: 11.12.2025
    private val LINE_HINTS = listOf(
        "expir",
        "paid",
        "paid-till",
        "free-date",
        "registrar registration exp",
        "registry exp",
        "valid until",
        "renewal",
        "domain registered",
        // CoCCA/кириллица в исходнике
        "истеч",
        "оконч",
    )

    private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
    private val DMY = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    private val DMY_DOT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT)
    private val DMY_SLASH = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT)
    private val YMD = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.ROOT)

    fun parse(text: String): List<WhoisDateCandidate> {
        val strict = parseFiltered(text, strictHints = true)
        if (strict.isNotEmpty()) return strict
        val loose = parseFiltered(text, strictHints = false)
        if (loose.isNotEmpty()) return loose
        return parseKeyValueFallback(text)
    }

    private fun normalizeLine(line: String): String {
        if (line.isEmpty() || !line.trimStart().startsWith('%')) return line
        // «%  paid-till: ...» (CoCCA)
        return line.trimStart { it == '%' || it == ' ' || it == '\t' }
    }

    private fun firstInstantInLine(line: String): Long? {
        // ISO instant
        val isoOffset = firstMatch(line, """(\d{4}-\d{2}-\d{2}T[0-9:+\-.Z]{2,30})""") { raw ->
            OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toInstant()
        }
        if (isoOffset != null) return isoOffset.toEpochMilli()

        // yyyy-MM-dd
        val ymd = firstMatch(line, """(\d{4}-\d{2}-\d{2})""") { raw ->
            LocalDate.parse(raw, ISO_DATE).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        if (ymd != null) return ymd.toEpochMilli()

        // dd-Mon-yyyy
        val dmy = firstMatch(line, """(\d{1,2}-[A-Za-z]{3}-\d{4})""") { raw ->
            LocalDate.parse(raw, DMY).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        if (dmy != null) return dmy.toEpochMilli()

        // d.m.y (RU), в т.ч. 1.2.2025
        val dmyDot = firstMatch(line, """(\d{1,2}\.\d{1,2}\.\d{4})""") { raw ->
            LocalDate.parse(raw, DMY_DOT).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        if (dmyDot != null) return dmyDot.toEpochMilli()

        // d/m/y (встречается в нотификациях/резюме WHOIS)
        val dmySlash = firstMatch(line, """(\d{1,2}/\d{1,2}/\d{4})""") { raw ->
            LocalDate.parse(raw, DMY_SLASH).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        if (dmySlash != null) return dmySlash.toEpochMilli()

        // yyyy.mm.dd
        val ymd2 = firstMatch(line, """(\d{4}\.\d{2}\.\d{2})""") { raw ->
            LocalDate.parse(raw, YMD).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        if (ymd2 != null) return ymd2.toEpochMilli()

        return null
    }

    private fun parseFiltered(text: String, strictHints: Boolean): List<WhoisDateCandidate> {
        val out = ArrayList<WhoisDateCandidate>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith('#')) continue
            val norm = normalizeLine(line)
            if (norm.isEmpty()) continue
            if (strictHints) {
                val low = norm.lowercase(Locale.ROOT)
                if (!LINE_HINTS.any { low.contains(it) }) {
                    continue
                }
            }

            val instant = firstInstantInLine(norm) ?: continue
            out.add(WhoisDateCandidate(line = norm, instantUtc = instant))
        }
        return out.distinctBy { it.instantUtc to it.line }
    }

    /**
     * Если дата в «хвосте» с переносами или с редким оформлением строк.
     */
    private fun parseKeyValueFallback(text: String): List<WhoisDateCandidate> {
        val p = Pattern.compile(
            // (?m) — без \R (старые API): только \n
            """(?im)(?:^|\n)\s*[*%]?\s*(paid-till|free-date)\s*:\s*([^\n]+)""",
        )
        val m = p.matcher(text)
        val out = ArrayList<WhoisDateCandidate>()
        while (m.find()) {
            val key = m.group(1) ?: continue
            val tail = m.group(2)?.trim() ?: continue
            val synthetic = "$key: $tail"
            val instant = firstInstantInLine(synthetic)
                ?: firstInstantInLine(tail)
                ?: continue
            out.add(WhoisDateCandidate(line = synthetic, instantUtc = instant))
        }
        return out.distinctBy { it.instantUtc to it.line }
    }

    private inline fun <T> firstMatch(
        line: String,
        pattern: String,
        map: (String) -> T,
    ): T? {
        val m = Pattern.compile(pattern).matcher(line)
        if (!m.find()) return null
        val g = m.group(1) ?: return null
        return try {
            map(g)
        } catch (_: Exception) {
            null
        }
    }
}
