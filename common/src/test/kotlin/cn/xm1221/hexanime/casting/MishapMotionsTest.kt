package cn.xm1221.hexanime.casting

import kotlin.test.Test
import kotlin.test.assertEquals

class MishapMotionsTest {
    @Test
    fun mishapClassesMapToStableMotionFamilies() {
        assertEquals(MishapFlavor.MEDIA, MishapMotions.flavorFor("MishapNotEnoughMedia"))
        assertEquals(MishapFlavor.SPATIAL, MishapMotions.flavorFor("MishapBadBlock"))
        assertEquals(MishapFlavor.MIND, MishapMotions.flavorFor("MishapBadBrainsweep"))
        assertEquals(MishapFlavor.RECOIL, MishapMotions.flavorFor("MishapInvalidIota"))
        assertEquals(MishapFlavor.SPATIAL, MishapFlavor.fromCommandName("spatial"))
    }

    @Test
    fun unknownVariantFallsBackToTheOriginalRecoil() {
        assertEquals(MishapFlavor.RECOIL, MishapFlavor.fromVariant(-1))
        assertEquals(MishapFlavor.RECOIL, MishapFlavor.fromVariant(99))
    }
}
