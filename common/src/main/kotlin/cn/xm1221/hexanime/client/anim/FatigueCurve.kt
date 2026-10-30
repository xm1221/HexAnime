package cn.xm1221.hexanime.client.anim

import kotlin.math.exp

/** Short exponential slump after the mishap pulse, about ten ticks. */
object FatigueCurve {
    const val DURATION_TICKS = 10f
    const val ARM_DROOP = 0.42f
    const val ITEM_DROOP = 0.22f
    const val BODY_DROOP = 0.16f
    const val HEAD_DROOP = 0.12f

    fun envelope(elapsedTicks: Float, durationTicks: Float = DURATION_TICKS): Float {
        if (!elapsedTicks.isFinite() || elapsedTicks < 0f || elapsedTicks >= durationTicks) return 0f
        return exp(-elapsedTicks / (durationTicks / 3f)).toFloat()
    }
}
