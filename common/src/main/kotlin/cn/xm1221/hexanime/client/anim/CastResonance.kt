package cn.xm1221.hexanime.client.anim

import kotlin.math.PI
import kotlin.math.sin

enum class ResonanceTier {
    CALM,
    FOCUSED,
    SURGE,
}

/**
 * Pure escalation curve for chained pattern drawing and rapid artifact casts.
 * Drives posture intensity, offhand energy channeling, FOV focus, and magic circle expansion.
 */
object CastResonance {
    const val TIER_FOCUSED = 0.25f
    const val TIER_SURGE = 0.65f
    private const val DECAY_PER_TICK = 0.055f

    fun resonanceForCount(patternCount: Int, comboCount: Int = 0): Float {
        val effective = patternCount.coerceAtLeast(0) + comboCount.coerceAtLeast(0) * 2
        return when {
            effective <= 0 -> 0f
            effective <= 2 -> effective * 0.10f
            effective <= 6 -> TIER_FOCUSED + (effective - 2) / 4f * (TIER_SURGE - TIER_FOCUSED)
            else -> (TIER_SURGE + (effective - 6) / 6f * (1f - TIER_SURGE)).coerceIn(0f, 1f)
        }
    }

    fun tier(resonance: Float): ResonanceTier {
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        return when {
            r < TIER_FOCUSED -> ResonanceTier.CALM
            r < TIER_SURGE -> ResonanceTier.FOCUSED
            else -> ResonanceTier.SURGE
        }
    }

    fun pulseAmplitudeScale(resonance: Float, enabled: Boolean): Float {
        if (!enabled) return 1f
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        return 1f + r * 0.35f
    }

    fun levitationOffset(resonance: Float, phase: Float, enabled: Boolean): Float {
        if (!enabled) return 0f
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        if (r < TIER_FOCUSED || !phase.isFinite()) return 0f
        val t = ((r - TIER_FOCUSED) / (1f - TIER_FOCUSED)).coerceIn(0f, 1f)
        val breath = sin((phase * 0.16f).toDouble()).toFloat()
        return (0.045f + 0.025f * breath) * t
    }

    fun step(current: Float, target: Float, active: Boolean, dtTicks: Float = 1f): Float {
        val c = if (current.isFinite()) current.coerceIn(0f, 1f) else 0f
        val t = if (target.isFinite()) target.coerceIn(0f, 1f) else 0f
        val dt = if (dtTicks.isFinite()) dtTicks.coerceAtLeast(0f) else 0f
        if (active) {
            return (c + (t - c) * (0.28f * dt).coerceIn(0f, 1f)).coerceIn(0f, 1f)
        }
        return (c - DECAY_PER_TICK * dt).coerceAtLeast(0f)
    }
}
