package cn.xm1221.hexanime.casting

import kotlin.math.exp

/**
 * Maps a real overcast damage amount into a temporary staff strain.
 * Remaining health is not an input; a low-health player with no overcast stays near zero.
 */
object OvercastStrain {
    const val HOLD_TICKS = 20f
    const val TAU_TICKS = 16f

    fun initialForDamage(amount: Float): Float {
        if (amount <= 0f || !amount.isFinite()) return 0f
        return (0.68f + amount / 12f).coerceIn(0f, 1f)
    }

    fun remaining(initial: Float, elapsedTicks: Float): Float {
        if (initial <= 0f || !initial.isFinite() || elapsedTicks < 0f) return 0f
        val held = initial.coerceIn(0f, 1f)
        if (elapsedTicks <= HOLD_TICKS) return held
        val decayed = held * exp(-(elapsedTicks - HOLD_TICKS) / TAU_TICKS)
        return decayed.toFloat().coerceIn(0f, 1f)
    }
}
