package cn.xm1221.hexanime.client.anim

/**
 * Smooth first-person pose compensation during elytra flight so staves and floating
 * packaged items stay forward of the camera near-plane without clipping.
 */
object ElytraFirstPersonPose {
    private const val ENTER_TICKS = 6f
    private const val EXIT_TICKS = 8f
    const val MAX_FORWARD_OFFSET = 0.24f
    const val MAX_VERTICAL_OFFSET = 0.14f
    const val MAX_PITCH_OFFSET = 0.24f
    const val MAX_ROLL_OFFSET = 0.10f

    data class Result(
        val weight: Float,
        val pitchOffset: Float,
        val forwardOffset: Float,
        val verticalOffset: Float,
        val rollOffset: Float,
    ) {
        companion object {
            val ZERO = Result(0f, 0f, 0f, 0f, 0f)
        }
    }

    fun stepWeight(current: Float, fallFlying: Boolean, dtTicks: Float = 1f): Float {
        val c = if (current.isFinite()) current.coerceIn(0f, 1f) else 0f
        val dt = if (dtTicks.isFinite()) dtTicks.coerceAtLeast(0f) else 0f
        return if (fallFlying) {
            (c + dt / ENTER_TICKS).coerceIn(0f, 1f)
        } else {
            (c - dt / EXIT_TICKS).coerceIn(0f, 1f)
        }
    }

    fun compute(
        weight: Float,
        cameraPitchDeg: Float,
        speed: Float,
        firstPerson: Boolean,
    ): Result {
        if (!firstPerson || !weight.isFinite() || weight <= 0.001f) return Result.ZERO
        val w = weight.coerceIn(0f, 1f)
        val safePitch = if (cameraPitchDeg.isFinite()) cameraPitchDeg.coerceIn(-75f, 75f) else 0f
        val safeSpeed = if (speed.isFinite()) speed.coerceAtLeast(0f) else 0f
        val speedFactor = ((safeSpeed - 0.25f) / 1.5f).coerceIn(0f, 1f)

        val pitch = (-safePitch * 0.0030f * w).coerceIn(-MAX_PITCH_OFFSET, MAX_PITCH_OFFSET)
        val forward = (-(0.14f + 0.07f * speedFactor) * w).coerceIn(-MAX_FORWARD_OFFSET, 0f)
        val vertical = (-(0.05f + 0.04f * speedFactor) * w).coerceIn(-MAX_VERTICAL_OFFSET, 0f)
        val roll = (0.055f * speedFactor * w).coerceIn(0f, MAX_ROLL_OFFSET)
        return Result(
            weight = w,
            pitchOffset = pitch,
            forwardOffset = forward,
            verticalOffset = vertical,
            rollOffset = roll,
        )
    }
}
