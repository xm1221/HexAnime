package cn.xm1221.hexanime.client.anim

/**
 * Adapts casting body/arm/leg posture for crouching, airborne levitation, and sprinting throws.
 */
object LocomotionPose {
    data class Offsets(
        val bodyPitch: Float,
        val bodyY: Float,
        val armPitch: Float,
        val armRoll: Float,
        val legPitch: Float,
        val legScale: Float,
    ) {
        companion object {
            val ZERO = Offsets(0f, 0f, 0f, 0f, 0f, 1f)
        }
    }

    fun compute(
        crouching: Boolean,
        sprinting: Boolean,
        airborne: Boolean,
        isStaff: Boolean,
        resonance: Float,
        activeWeight: Float,
        isPulse: Boolean,
        enabled: Boolean,
    ): Offsets {
        if (!enabled) return Offsets.ZERO
        val w = if (activeWeight.isFinite()) activeWeight.coerceIn(0f, 1f) else 0f
        if (w <= 0.01f && !isPulse) return Offsets.ZERO
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        val effectiveW = maxOf(w, if (isPulse) 1f else 0f).coerceIn(0f, 1f)

        var bodyPitch = 0f
        var bodyY = 0f
        var armPitch = 0f
        var armRoll = 0f
        var legPitch = 0f
        var legScale = 1f

        if (crouching) {
            bodyPitch += 0.11f * effectiveW
            bodyY += 0.04f * effectiveW
            armPitch += 0.16f * effectiveW
            armRoll -= 0.08f * effectiveW
        }

        if (airborne && (isPulse || r >= CastResonance.TIER_FOCUSED)) {
            val airWeight = if (isPulse) effectiveW else effectiveW * r
            bodyPitch -= 0.08f * airWeight
            legPitch += 0.24f * airWeight
        }

        if (sprinting && !isStaff && isPulse) {
            bodyPitch += 0.12f * effectiveW
            armPitch -= 0.10f * effectiveW
            legScale = 0f
        }

        return Offsets(
            bodyPitch = bodyPitch,
            bodyY = bodyY,
            armPitch = armPitch,
            armRoll = armRoll,
            legPitch = legPitch,
            legScale = legScale,
        )
    }
}
