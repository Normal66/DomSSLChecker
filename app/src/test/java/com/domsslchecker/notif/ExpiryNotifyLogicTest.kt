package com.domsslchecker.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ExpiryNotifyLogicTest {
    @Test
    fun days_left_utc_floor() {
        val now = Instant.parse("2026-03-10T15:00:00Z")
        val expiry = Instant.parse("2026-03-15T01:00:00Z")
        assertEquals(5, ExpiryNotifyLogic.daysLeftUtcFloor(now, expiry))
    }

    @Test
    fun largest_applicable_threshold() {
        val thresholds = listOf(1, 3, 7, 14, 30)
        assertEquals(30, ExpiryNotifyLogic.largestApplicableThreshold(25, thresholds))
        assertEquals(30, ExpiryNotifyLogic.largestApplicableThreshold(5, thresholds))
        assertNull(ExpiryNotifyLogic.largestApplicableThreshold(31, thresholds))
    }
}
