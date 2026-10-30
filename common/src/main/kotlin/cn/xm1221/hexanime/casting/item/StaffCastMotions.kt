package cn.xm1221.hexanime.casting.item

import kotlin.math.abs

/**
 * Spatial finishing poses for a staff cast. These are not packaged-hex [VariantMotions].
 * Variant is chosen from the last raw cursor sample:
 * 0 level thrust, 1 ground tap, 2 sky raise, 3 horizontal sweep.
 */
object StaffCastMotions {
    private val motions = listOf(
        VariantMotion(
            variant = 0,
            durationTicks = 16,
            anticipation = 0.16f,
            bodyPitch = -0.06f,
            headPitch = -0.04f,
            armYaw = 0.08f,
            armPitch = -0.22f,
            armRoll = 0.06f,
            itemYaw = -0.10f,
            itemPitch = -0.28f,
            itemRoll = 0.12f,
            itemZ = -0.16f,
            offhandPitch = 0.08f,
            firstPersonPitch = -0.20f,
            firstPersonZ = -0.08f,
        ),
        VariantMotion(
            variant = 1,
            durationTicks = 14,
            anticipation = 0.14f,
            bodyPitch = 0.16f,
            bodyY = -0.06f,
            headPitch = 0.18f,
            armYaw = 0.06f,
            armPitch = 0.72f,
            armRoll = 0.10f,
            itemYaw = 0.08f,
            itemPitch = 0.96f,
            itemRoll = -0.16f,
            itemY = -0.22f,
            itemZ = 0.06f,
            leftLegPitch = 0.12f,
            rightLegPitch = 0.08f,
            firstPersonPitch = 0.55f,
            firstPersonY = -0.10f,
        ),
        VariantMotion(
            variant = 2,
            durationTicks = 18,
            anticipation = 0.18f,
            bodyPitch = -0.10f,
            bodyY = 0.10f,
            headPitch = -0.22f,
            armYaw = -0.06f,
            armPitch = -0.92f,
            armRoll = -0.14f,
            itemYaw = 0.12f,
            itemPitch = -1.05f,
            itemRoll = 0.18f,
            itemY = 0.36f,
            itemZ = -0.10f,
            offhandPitch = -0.16f,
            firstPersonPitch = -0.70f,
            firstPersonY = 0.12f,
            secondaryBeat = 0.16f,
        ),
        VariantMotion(
            variant = 3,
            durationTicks = 16,
            anticipation = 0.15f,
            bodyYaw = 0.34f,
            bodyRoll = 0.08f,
            headYaw = -0.16f,
            armYaw = 0.88f,
            armPitch = -0.18f,
            armRoll = 0.42f,
            itemYaw = 1.05f,
            itemPitch = -0.20f,
            itemRoll = -0.72f,
            itemX = -0.14f,
            offhandYaw = -0.22f,
            leftLegYaw = 0.10f,
            rightLegYaw = -0.08f,
            firstPersonYaw = 0.62f,
            firstPersonRoll = 0.28f,
            firstPersonX = -0.08f,
        ),
    )

    fun byVariant(variant: Int): VariantMotion = motions[Math.floorMod(variant, motions.size)]

    /**
     * Screen-space cursor: +Y is downward, matching [cn.xm1221.hexanime.client.CursorSampler].
     * Near the origin is a level thrust. A wider horizontal sample is a sweep.
     * Otherwise the vertical sign picks ground (down) or sky (up).
     */
    fun variantForCursor(normX: Float, normY: Float): Int {
        val x = if (normX.isFinite()) normX else 0f
        val y = if (normY.isFinite()) normY else 0f
        val ax = abs(x)
        val ay = abs(y)
        if (ax < CENTER_RADIUS && ay < CENTER_RADIUS) return 0
        if (ax >= ay) return 3
        return if (y > 0f) 1 else 2
    }

    private const val CENTER_RADIUS = 0.22f
}
