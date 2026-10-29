package cn.xm1221.hexanime.casting.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariantMotionsTest {
    @Test
    fun allEightVariantsHaveDistinctPrimaryMotion() {
        val armSignatures = (0 until 8).map { variant ->
            val motion = VariantMotions.byVariant(variant)
            listOf(
                motion.armYaw,
                motion.armPitch,
                motion.armRoll,
                motion.itemYaw,
                motion.itemPitch,
                motion.itemRoll,
            )
        }
        assertEquals(8, armSignatures.toSet().size)
    }

    @Test
    fun variantIndexWrapsInsteadOfCrashing() {
        assertEquals(VariantMotions.byVariant(0), VariantMotions.byVariant(8))
        assertEquals(VariantMotions.byVariant(1), VariantMotions.byVariant(-7))
    }

    @Test
    fun signaturesUseVisibleAmplitude() {
        (0 until 8).forEach { variant ->
            val motion = VariantMotions.byVariant(variant)
            val peak = maxOf(
                kotlin.math.abs(motion.armYaw),
                kotlin.math.abs(motion.armPitch),
                kotlin.math.abs(motion.armRoll),
                kotlin.math.abs(motion.itemYaw),
                kotlin.math.abs(motion.itemPitch),
                kotlin.math.abs(motion.itemRoll),
            )
            assertTrue(peak >= 0.5f, "Variant $variant is too subtle: $peak")
        }
    }
}
