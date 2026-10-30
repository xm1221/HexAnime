package cn.xm1221.hexanime.client.anim

/** Ordinary cast flourishes only include leg work occasionally. */
object LegMotionCadence {
    const val CASTS_PER_LEG_MOTION = 4

    fun shouldAnimate(castOrdinal: Int): Boolean =
        castOrdinal > 0 && castOrdinal % CASTS_PER_LEG_MOTION == 0
}
