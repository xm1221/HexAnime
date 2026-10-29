package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.staff.Staffs
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand

/**
 * Original staff-only first-person path. Packaged Hex actions use playerAnimator's
 * THIRD_PERSON_MODEL instead; this override applies only while a staff aim is active.
 */
object StaffFirstPerson {
    private const val YAW_SPREAD = 0.28f
    private const val PITCH_SPREAD = 0.16f
    private const val MODEL_YAW_SPREAD = 0.32f
    private const val MODEL_PITCH_SPREAD = 0.24f
    private const val BASE_VERTICAL_PITCH = -0.48f

    @JvmStatic
    fun pushAim(
        poseStack: PoseStack,
        player: AbstractClientPlayer,
        hand: InteractionHand,
        partialTick: Float,
    ): Boolean {
        if (CastAnimationClient.isMishapActive(player, hand)) return false
        if (!Staffs.holdsStaff(player, hand)) return false
        val frame = CastAnimationClient.frame(player, hand, partialTick)
        if (frame.profile?.supportsAim != true || frame.aimWeight <= 0.001f) return false

        val yaw = frame.aimYaw / MODEL_YAW_SPREAD * YAW_SPREAD * frame.aimWeight
        val pitch = BASE_VERTICAL_PITCH * frame.aimWeight +
            frame.aimPitch / MODEL_PITCH_SPREAD * PITCH_SPREAD * frame.aimWeight
        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotation(yaw))
        poseStack.mulPose(Axis.XP.rotation(pitch))
        return true
    }
}
