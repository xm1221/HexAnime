package cn.xm1221.hexanime.client.anim

import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/** Display-only staff flavor. Does not change the cursor that is sent to the server. */
object StaffDisplay {
    data class Shake(val yaw: Float, val pitch: Float, val roll: Float)

    fun quantize(value: Float, step: Float): Float {
        if (!value.isFinite() || step <= 0.0001f) return if (value.isFinite()) value else 0f
        return round(value / step) * step
    }

    fun drift(phase: Float, amplitude: Float): Pair<Float, Float> {
        if (amplitude == 0f) return 0f to 0f
        val x = sin(phase * 0.23f) * amplitude
        val y = cos(phase * 0.17f) * amplitude * 0.65f
        return x to y
    }

    /** Incommensurate wobble once strain is clearly into overcast, above 0.65. */
    fun chaos(strain: Float, phase: Float): Shake {
        val amp = ((strain - CHAOS_START) / (1f - CHAOS_START)).coerceIn(0f, 1f)
        if (amp <= 0f) return Shake(0f, 0f, 0f)
        return Shake(
            yaw = sin(phase * 2.7f) * sin(phase * 0.37f) * 0.09f * amp,
            pitch = sin(phase * 3.1f + 0.6f) * sin(phase * 0.53f) * 0.07f * amp,
            roll = sin(phase * 2.2f + 1.4f) * sin(phase * 0.29f) * 0.11f * amp,
        )
    }

    const val CHAOS_START = 0.65f
    const val PARTICLE_STRAIN = 0.65f
}

object StaffDisplayTuning {
    const val DEFAULT_HEAD_PITCH = -0.28f

    fun headPitch(): Float = (clientFloat { it.mindspliceHeadPitch.toFloat() } ?: DEFAULT_HEAD_PITCH)
        .coerceIn(-0.6f, 0f)

    fun quenchedTremor(): Float = (clientFloat { it.quenchedTremor.toFloat() } ?: 0.016f)
        .coerceIn(0f, 0.08f)

    fun quenchedStep(): Float = (clientFloat { it.quenchedStep.toFloat() } ?: 0.08f)
        .coerceIn(0.02f, 0.4f)

    fun mindspliceDrift(): Float = (clientFloat { it.mindspliceDrift.toFloat() } ?: 0.03f)
        .coerceIn(0f, 0.12f)

    private fun clientFloat(read: (cn.xm1221.hexanime.config.HexAnimeClientConfig.ClientConfig) -> Float): Float? {
        if (!cn.xm1221.hexanime.config.HexAnimeClientConfig::holder.isInitialized) return null
        return read(cn.xm1221.hexanime.config.HexAnimeClientConfig.config)
    }
}

