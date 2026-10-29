package cn.xm1221.hexanime.client

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory
import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.client.anim.StaffFollowAnimation
import cn.xm1221.hexanime.client.anim.StaffFollowLayer
import cn.xm1221.hexanime.networking.msg.MsgStaffAimS2C
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand

/**
 * Client wire-up: one playerAnimator layer per player, fed by the local sampler or by S2C aims.
 */
object StaffFollowClient {
    val LAYER_ID = HexAnime.id("staff_follow")

    fun init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
            LAYER_ID,
            StaffFollowAnimation.LAYER_PRIORITY,
        ) { player ->
            StaffFollowLayer(player)
        }
        CursorSampler.init()
        HexAnime.LOGGER.info("Registered staff-follow animation layer {}", LAYER_ID)
    }

    fun apply(player: AbstractClientPlayer, active: Boolean, normX: Float, normY: Float, hand: InteractionHand) {
        layer(player)?.aim?.setAim(active, normX, normY, hand)
    }

    fun onRemote(msg: MsgStaffAimS2C) {
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        if (minecraft.player?.id == msg.entityId) return
        val entity = minecraft.level?.getEntity(msg.entityId) as? AbstractClientPlayer ?: return
        apply(entity, msg.active, msg.normX, msg.normY, msg.hand)
    }

    private fun layer(player: AbstractClientPlayer): StaffFollowLayer? {
        val found = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID)
        return found as? StaffFollowLayer
    }
}
