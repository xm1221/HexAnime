package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CastItemActivityTest {
    @Test
    fun idleStaffWithOvercastKeepsTheAnimationLayerActive() {
        assertTrue(
            CastItemActivity.shouldRun(
                enabled = true,
                hasAim = false,
                hasPulse = false,
                hasMicroPulse = false,
                hasFatigue = false,
                overcastStrain = 0.8f,
            ),
        )
    }

    @Test
    fun disabledConfigOrNoFeedbackKeepsTheLayerInactive() {
        assertFalse(
            CastItemActivity.shouldRun(
                enabled = false,
                hasAim = true,
                hasPulse = true,
                hasMicroPulse = true,
                hasFatigue = true,
                overcastStrain = 1f,
            ),
        )
        assertFalse(
            CastItemActivity.shouldRun(
                enabled = true,
                hasAim = false,
                hasPulse = false,
                hasMicroPulse = false,
                hasFatigue = false,
                overcastStrain = 0f,
            ),
        )
    }
}
