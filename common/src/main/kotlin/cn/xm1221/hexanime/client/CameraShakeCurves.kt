package cn.xm1221.hexanime.client

/**
 * Fast-attack camera envelope. Cubic so the hit spends most of its time settling,
 * instead of a long sine wobble.
 */
object CameraShakeCurves {
    fun attackFade(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val remaining = 1f - clamped
        return remaining * remaining * remaining
    }
}
