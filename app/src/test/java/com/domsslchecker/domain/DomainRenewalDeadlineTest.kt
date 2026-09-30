package com.domsslchecker.domain

import com.domsslchecker.data.db.DomainExpirySource
import com.domsslchecker.data.db.DomainRecordEntity
import com.domsslchecker.data.db.SslStatus
import com.domsslchecker.data.db.UpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class DomainRenewalDeadlineTest {
    @Test
    fun auto_whois_minus_one_month() {
        val registry = Instant.parse("2026-03-15T12:00:00Z").toEpochMilli()
        val record = baseRecord().copy(
            domainExpiryUtcAuto = registry,
            domainExpirySource = DomainExpirySource.AUTO,
        )
        val expected = Instant.parse("2026-02-15T00:00:00Z").toEpochMilli()
        assertEquals(expected, DomainRenewalDeadline.renewalDeadlineUtcMillis(record))
    }

    @Test
    fun manual_without_offset() {
        val manual = Instant.parse("2026-02-01T00:00:00Z").toEpochMilli()
        val record = baseRecord().copy(
            domainExpiryUtcAuto = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli(),
            domainExpiryUtcManual = manual,
            domainExpirySource = DomainExpirySource.MANUAL,
        )
        assertEquals(manual, DomainRenewalDeadline.renewalDeadlineUtcMillis(record))
    }

    @Test
    fun no_dates_returns_null() {
        assertNull(DomainRenewalDeadline.renewalDeadlineUtcMillis(baseRecord()))
    }

    private fun baseRecord() = DomainRecordEntity(
        id = 1,
        domain = "example.com",
        domainExpiryUtcAuto = null,
        domainExpiryUtcManual = null,
        domainExpirySource = DomainExpirySource.AUTO,
        domainUpdateStatus = UpdateStatus.OK,
        domainUpdateLastRunUtc = null,
        domainUpdateLastError = null,
        sslLastCheckedUtc = null,
        sslStatus = SslStatus.OK,
        sslLastError = null,
        thresholdsOverrideDaysCsv = null,
        label = null,
        notes = null,
        registrar = null,
        hoster = null,
        nameServersCsv = null,
        createdAtUtc = 0,
        updatedAtUtc = 0,
    )
}
