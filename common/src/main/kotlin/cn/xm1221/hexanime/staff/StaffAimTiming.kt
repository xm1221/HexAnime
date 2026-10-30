package cn.xm1221.hexanime.staff

/** Shared server refresh bounds and client timeout for continuous staff aim. */
object StaffAimTiming {
    const val MIN_REFRESH_TICKS = 5
    const val MAX_REFRESH_TICKS = 20
    const val REMOTE_TIMEOUT_TICKS = 30f

    fun refreshTicks(configured: Int): Int =
        configured.coerceIn(MIN_REFRESH_TICKS, MAX_REFRESH_TICKS)
}
