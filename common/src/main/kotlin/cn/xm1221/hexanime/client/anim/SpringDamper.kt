package cn.xm1221.hexanime.client.anim

import kotlin.math.sqrt

/**
 * Display-only second-order spring. Callers must pass a timestep that has already been
 * clamped; [step] also refuses anything above [MAX_DT] so a stall cannot explode the state.
 */
object SpringDamper {
    const val MAX_DT = 0.05f
    const val TICK_DT = 0.05f
    const val MAX_ABS_VELOCITY = 14f

    data class State(val position: Float, val velocity: Float)

    fun step(
        position: Float,
        velocity: Float,
        target: Float,
        stiffness: Float,
        dampingRatio: Float,
        dt: Float,
        maxAbsVelocity: Float = MAX_ABS_VELOCITY,
    ): State {
        val stepDt = if (dt.isFinite()) dt.coerceIn(0f, MAX_DT) else 0f
        if (stepDt == 0f || !position.isFinite() || !velocity.isFinite() || !target.isFinite()) {
            return State(if (position.isFinite()) position else 0f, 0f)
        }
        val k = stiffness.coerceAtLeast(0f)
        val omega = sqrt(k)
        val damping = 2f * dampingRatio.coerceAtLeast(0f) * omega
        val accel = -k * (position - target) - damping * velocity
        val limit = maxAbsVelocity.coerceAtLeast(0f)
        val nextVelocity = (velocity + accel * stepDt).let { raw ->
            if (raw.isFinite()) raw.coerceIn(-limit, limit) else 0f
        }
        val nextPosition = position + nextVelocity * stepDt
        return State(
            position = if (nextPosition.isFinite()) nextPosition else target,
            velocity = nextVelocity,
        )
    }
}
