package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.ItemCastMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ItemFloatPose(
    val weight: Float,
    val armYaw: Float,
    val armPitch: Float,
    val armRoll: Float,
    val itemYaw: Float,
    val itemPitch: Float,
    val itemRoll: Float,
    val itemX: Float,
    val itemY: Float,
    val itemZ: Float,
) {
    /** First-person camera-local offset. X right, Y up, Z back; playerAnimator expects pixels. */
    fun firstPersonOffset(): Triple<Float, Float, Float> =
        Triple(itemX * 16f, itemY * 16f, itemZ * 16f)

    fun firstPersonTarget(inward: Float): ItemTargetPose {
        val baseX = if (inward < 0f) 0.56f else -0.56f
        return ItemTargetPose(baseX + itemX, -0.52f + itemY, -0.72f + itemZ)
    }
}

data class ItemTargetPose(val x: Float, val y: Float, val z: Float)

/** Detached item choreography, matched to the cad/0..7 item silhouettes. */
object ItemFloatMotions {
    private const val START = 0.12f
    private const val END = 0.84f
    private const val EDGE = 0.16f
    private const val FULL_TURN = 2f * PI.toFloat()

    fun pose(
        motion: ItemCastMotion,
        tickProgress: Float,
        inward: Float,
        orbitalEnabled: Boolean = true,
    ): ItemFloatPose? {
        if (tickProgress <= START || tickProgress >= END) return null
        val progress = ((tickProgress - START) / (END - START)).coerceIn(0f, 1f)
        val weight = when {
            progress < EDGE -> smoothStep(progress / EDGE)
            progress > 1f - EDGE -> smoothStep((1f - progress) / EDGE)
            else -> 1f
        }
        val spin = smoothStep(hitstopProgress(progress, orbitalEnabled)) * FULL_TURN
        val wave = sin(progress * PI * 2f).toFloat()
        val side = inward.coerceIn(-1f, 1f)
        val orbitAngle = (progress - 0.5f) * PI.toFloat() * 1.35f
        val orbitX = if (orbitalEnabled) 0.38f + 0.18f * cos(orbitAngle) else 0.56f
        val orbitZ = if (orbitalEnabled) 0.14f * sin(orbitAngle) else 0f
        val orbitY = if (orbitalEnabled) 0.06f * sin(progress * PI.toFloat()) else 0f

        return when (motion) {
            ItemCastMotion.ARM_FLIGHT -> ItemFloatPose(
                weight = weight,
                armYaw = side * 0.10f,
                armPitch = -1.22f,
                armRoll = side * 0.12f,
                itemYaw = side * 0.06f,
                itemPitch = -0.06f + wave * 0.04f,
                itemRoll = side * 0.08f + wave * 0.04f,
                itemX = side * 0.04f,
                itemY = 0.16f + wave * 0.04f,
                itemZ = -0.50f,
            )
            ItemCastMotion.CHEST_HOVER -> ItemFloatPose(
                weight = weight,
                armYaw = side * 0.42f,
                armPitch = -0.92f,
                armRoll = side * 0.16f,
                itemYaw = side * (0.04f + (if (orbitalEnabled) 0.18f * sin(orbitAngle) else 0f)),
                itemPitch = 0.12f + wave * 0.05f,
                itemRoll = side * 0.05f + wave * 0.06f,
                itemX = side * orbitX,
                itemY = 0.28f + wave * 0.03f + orbitY,
                itemZ = 0.26f + orbitZ,
            )
            ItemCastMotion.CHEST_SPIN -> ItemFloatPose(
                weight = weight,
                armYaw = side * 0.38f,
                armPitch = -0.88f,
                armRoll = side * 0.14f,
                itemYaw = spin,
                itemPitch = wave * 0.04f,
                itemRoll = side * 0.10f,
                itemX = side * (orbitX - 0.02f),
                itemY = 0.26f + wave * 0.03f + orbitY,
                itemZ = 0.24f + orbitZ,
            )
            ItemCastMotion.HAND_THRUST -> {
                val rollAlign = smoothStep((progress / 0.22f).coerceIn(0f, 1f)) *
                    (-PI.toFloat() / 4f)
                val pitchAlign = smoothStep(((progress - 0.20f) / 0.16f).coerceIn(0f, 1f)) *
                    (-PI.toFloat() / 2f)
                val thrust = smoothStep(((progress - 0.36f) / 0.18f).coerceIn(0f, 1f))
                ItemFloatPose(
                    weight = weight,
                    armYaw = side * 0.03f,
                    armPitch = -0.35f - thrust * 1.10f,
                    armRoll = side * 0.08f,
                    itemYaw = 0f,
                    itemPitch = pitchAlign,
                    itemRoll = rollAlign,
                    itemX = side * 0.04f,
                    itemY = 0.04f + wave * 0.015f,
                    itemZ = -0.04f - thrust * 0.50f,
                )
            }
        }
    }

    /** Subtle climax hesitation near progress = 0.50 so orbital spins snap with crisp emphasis. */
    private fun hitstopProgress(progress: Float, enabled: Boolean): Float {
        if (!enabled) return progress
        val delta = progress - 0.5f
        return (progress - sin(delta * PI.toFloat() * 2f) * 0.035f).coerceIn(0f, 1f)
    }

    private fun smoothStep(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
