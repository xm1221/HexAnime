package cn.xm1221.hexanime.client.vfx

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import kotlin.test.Test
import kotlin.test.assertTrue

class HexPatternGeometryTest {
    @Test
    fun realHexPatternNormalizesInsideUnitSquare() {
        val pattern = HexPattern.fromAngles("qaq", HexDir.EAST)
        val norm = HexPatternGeometry.fromHexPattern(pattern)
        assertTrue(norm.size >= 3)
        norm.forEach { p ->
            assertTrue(p.x in -1.01f..1.01f && p.y in -1.01f..1.01f)
        }
    }

    @Test
    fun fallbackGlyphProducesCleanBoundedPolyline() {
        val glyph = HexPatternGeometry.fallbackGlyph(42)
        assertTrue(glyph.size >= 5)
        glyph.forEach { p ->
            assertTrue(p.x.isFinite() && p.y.isFinite())
            assertTrue(p.x in -1.01f..1.01f && p.y in -1.01f..1.01f)
        }
    }
}
