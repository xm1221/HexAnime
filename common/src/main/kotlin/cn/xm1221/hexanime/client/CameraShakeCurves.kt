package cn.xm1221.hexanime.client

import kotlin.math.PI
import kotlin.math.sin

/**
 * Fast-attack camera envelopes. Cubic decay so hits settle cleanly
 * instead of lingering as a long wobble.
 */
object CameraShakeCurves {
    fun attackFade(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val remaining = 1f - clamped
        return remaining * remaining * remaining
    }

    fun rollWave(progress: Float, cycles: Float = 1.5f): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val remaining = 1f - clamped
        return sin(clamped * PI.toFloat() * cycles) * remaining * remaining
    }

    fun fovKickEnvelope(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val rise = (clamped / 0.14f).coerceAtMost(1f)
        val fall = 1f - clamped
        return rise * fall * fall
    }
}
