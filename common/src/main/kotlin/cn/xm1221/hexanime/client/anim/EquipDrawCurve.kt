package cn.xm1221.hexanime.client.anim

import kotlin.math.PI
import kotlin.math.sin

/**
 * Crisp equip draw flourish when switching into a staff (twirl & settle)
 * or packaged Hex item (palm toss & catch).
 */
object EquipDrawCurve {
    const val DURATION_TICKS = 11f

    data class Pose(
        val armPitch: Float,
        val armRoll: Float,
        val itemPitch: Float,
        val itemRoll: Float,
        val itemY: Float,
        val firstPersonPitch: Float,
        val firstPersonRoll: Float,
        val firstPersonY: Float,
    ) {
        companion object {
            val ZERO = Pose(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    fun evaluate(elapsedTicks: Float, isStaff: Boolean, inward: Float, enabled: Boolean): Pose {
        if (!enabled || !elapsedTicks.isFinite() || elapsedTicks < 0f || elapsedTicks > DURATION_TICKS) {
            return Pose.ZERO
        }
        val t = (elapsedTicks / DURATION_TICKS).coerceIn(0f, 1f)
        // Peak later (~0.45..0.65) so it remains clearly visible as vanilla equip raises into view.
        val envelope = sin(t * PI).toFloat()
        val lateArc = sin((t * t) * PI).toFloat()
        val settle = sin(t * PI * 2f).toFloat() * (1f - t) * 0.25f
        val side = inward.coerceIn(-1f, 1f)
        return if (isStaff) {
            Pose(
                armPitch = -0.88f * envelope + settle * 0.25f,
                armRoll = side * 0.24f * envelope,
                itemPitch = -0.58f * lateArc + settle,
                itemRoll = -side * 2.15f * lateArc,
                itemY = 0.08f * envelope,
                firstPersonPitch = -0.44f * lateArc + settle * 0.5f,
                firstPersonRoll = -side * 1.45f * lateArc,
                firstPersonY = 0.11f * envelope,
            )
        } else {
            Pose(
                armPitch = -0.78f * envelope,
                armRoll = side * 0.16f * envelope,
                itemPitch = -0.95f * lateArc + settle,
                itemRoll = -side * 0.85f * lateArc,
                itemY = -0.24f * lateArc,
                firstPersonPitch = -0.68f * lateArc,
                firstPersonRoll = -side * 0.65f * lateArc,
                firstPersonY = 0.15f * lateArc,
            )
        }
    }
}
