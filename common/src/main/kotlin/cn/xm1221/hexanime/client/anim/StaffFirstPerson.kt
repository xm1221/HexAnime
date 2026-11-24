package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.staff.Staffs
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand
import kotlin.math.abs

/**
 * First-person PoseStack hook for staff aiming, equip draw flourishes, and
 * offhand ritual / dual-wield synergy while the opposite hand is casting.
 */
object StaffFirstPerson {
    private const val YAW_SPREAD = 0.28f
    private const val PITCH_SPREAD = 0.16f
    private const val MODEL_YAW_SPREAD = 0.32f
    private const val MODEL_PITCH_SPREAD = 0.24f
    private const val BASE_VERTICAL_PITCH = -0.32f
    private const val BASE_VERTICAL_LIFT = 0.12f

    @JvmStatic
    fun pushAim(
        poseStack: PoseStack,
        player: AbstractClientPlayer,
        hand: InteractionHand,
        partialTick: Float,
    ): Boolean {
        if (CastAnimationClient.isMishapActive(player, hand)) return false
        val frame = CastAnimationClient.frame(player, hand, partialTick)
        val profile = frame.profile
        val hasStaffAim = profile?.animatesAsStaff == true &&
            profile.supportsAim &&
            frame.aimWeight > 0.001f

        if (hasStaffAim) {
            val rot = FirstPersonEnvelope.ROTATION
            val pos = FirstPersonEnvelope.TRANSLATION
            val elytra = CastAnimationClient.elytraPose(player, partialTick)
            val yaw = frame.aimYaw / MODEL_YAW_SPREAD * YAW_SPREAD * frame.aimWeight * rot
            val pitch = BASE_VERTICAL_PITCH * frame.aimWeight +
                frame.aimPitch / MODEL_PITCH_SPREAD * PITCH_SPREAD * frame.aimWeight * rot +
                elytra.pitchOffset
            poseStack.pushPose()
            poseStack.translate(
                frame.firstPersonX * pos,
                frame.firstPersonY * pos + BASE_VERTICAL_LIFT * frame.aimWeight * (1f - 0.55f * elytra.weight) + elytra.verticalOffset,
                frame.firstPersonZ * pos + elytra.forwardOffset,
            )
            poseStack.mulPose(Axis.YP.rotation(yaw + frame.firstPersonYaw * rot))
            poseStack.mulPose(Axis.XP.rotation(pitch + frame.firstPersonPitch * rot))
            poseStack.mulPose(Axis.ZP.rotation(frame.firstPersonRoll * rot + elytra.rollOffset))
            return true
        }

        // First-person equip draw flourish (staff twirl or packaged catch) when idle.
        if (CastAnimationClient.isEquipDrawActive(player, hand)) {
            val hasEquipMotion = abs(frame.firstPersonPitch) +
                abs(frame.firstPersonRoll) +
                abs(frame.firstPersonY) > 0.001f
            if (hasEquipMotion) {
                poseStack.pushPose()
                poseStack.translate(0f, frame.firstPersonY, -0.06f * (frame.firstPersonY * 6f).coerceIn(0f, 1f))
                poseStack.mulPose(Axis.XP.rotation(frame.firstPersonPitch))
                poseStack.mulPose(Axis.ZP.rotation(frame.firstPersonRoll))
                return true
            }
        }

        // Offhand ritual / dual-wield synergy while the opposite hand is casting in first person.
        val opposite = if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        val oppFrame = CastAnimationClient.frame(player, opposite, partialTick)
        val offhandSignal = abs(oppFrame.offhandYawDelta) +
            abs(oppFrame.offhandPitchDelta) +
            abs(oppFrame.offhandRollDelta)
        if (offhandSignal > 0.01f && (oppFrame.aimWeight > 0.02f || oppFrame.hasMotion)) {
            val activity = maxOf(oppFrame.aimWeight, if (oppFrame.hasMotion) 0.85f else 0f).coerceIn(0f, 1f)
            val inward = if (Staffs.armBone(player, hand) == Staffs.RIGHT_ARM) -1f else 1f
            poseStack.pushPose()
            poseStack.translate(
                inward * 0.14f * activity,
                0.18f * activity,
                -0.08f * activity,
            )
            poseStack.mulPose(Axis.YP.rotation(oppFrame.offhandYawDelta * 0.65f))
            poseStack.mulPose(Axis.XP.rotation(oppFrame.offhandPitchDelta * 0.42f))
            poseStack.mulPose(Axis.ZP.rotation(oppFrame.offhandRollDelta * 0.65f))
            return true
        }

        return false
    }
}
