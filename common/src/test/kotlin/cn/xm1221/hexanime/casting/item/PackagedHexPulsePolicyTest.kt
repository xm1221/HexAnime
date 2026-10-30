package cn.xm1221.hexanime.casting.item

import kotlin.test.Test
import kotlin.test.assertEquals

class PackagedHexPulsePolicyTest {
    @Test
    fun depletedBreakableItemIsBreak() {
        assertEquals(CastPulseKind.BREAK, PackagedHexPulsePolicy.kindForBroken(true))
    }

    @Test
    fun mediaRemainingIsCast() {
        assertEquals(CastPulseKind.CAST, PackagedHexPulsePolicy.kindForBroken(false))
    }
}
