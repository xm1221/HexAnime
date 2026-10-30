package cn.xm1221.hexanime.client.anim

/** Keeps a third-party staff inside a smaller pose so it does not intersect the body. */
object TaggedEnvelope {
    const val ROT_SCALE = 0.58f
    const val POS_SCALE = 0.40f
    const val ROT_MAX = 0.48f
    const val POS_MAX = 0.12f

    fun scaleRotation(value: Float): Float = (value * ROT_SCALE).coerceIn(-ROT_MAX, ROT_MAX)

    fun scalePosition(value: Float): Float = (value * POS_SCALE).coerceIn(-POS_MAX, POS_MAX)
}
