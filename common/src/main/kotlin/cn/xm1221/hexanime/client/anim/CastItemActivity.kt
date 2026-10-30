package cn.xm1221.hexanime.client.anim

/** Pure activity gate for the playerAnimator layer. */
internal object CastItemActivity {
    fun shouldRun(
        enabled: Boolean,
        hasAim: Boolean,
        hasPulse: Boolean,
        hasMicroPulse: Boolean,
        hasFatigue: Boolean,
        overcastStrain: Float,
    ): Boolean {
        if (!enabled) return false
        return hasAim ||
            hasPulse ||
            hasMicroPulse ||
            hasFatigue ||
            overcastStrain > 0f
    }
}
