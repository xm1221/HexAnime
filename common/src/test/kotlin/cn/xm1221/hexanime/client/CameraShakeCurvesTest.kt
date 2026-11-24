package cn.xm1221.hexanime.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraShakeCurvesTest {
    @Test
    fun cubicAttackStartsFullAndEndsAtRest() {
        assertEquals(1f, CameraShakeCurves.attackFade(0f))
        assertEquals(0.125f, CameraShakeCurves.attackFade(0.5f))
        assertEquals(0f, CameraShakeCurves.attackFade(1f))
    }

    @Test
    fun cubicAttackIsMonotoneAndClamped() {
        val samples = (0..10).map { CameraShakeCurves.attackFade(it / 10f) }
        samples.zipWithNext().forEach { (left, right) -> assertTrue(left >= right) }
        assertEquals(1f, CameraShakeCurves.attackFade(-1f))
        assertEquals(0f, CameraShakeCurves.attackFade(2f))
    }

    @Test
    fun rollAndFovKickStartAndEndAtZero() {
        assertEquals(0f, CameraShakeCurves.rollWave(0f), 1.0e-5f)
        assertEquals(0f, CameraShakeCurves.rollWave(1f), 1.0e-5f)
        assertTrue(CameraShakeCurves.rollWave(0.2f) > 0.3f)

        assertEquals(0f, CameraShakeCurves.fovKickEnvelope(0f), 1.0e-5f)
        assertEquals(0f, CameraShakeCurves.fovKickEnvelope(1f), 1.0e-5f)
        assertTrue(CameraShakeCurves.fovKickEnvelope(0.15f) > 0.5f)
    }
}
