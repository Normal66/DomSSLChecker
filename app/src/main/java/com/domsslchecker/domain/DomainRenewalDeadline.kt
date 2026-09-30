package com.domsslchecker.domain

import com.domsslchecker.data.db.DomainExpirySource
import com.domsslchecker.data.db.DomainRecordEntity
import java.time.Instant
import java.time.ZoneOffset
/**
 * Дедлайн продления/оплаты домена для UI и уведомлений.
 * WHOIS отдаёт дату окончания регистрации; оплатить обычно нужно раньше — минус 1 календарный месяц.
 * Ручная дата (MANUAL) трактуется как уже выбранный пользователем дедлайн, без сдвига.
 */
object DomainRenewalDeadline {
    fun renewalDeadlineUtcMillis(record: DomainRecordEntity): Long? {
        return when (record.domainExpirySource) {
            DomainExpirySource.MANUAL -> record.domainExpiryUtcManual ?: record.domainExpiryUtcAuto?.let(::minusOneCalendarMonthUtc)
            DomainExpirySource.AUTO -> {
                val auto = record.domainExpiryUtcAuto
                if (auto != null) minusOneCalendarMonthUtc(auto)
                else record.domainExpiryUtcManual
            }
        }
    }

    fun minusOneCalendarMonthUtc(registryExpiryUtcMillis: Long): Long {
        val instant = Instant.ofEpochMilli(registryExpiryUtcMillis)
        return instant.atZone(ZoneOffset.UTC)
            .toLocalDate()
            .minusMonths(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
}
