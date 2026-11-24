package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElytraFirstPersonPoseTest {
    @Test
    fun groundStateStaysZeroAndGlidingRampsSmoothly() {
        var w = 0f
        repeat(6) {
            w = ElytraFirstPersonPose.stepWeight(w, fallFlying = true)
        }
        assertEquals(1f, w, 1.0e-5f)

        val midExit = ElytraFirstPersonPose.stepWeight(w, fallFlying = false)
        assertTrue(midExit in 0.8f..0.95f)
        repeat(8) {
            w = ElytraFirstPersonPose.stepWeight(w, fallFlying = false)
        }
        assertEquals(0f, w, 1.0e-5f)
    }

    @Test
    fun thirdPersonReturnsZeroAndFirstPersonClampsSafeOffsets() {
        assertEquals(
            ElytraFirstPersonPose.Result.ZERO,
            ElytraFirstPersonPose.compute(1f, 30f, 2f, firstPerson = false),
        )
        val pose = ElytraFirstPersonPose.compute(1f, 180f, 10f, firstPerson = true)
        assertTrue(pose.forwardOffset < 0f && pose.forwardOffset >= -ElytraFirstPersonPose.MAX_FORWARD_OFFSET)
        assertTrue(pose.verticalOffset < 0f && pose.verticalOffset >= -ElytraFirstPersonPose.MAX_VERTICAL_OFFSET)
        assertTrue(pose.pitchOffset in -ElytraFirstPersonPose.MAX_PITCH_OFFSET..ElytraFirstPersonPose.MAX_PITCH_OFFSET)
        assertTrue(pose.rollOffset in 0f..ElytraFirstPersonPose.MAX_ROLL_OFFSET)
    }

    @Test
    fun nonFiniteInputsNeverProduceNaN() {
        val pose = ElytraFirstPersonPose.compute(0.5f, Float.NaN, Float.POSITIVE_INFINITY, firstPerson = true)
        assertTrue(pose.pitchOffset.isFinite())
        assertTrue(pose.forwardOffset.isFinite())
        assertTrue(pose.verticalOffset.isFinite())
        assertTrue(pose.rollOffset.isFinite())
    }
}
