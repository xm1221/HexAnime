package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.staff.Staffs
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand

/**
 * Turns the vanilla first-person staff pose toward the same smoothed cursor as the third-person arm.
 *
 * The rotation is applied in view space, outside [net.minecraft.client.renderer.ItemInHandRenderer]'s
 * own push, so the whole held item (and the empty-hand arm) orbits the camera. That renderer runs in
 * the world hand pass, before screens, so the spell grid stays above it. Nothing here draws.
 *
 * Third-person angles are not reused: those are absolute bone rotations (forward is -π/2). Here the
 * vanilla pose is already the "forward" pose, and only the cursor offset is added, scaled by the fade.
 */
object StaffFirstPerson {
    /**
     * View-space travel at the edge of the grid. Smaller than the third-person bone spread so the
     * item stays in the first-person view (about 70° FOV) instead of swinging off-screen.
     */
    private const val YAW_SPREAD = 0.72f
    private const val PITCH_SPREAD = 0.62f

    /**
     * @return true when a pose was pushed and the caller must [PoseStack.popPose] after vanilla does.
     */
    @JvmStatic
    fun pushAim(poseStack: PoseStack, player: AbstractClientPlayer, hand: InteractionHand, partialTick: Float): Boolean {
        val aim = StaffFollowClient.aim(player) ?: return false
        if (aim.hand != hand || !aim.isActive) return false
        if (!Staffs.holdsStaff(player, hand)) return false
        val frame = aim.frame(partialTick)
        if (frame.weight <= 0.001f) return false

        val yaw = -frame.normX * YAW_SPREAD * frame.weight
        val pitch = frame.normY * PITCH_SPREAD * frame.weight
        poseStack.pushPose()
        // Last mulPose is applied to the vertex first, so this is yaw around view-up, then pitch.
        // +X is screen-right and +Y is screen-down on the cursor. Positive view-Y turns the held
        // item toward screen-left, so a cursor on the right uses a negative yaw. Positive view-X
        // pitches it downward, matching ItemInHandRenderer's view-pitch bob.
        poseStack.mulPose(Axis.YP.rotation(yaw))
        poseStack.mulPose(Axis.XP.rotation(pitch))
        return true
    }
}
