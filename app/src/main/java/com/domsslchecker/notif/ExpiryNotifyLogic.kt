package com.domsslchecker.notif

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object ExpiryNotifyLogic {
    fun daysLeftUtcFloor(now: Instant, expiry: Instant): Long {
        val startNow = now.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        val startExp = expiry.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
        return ChronoUnit.DAYS.between(startNow, startExp)
    }

    /** Наибольший порог T из списка, для которого daysLeft <= T. */
    fun largestApplicableThreshold(daysLeft: Long, thresholdsSortedAsc: List<Int>): Int? {
        return thresholdsSortedAsc.filter { daysLeft <= it }.maxOrNull()
    }
}
