package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegMotionCadenceTest {
    @Test
    fun ordinaryCastsOnlyUseLegsOnEveryFourthFinish() {
        assertFalse(LegMotionCadence.shouldAnimate(0))
        assertFalse(LegMotionCadence.shouldAnimate(1))
        assertFalse(LegMotionCadence.shouldAnimate(2))
        assertFalse(LegMotionCadence.shouldAnimate(3))
        assertTrue(LegMotionCadence.shouldAnimate(4))
        assertFalse(LegMotionCadence.shouldAnimate(5))
        assertTrue(LegMotionCadence.shouldAnimate(8))
    }
}
