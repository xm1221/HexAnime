package cn.xm1221.hexanime.casting.item

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaffCastMotionsTest {
    @Test
    fun fourDirectionsAreDistinct() {
        val signatures = (0 until 4).map { variant ->
            val motion = StaffCastMotions.byVariant(variant)
            listOf(motion.armYaw, motion.armPitch, motion.armRoll, motion.itemPitch, motion.itemYaw)
        }
        assertEquals(4, signatures.toSet().size)
    }

    @Test
    fun indexWrapsInsideTheStaffTable() {
        assertEquals(StaffCastMotions.byVariant(0), StaffCastMotions.byVariant(4))
        assertEquals(StaffCastMotions.byVariant(1), StaffCastMotions.byVariant(-3))
    }

    @Test
    fun cursorQuadrantsPickTheNamedFlourish() {
        assertEquals(0, StaffCastMotions.variantForCursor(0f, 0f))
        assertEquals(0, StaffCastMotions.variantForCursor(0.1f, -0.1f))
        assertEquals(3, StaffCastMotions.variantForCursor(0.8f, 0.1f))
        assertEquals(3, StaffCastMotions.variantForCursor(-0.9f, 0.2f))
        assertEquals(1, StaffCastMotions.variantForCursor(0.1f, 0.8f))
        assertEquals(2, StaffCastMotions.variantForCursor(0.1f, -0.8f))
        assertEquals(0, StaffCastMotions.variantForCursor(Float.NaN, Float.POSITIVE_INFINITY))
    }

    @Test
    fun eachFlourishHasAReadableArm() {
        (0 until 4).forEach { variant ->
            val motion = StaffCastMotions.byVariant(variant)
            val peak = maxOf(
                abs(motion.armYaw),
                abs(motion.armPitch),
                abs(motion.armRoll),
                abs(motion.itemYaw),
                abs(motion.itemPitch),
            )
            assertTrue(peak >= 0.2f, "Staff flourish $variant is flat")
        }
    }
}
