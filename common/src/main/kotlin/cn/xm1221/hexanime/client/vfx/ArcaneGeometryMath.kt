package cn.xm1221.hexanime.client.vfx

import kotlin.math.exp

/**
 * Pure math helpers for lightweight 3D arcane circles, multi-layer 10+ block expansion,
 * time-based expansion/linger curves, staff-tip ribbon trails, and mishap shatter rings.
 */
object ArcaneGeometryMath {
    const val MAX_CIRCLE_RADIUS = 12.0f
    const val EXPANSION_RATE_PER_SEC = 1.85f
    const val RETRACTION_RATE_PER_SEC = 0.90f
    const val ALPHA_ATTACK_PER_SEC = 7.5f
    const val ALPHA_RELEASE_PER_SEC = 1.15f

    fun circleRadius(
        aimWeight: Float,
        resonance: Float,
        patternCount: Int = 0,
        expandingEnabled: Boolean = true,
        shatterProgress: Float = -1f,
    ): Float {
        val w = if (aimWeight.isFinite()) aimWeight.coerceIn(0f, 1f) else 0f
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        val patterns = if (expandingEnabled) patternCount.coerceAtLeast(0) else 0
        // Smooth asymptotic expansion: ~0.9 at 0 patterns -> ~4.8 at 5 -> ~8.5 at 12 -> 11.4+ at 25+.
        val patternGrowth = if (expandingEnabled) {
            (1f - exp((-patterns * 0.10f).toDouble()).toFloat()) * 10.6f
        } else {
            0f
        }
        val baseRadius = (0.85f + 0.45f * r + patternGrowth).coerceAtMost(MAX_CIRCLE_RADIUS)
        if (shatterProgress in 0f..1f) {
            return baseRadius + shatterProgress * (0.95f + baseRadius * 0.18f)
        }
        return baseRadius * (0.55f + 0.45f * w)
    }

    /** Frame-rate independent exponential smoothing for gradual unfurling and retraction. */
    fun approachDt(current: Float, target: Float, ratePerSecond: Float, dtSeconds: Float): Float {
        val c = if (current.isFinite()) current else 0f
        val t = if (target.isFinite()) target else 0f
        val dt = if (dtSeconds.isFinite()) dtSeconds.coerceIn(0f, 0.25f) else 0f
        val rate = if (ratePerSecond.isFinite()) ratePerSecond.coerceAtLeast(0f) else 0f
        val factor = (1f - exp((-rate * dt).toDouble()).toFloat()).coerceIn(0f, 1f)
        return c + (t - c) * factor
    }

    /** Returns how many concentric ring layers (1..4) are active for the current radius. */
    fun layerCount(radius: Float): Int = when {
        !radius.isFinite() || radius < 1.65f -> 1
        radius < 3.60f -> 2
        radius < 6.40f -> 3
        else -> 4
    }

    fun ringHalfWidth(radius: Float): Float {
        val r = if (radius.isFinite()) radius.coerceAtLeast(0f) else 0f
        return (0.024f + r * 0.0045f).coerceIn(0.024f, 0.068f)
    }

    fun glyphHalfWidth(glyphScale: Float): Float {
        val s = if (glyphScale.isFinite()) glyphScale.coerceAtLeast(0f) else 0f
        return (0.022f + s * 0.024f).coerceIn(0.022f, 0.052f)
    }

    fun circleAlpha(aimWeight: Float, resonance: Float, isLocalFirstPerson: Boolean): Float {
        val w = if (aimWeight.isFinite()) aimWeight.coerceIn(0f, 1f) else 0f
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        val base = (0.42f + 0.36f * r) * w
        return (if (isLocalFirstPerson) base * 0.34f else base).coerceIn(0f, 1f)
    }

    fun trailWidth(fractionFromHead: Float, resonance: Float): Float {
        val f = if (fractionFromHead.isFinite()) fractionFromHead.coerceIn(0f, 1f) else 1f
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        return (1f - f) * (0.034f + 0.024f * r)
    }

    fun trailAlpha(fractionFromHead: Float, ageFraction: Float, isLocalFirstPerson: Boolean): Float {
        val f = if (fractionFromHead.isFinite()) fractionFromHead.coerceIn(0f, 1f) else 1f
        val age = if (ageFraction.isFinite()) ageFraction.coerceIn(0f, 1f) else 1f
        val scale = if (isLocalFirstPerson) 0.44f else 0.80f
        return ((1f - f) * (1f - age) * scale).coerceIn(0f, 1f)
    }
}
