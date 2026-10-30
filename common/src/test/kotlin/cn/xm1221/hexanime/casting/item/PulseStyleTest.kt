package cn.xm1221.hexanime.casting.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PulseStyleTest {
    @Test
    fun familiesAreDistinguishable() {
        assertTrue(PulseStyle.CYPHER.allowBreak)
        assertFalse(PulseStyle.TRINKET.allowBreak)
        assertFalse(PulseStyle.ARTIFACT.allowBreak)
        assertTrue(PulseStyle.ARTIFACT.reentry)
        assertFalse(PulseStyle.TRINKET.reentry)
        assertTrue(PulseStyle.CYPHER.durationScale < PulseStyle.TRINKET.durationScale)
        assertTrue(PulseStyle.TRINKET.castScale < PulseStyle.ARTIFACT.castScale)
        assertTrue(PulseStyle.CYPHER.breakScale > PulseStyle.DEFAULT.breakScale)
    }

    @Test
    fun artifactReentryRewindsToAboutAFifth() {
        val start = PulseStyle.ARTIFACT.reentryStartTick(ageTicks = 40f, durationTicks = 20f)
        assertEquals(40f - 20f * 0.22f, start, 1.0e-4f)
        val progress = (40f - start) / 20f
        assertEquals(0.22f, progress, 1.0e-4f)
    }
}
