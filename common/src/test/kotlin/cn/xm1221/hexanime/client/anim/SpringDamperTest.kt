package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.StaffMaterial
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpringDamperTest {
    @Test
    fun firstPersonEnvelopeMatchesThePlan() {
        assertEquals(0.60f, FirstPersonEnvelope.ROTATION)
        assertEquals(0.35f, FirstPersonEnvelope.TRANSLATION)
    }

    @Test
    fun quenchedPairMatchesTheReviewedPlan() {
        val quenched = StaffSprings.forMaterial(StaffMaterial.QUENCHED)
        assertEquals(320f, quenched.stiffness)
        assertEquals(0.35f, quenched.dampingRatio)
    }

    @Test
    fun underdampedStepOvershootsThenStaysBounded() {
        val woodPeak = peak(StaffSprings.forMaterial(StaffMaterial.WOOD))
        val quenchedPeak = peak(StaffSprings.forMaterial(StaffMaterial.QUENCHED))
        assertTrue(woodPeak > 1.02f, "wood should overshoot, peak=$woodPeak")
        assertTrue(quenchedPeak > woodPeak, "quenched should overshoot more than wood")
        assertTrue(quenchedPeak < 2.5f, "quenched ran away: $quenchedPeak")
    }

    @Test
    fun overdampedStepDoesNotOvershoot() {
        var position = 0f
        var velocity = 0f
        var peak = 0f
        repeat(80) {
            val next = SpringDamper.step(position, velocity, 1f, 180f, 1.2f, SpringDamper.TICK_DT)
            position = next.position
            velocity = next.velocity
            if (position > peak) peak = position
            assertTrue(position.isFinite() && velocity.isFinite())
            assertTrue(abs(velocity) <= SpringDamper.MAX_ABS_VELOCITY)
        }
        assertTrue(peak < 1.02f, "overdamped peak=$peak")
        assertTrue(abs(position - 1f) < 0.05f)
    }

    @Test
    fun timestepAboveTheCapMatchesTheCap() {
        val capped = SpringDamper.step(0f, 0f, 1f, 320f, 0.35f, SpringDamper.MAX_DT)
        val huge = SpringDamper.step(0f, 0f, 1f, 320f, 0.35f, 1f)
        assertEquals(capped, huge)
    }

    @Test
    fun velocityIsClamped() {
        val next = SpringDamper.step(
            position = 0f,
            velocity = 0f,
            target = 1f,
            stiffness = 320f,
            dampingRatio = 0.35f,
            dt = SpringDamper.TICK_DT,
            maxAbsVelocity = 0.2f,
        )
        assertTrue(abs(next.velocity) <= 0.2f + 1e-4f)
    }

    private fun peak(params: StaffSprings.Params): Float {
        var position = 0f
        var velocity = 0f
        var peak = 0f
        repeat(80) {
            val next = SpringDamper.step(
                position,
                velocity,
                1f,
                params.stiffness,
                params.dampingRatio,
                SpringDamper.TICK_DT,
            )
            position = next.position
            velocity = next.velocity
            if (position > peak) peak = position
            assertTrue(abs(velocity) <= SpringDamper.MAX_ABS_VELOCITY + 1e-4f)
        }
        return peak
    }
}
