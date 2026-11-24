package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquipDrawAndLocomotionTest {
    @Test
    fun equipDrawReturnsZeroOutsideWindowOrWhenDisabled() {
        assertEquals(EquipDrawCurve.Pose.ZERO, EquipDrawCurve.evaluate(-1f, true, -1f, true))
        assertEquals(EquipDrawCurve.Pose.ZERO, EquipDrawCurve.evaluate(12f, true, -1f, true))
        assertEquals(EquipDrawCurve.Pose.ZERO, EquipDrawCurve.evaluate(5f, true, -1f, false))

        val staffMid = EquipDrawCurve.evaluate(7.5f, isStaff = true, inward = -1f, enabled = true)
        val packagedMid = EquipDrawCurve.evaluate(7.5f, isStaff = false, inward = -1f, enabled = true)
        assertTrue(staffMid.itemRoll > 1.0f)
        assertTrue(packagedMid.itemY < -0.10f)
        assertTrue(staffMid.firstPersonRoll > 0.7f)
    }

    @Test
    fun locomotionAdaptsCrouchAirborneAndSprintingThrows() {
        val crouch = LocomotionPose.compute(
            crouching = true,
            sprinting = false,
            airborne = false,
            isStaff = true,
            resonance = 0.2f,
            activeWeight = 1f,
            isPulse = false,
            enabled = true,
        )
        assertTrue(crouch.bodyPitch > 0f && crouch.armPitch > 0f)

        val airborne = LocomotionPose.compute(
            crouching = false,
            sprinting = false,
            airborne = true,
            isStaff = true,
            resonance = 0.8f,
            activeWeight = 1f,
            isPulse = false,
            enabled = true,
        )
        assertTrue(airborne.legPitch > 0.1f && airborne.bodyPitch < 0f)

        val sprintThrow = LocomotionPose.compute(
            crouching = false,
            sprinting = true,
            airborne = false,
            isStaff = false,
            resonance = 0f,
            activeWeight = 0f,
            isPulse = true,
            enabled = true,
        )
        assertEquals(0f, sprintThrow.legScale)
        assertTrue(sprintThrow.bodyPitch > 0f)
    }
}
