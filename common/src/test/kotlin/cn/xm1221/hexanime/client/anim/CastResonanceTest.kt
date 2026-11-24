package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CastResonanceTest {
    @Test
    fun resonanceEscalatesAcrossThreeTiers() {
        val zero = CastResonance.resonanceForCount(0, 0)
        val tier1 = CastResonance.resonanceForCount(2, 0)
        val tier2 = CastResonance.resonanceForCount(4, 0)
        val tier3 = CastResonance.resonanceForCount(8, 1)

        assertEquals(0f, zero)
        assertEquals(ResonanceTier.CALM, CastResonance.tier(zero))
        assertEquals(ResonanceTier.CALM, CastResonance.tier(tier1))
        assertEquals(ResonanceTier.FOCUSED, CastResonance.tier(tier2))
        assertEquals(ResonanceTier.SURGE, CastResonance.tier(tier3))
        assertTrue(tier3 <= 1f)
    }

    @Test
    fun levitationAndPulseScaleRespectBoundsAndToggle() {
        assertEquals(1f, CastResonance.pulseAmplitudeScale(0.9f, enabled = false))
        assertTrue(CastResonance.pulseAmplitudeScale(0.8f, enabled = true) > 1.2f)
        assertEquals(0f, CastResonance.levitationOffset(0.1f, 10f, enabled = true))
        assertEquals(0f, CastResonance.levitationOffset(0.9f, 10f, enabled = false))
        assertTrue(CastResonance.levitationOffset(0.8f, 10f, enabled = true) > 0f)
    }

    @Test
    fun stepDecaysCleanlyAndRejectsNaN() {
        val decayed = CastResonance.step(0.5f, 0f, active = false, dtTicks = 4f)
        assertTrue(decayed < 0.5f && decayed >= 0f)
        assertEquals(0f, CastResonance.step(Float.NaN, Float.POSITIVE_INFINITY, active = false))
    }
}
