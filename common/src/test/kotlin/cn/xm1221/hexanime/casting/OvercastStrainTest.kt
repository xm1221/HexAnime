package cn.xm1221.hexanime.casting

import cn.xm1221.hexanime.client.anim.StaffDisplay
import cn.xm1221.hexanime.client.anim.StaffDisplayTuning
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OvercastStrainTest {
    @Test
    fun zeroDamageDoesNotRaiseStrain() {
        assertEquals(0f, OvercastStrain.initialForDamage(0f))
        assertEquals(0f, OvercastStrain.initialForDamage(-2f))
        assertEquals(0f, OvercastStrain.initialForDamage(Float.NaN))
    }

    @Test
    fun anyRealHitClearsTheParticleThresholdAndCapsAtOne() {
        val small = OvercastStrain.initialForDamage(0.5f)
        assertTrue(small > StaffDisplay.PARTICLE_STRAIN)
        assertEquals(1f, OvercastStrain.initialForDamage(100f))
    }

    @Test
    fun strainHoldsThenDecays() {
        val initial = 0.8f
        assertEquals(initial, OvercastStrain.remaining(initial, 0f))
        assertEquals(initial, OvercastStrain.remaining(initial, OvercastStrain.HOLD_TICKS))
        val later = OvercastStrain.remaining(initial, OvercastStrain.HOLD_TICKS + 16f)
        assertTrue(later < initial)
        assertTrue(later > 0f)
        assertTrue(
            OvercastStrain.remaining(initial, 80f) < OvercastStrain.remaining(initial, 40f),
        )
    }
}

class StaffDisplayTest {
    @Test
    fun quantizeSnapsToTheStep() {
        assertEquals(0.08f, StaffDisplay.quantize(0.1f, 0.08f), 1.0e-5f)
        assertEquals(0f, StaffDisplay.quantize(0.03f, 0.08f), 1.0e-5f)
        assertEquals(0.5f, StaffDisplay.quantize(0.5f, 0f), 1.0e-5f)
    }

    @Test
    fun chaosStaysQuietBelowTheThreshold() {
        val quiet = StaffDisplay.chaos(0.65f, 3f)
        assertEquals(0f, quiet.yaw)
        val loud = StaffDisplay.chaos(1f, 1.2f)
        val energy = abs(loud.yaw) + abs(loud.pitch) + abs(loud.roll)
        assertTrue(energy > 0f)
    }

    @Test
    fun mindspliceNodDefaultsBelowTheOldHalfRadian() {
        assertEquals(-0.28f, StaffDisplayTuning.DEFAULT_HEAD_PITCH)
    }
}
