package cn.xm1221.hexanime.client.anim

/** Short damage acknowledgement; persistent low-media state alone must not keep the layer alive. */
object DamageFeedback {
    const val DURATION_TICKS = 12f
    private const val HOLD_TICKS = 2f

    fun envelope(elapsedTicks: Float): Float {
        if (elapsedTicks < 0f || elapsedTicks >= DURATION_TICKS) return 0f
        if (elapsedTicks <= HOLD_TICKS) return 1f
        val remaining = 1f - (elapsedTicks - HOLD_TICKS) / (DURATION_TICKS - HOLD_TICKS)
        return remaining * remaining
    }
}
