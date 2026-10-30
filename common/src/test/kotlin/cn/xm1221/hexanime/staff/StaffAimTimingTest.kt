package cn.xm1221.hexanime.staff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaffAimTimingTest {
    @Test
    fun refreshIntervalCannotOutliveTheRemoteTimeout() {
        assertEquals(StaffAimTiming.MAX_REFRESH_TICKS, StaffAimTiming.refreshTicks(100))
        assertTrue(StaffAimTiming.MAX_REFRESH_TICKS.toFloat() < StaffAimTiming.REMOTE_TIMEOUT_TICKS)
    }

    @Test
    fun refreshIntervalKeepsItsExistingLowerBound() {
        assertEquals(StaffAimTiming.MIN_REFRESH_TICKS, StaffAimTiming.refreshTicks(0))
        assertEquals(20, StaffAimTiming.refreshTicks(20))
    }
}
