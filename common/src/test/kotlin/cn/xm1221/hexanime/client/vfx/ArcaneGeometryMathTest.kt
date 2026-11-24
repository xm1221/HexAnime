package cn.xm1221.hexanime.client.vfx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArcaneGeometryMathTest {
    @Test
    fun firstPersonCircleIsSubtleAndShatterExpands() {
        val thirdAlpha = ArcaneGeometryMath.circleAlpha(1f, 0.8f, isLocalFirstPerson = false)
        val firstAlpha = ArcaneGeometryMath.circleAlpha(1f, 0.8f, isLocalFirstPerson = true)
        assertTrue(firstAlpha > 0f && firstAlpha < thirdAlpha * 0.5f)

        val normalRadius = ArcaneGeometryMath.circleRadius(1f, 0.5f, patternCount = 2)
        val shatterEnd = ArcaneGeometryMath.circleRadius(1f, 0.5f, patternCount = 2, shatterProgress = 1f)
        assertTrue(shatterEnd > normalRadius)
    }

    @Test
    fun circleExpandsPastTenBlocksWithMultiLayerProgression() {
        val r0 = ArcaneGeometryMath.circleRadius(1f, 0f, patternCount = 0)
        val r4 = ArcaneGeometryMath.circleRadius(1f, 0.3f, patternCount = 4)
        val r12 = ArcaneGeometryMath.circleRadius(1f, 0.8f, patternCount = 12)
        val r25 = ArcaneGeometryMath.circleRadius(1f, 1.0f, patternCount = 25)
        val compact = ArcaneGeometryMath.circleRadius(1f, 1.0f, patternCount = 25, expandingEnabled = false)

        assertTrue(r4 > r0)
        assertTrue(r12 > r4)
        assertTrue(r25 > 10.0f && r25 <= ArcaneGeometryMath.MAX_CIRCLE_RADIUS)
        assertTrue(compact < 1.5f)
        assertEquals(1, ArcaneGeometryMath.layerCount(r0))
        assertEquals(4, ArcaneGeometryMath.layerCount(r25))
    }

    @Test
    fun dtApproachExpandsGraduallyAndLingersOnRelease() {
        val afterQuarterSecExpand = ArcaneGeometryMath.approachDt(
            1.0f,
            10.0f,
            ArcaneGeometryMath.EXPANSION_RATE_PER_SEC,
            0.25f,
        )
        assertTrue(afterQuarterSecExpand in 2.5f..5.5f)

        val alphaAfterHalfSecFade = ArcaneGeometryMath.approachDt(
            0.8f,
            0f,
            ArcaneGeometryMath.ALPHA_RELEASE_PER_SEC,
            0.5f,
        )
        assertTrue(alphaAfterHalfSecFade > 0.35f)
        assertTrue(ArcaneGeometryMath.ringHalfWidth(10f) > ArcaneGeometryMath.ringHalfWidth(1f))
        assertTrue(ArcaneGeometryMath.glyphHalfWidth(1f) > ArcaneGeometryMath.glyphHalfWidth(0.2f))
    }

    @Test
    fun trailTapersAndFadesAlongTail() {
        assertTrue(
            ArcaneGeometryMath.trailWidth(0f, 0.5f) > ArcaneGeometryMath.trailWidth(0.8f, 0.5f),
        )
        assertTrue(
            ArcaneGeometryMath.trailAlpha(0f, 0f, false) > ArcaneGeometryMath.trailAlpha(0.7f, 0.5f, false),
        )
    }
}
