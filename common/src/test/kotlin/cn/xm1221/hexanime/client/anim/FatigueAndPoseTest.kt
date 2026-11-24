package cn.xm1221.hexanime.client.anim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FatigueAndPoseTest {
    @Test
    fun fatigueLastsTenTicksAndDecays() {
        assertEquals(1f, FatigueCurve.envelope(0f), 1.0e-5f)
        assertEquals(0f, FatigueCurve.envelope(10f))
        assertEquals(0f, FatigueCurve.envelope(-1f))
        val early = FatigueCurve.envelope(1f)
        val late = FatigueCurve.envelope(8f)
        assertTrue(early > late)
        assertTrue(late > 0f)
    }

    @Test
    fun damageFeedbackEndsInsteadOfPersistingWithLowMedia() {
        assertEquals(1f, DamageFeedback.envelope(0f))
        assertEquals(1f, DamageFeedback.envelope(2f))
        assertTrue(DamageFeedback.envelope(8f) > 0f)
        assertEquals(0f, DamageFeedback.envelope(DamageFeedback.DURATION_TICKS))
        assertEquals(0f, DamageFeedback.envelope(-1f))
    }

    @Test
    fun offhandKindsSupportMediaAndResonantEmptyHand() {
        assertEquals(OffhandRitual.Kind.EMPTY, OffhandRitual.classify(true, "minecraft", "air"))
        assertEquals(
            OffhandRitual.Kind.PATCHOULI,
            OffhandRitual.classify(false, "patchouli", "guide_book"),
        )
        assertEquals(
            OffhandRitual.Kind.LORE,
            OffhandRitual.classify(false, "hexcasting", "lore_fragment"),
        )
        assertEquals(
            OffhandRitual.Kind.HEX_MEDIA,
            OffhandRitual.classify(false, "hexcasting", "cypher"),
        )
        assertEquals(
            OffhandRitual.Kind.HEX_MEDIA,
            OffhandRitual.classify(false, "hexcasting", "focus"),
        )
        assertEquals(
            OffhandRitual.Kind.OTHER,
            OffhandRitual.classify(false, "minecraft", "stick"),
        )
        assertNull(OffhandRitual.pose(OffhandRitual.Kind.EMPTY))
        assertNotNull(OffhandRitual.pose(OffhandRitual.Kind.EMPTY, resonance = 0.5f, offhandEnergyEnabled = true))
        assertNull(OffhandRitual.pose(OffhandRitual.Kind.EMPTY, resonance = 0.5f, offhandEnergyEnabled = false))
        assertNotNull(OffhandRitual.pose(OffhandRitual.Kind.HEX_MEDIA))
        assertNotNull(OffhandRitual.pose(OffhandRitual.Kind.DUAL_CAST_ITEM))
        assertNotNull(OffhandRitual.pose(OffhandRitual.Kind.LORE))
        assertNull(OffhandRitual.pose(OffhandRitual.Kind.OTHER))
    }

    @Test
    fun taggedEnvelopeShrinksAndCaps() {
        assertEquals(0.29f, TaggedEnvelope.scaleRotation(0.5f), 1.0e-4f)
        assertEquals(TaggedEnvelope.ROT_MAX, TaggedEnvelope.scaleRotation(4f))
        assertEquals(-TaggedEnvelope.POS_MAX, TaggedEnvelope.scalePosition(-2f))
        assertTrue(TaggedEnvelope.scaleRotation(0.2f) < 0.2f)
    }
}
