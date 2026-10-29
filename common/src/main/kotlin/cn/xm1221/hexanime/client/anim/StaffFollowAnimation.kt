package cn.xm1221.hexanime.client.anim

import dev.kosmx.playerAnim.api.TransformType
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode
import dev.kosmx.playerAnim.api.layered.IAnimation
import dev.kosmx.playerAnim.api.layered.ModifierLayer
import dev.kosmx.playerAnim.core.util.Vec3f
import cn.xm1221.hexanime.staff.Staffs
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand

/**
 * Points the staff arm at a normalized GUI cursor.
 *
 * Rotations are radians, matching playerAnimator's [TransformType.ROTATION] (see KosmX's
 * [dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier] example, which converts pitch with
 * `Math.toRadians` before adding it). The pose replaces the vanilla arm swing while the weight is 1,
 * and lerps back to it while fading out.
 *
 * First person stays on [FirstPersonMode.NONE]. playerAnimator's vanilla and third-person-model
 * modes would replace this pose (absolute third-person arm angles) onto the first-person model, or
 * cancel the vanilla hands. The held staff in first person is the vanilla item pose, so
 * [cn.xm1221.hexanime.client.StaffFirstPerson] rotates that pose in view space instead. That pass
 * runs with the world, before the spell GUI, so the grid stays on top. Third person, including the
 * local player, still uses this layer.
 */
class StaffArmAimAnimation(
    private val player: AbstractClientPlayer,
) : IAnimation {
    var active: Boolean = false
    var targetX: Float = 0f
    var targetY: Float = 0f
    var hand: InteractionHand = InteractionHand.MAIN_HAND

    private var shownX = 0f
    private var shownY = 0f
    private var weight = 0f
    private var prevX = 0f
    private var prevY = 0f
    private var prevWeight = 0f

    fun setAim(active: Boolean, normX: Float, normY: Float, hand: InteractionHand) {
        this.hand = hand
        this.active = active && Staffs.holdsStaff(player, hand)
        if (this.active) {
            targetX = normX
            targetY = normY
        }
    }

    override fun isActive(): Boolean = active || weight > 0.01f

    override fun tick() {
        prevX = shownX
        prevY = shownY
        prevWeight = weight
        if (active && !Staffs.holdsStaff(player, hand)) {
            active = false
        }
        val desired = if (active) 1f else 0f
        weight = Mth.lerp(FADE_PER_TICK, weight, desired)
        if (!active && weight < 0.01f) weight = 0f
        if (active) {
            shownX = Mth.lerp(AIM_PER_TICK, shownX, targetX)
            shownY = Mth.lerp(AIM_PER_TICK, shownY, targetY)
        }
    }

    override fun setupAnim(tickDelta: Float) {}

    /** Smoothed cursor and fade, for the third-person bones and the first-person hand pose. */
    fun frame(tickDelta: Float): AimFrame {
        return AimFrame(
            Mth.lerp(tickDelta, prevX, shownX),
            Mth.lerp(tickDelta, prevY, shownY),
            Mth.lerp(tickDelta, prevWeight, weight),
        )
    }

    override fun get3DTransform(modelName: String, type: TransformType, tickDelta: Float, value0: Vec3f): Vec3f {
        if (type != TransformType.ROTATION) return value0
        val arm = Staffs.armBone(player, hand)
        if (modelName != arm && modelName != Staffs.itemBone(arm)) return value0

        val frame = frame(tickDelta)
        if (frame.weight <= 0.001f) return value0

        val (pitch, yaw) = angles(frame.normX, frame.normY)
        return Vec3f(
            Mth.lerp(frame.weight, value0.x, pitch),
            Mth.lerp(frame.weight, value0.y, yaw),
            Mth.lerp(frame.weight, value0.z, 0f),
        )
    }

    override fun getFirstPersonMode(tickDelta: Float): FirstPersonMode = FirstPersonMode.NONE

    companion object {
        /** Exponential smoothing toward the latest cursor sample. */
        const val AIM_PER_TICK = 0.45f

        /** Fade in / out. About a quarter of a second at 20 tps. */
        const val FADE_PER_TICK = 0.22f

        /**
         * Arm hanging down is xRot 0. Pointing forward is -π/2, the same pose Hex-like mods and vanilla
         * bows use (`rightArm.xRot = -PI/2 + head.xRot`).
         *
         * Screen +y is downward, so a cursor below center adds a positive pitch offset (arm lowers).
         * Screen +x is to the right. Vanilla points both arms left by increasing yRot (`arm.yRot = head.yRot`
         * while looking left), so a cursor on the left (negative x) needs a positive yRot. Both bones share
         * that sign; which bone we write is chosen by [Staffs.armBone].
         */
        fun angles(normX: Float, normY: Float): Pair<Float, Float> {
            val pitch = FORWARD_PITCH + normY * PITCH_SPREAD
            val yaw = -normX * YAW_SPREAD
            return pitch to yaw
        }

        private const val FORWARD_PITCH = (-Math.PI / 2.0).toFloat()
        private const val PITCH_SPREAD = 0.95f
        private const val YAW_SPREAD = 0.85f
    }
}

/** Interpolated aim. [normX] and [normY] are the GUI cursor, ±1 at the edges. [weight] is the fade. */
data class AimFrame(val normX: Float, val normY: Float, val weight: Float)

/** Per-player layer. Registered at priority [StaffFollowAnimation.LAYER_PRIORITY] so it wins over idle emotes. */
class StaffFollowLayer(val aim: StaffArmAimAnimation) : ModifierLayer<StaffArmAimAnimation>(aim) {
    constructor(player: AbstractClientPlayer) : this(StaffArmAimAnimation(player))
}

object StaffFollowAnimation {
    /** Higher than typical emote layers (often ≤ 100). Applied later, so it overrides them while active. */
    const val LAYER_PRIORITY = 1000
}
