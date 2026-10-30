package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.casting.item.CastItemProfiles
import cn.xm1221.hexanime.casting.item.CastItemState
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.client.anim.CastAnimationTuning
import cn.xm1221.hexanime.client.anim.CastHandFrame
import cn.xm1221.hexanime.client.anim.CastItemAnimation
import cn.xm1221.hexanime.client.anim.CastItemLayer
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.networking.msg.MsgCastPulseS2C
import cn.xm1221.hexanime.networking.msg.MsgStaffAimS2C
import dev.architectury.event.events.client.ClientTickEvent
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand

object CastAnimationClient {
    val LAYER_ID = HexAnime.id("cast_item")

    fun init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
            LAYER_ID,
            CastAnimationTuning.LAYER_PRIORITY,
        ) { player -> CastItemLayer(player) }
        AnimationPreferencesClient.init()
        CursorSampler.init()
        ClientTickEvent.CLIENT_POST.register { minecraft ->
            minecraft.level?.players()?.forEach { player ->
                animation(player)?.observeHeldItems()
            }
        }
        HexAnime.LOGGER.info("Registered cast-item animation layer {}", LAYER_ID)
    }

    fun apply(
        player: AbstractClientPlayer,
        active: Boolean,
        normX: Float,
        normY: Float,
        hand: InteractionHand,
        state: CastItemState?,
    ) {
        animation(player)?.setAim(active, normX, normY, hand, state)
    }

    fun onRemote(msg: MsgStaffAimS2C) {
        if (!HexAnimeClientConfig.config.receiveRemoteAim) return
        val minecraft = Minecraft.getInstance()
        if (minecraft.player?.id == msg.entityId) return
        val entity = minecraft.level?.getEntity(msg.entityId) as? AbstractClientPlayer ?: return
        val resolved = CastItemProfiles.byId(msg.profileId)
            ?: CastItems.snapshot(entity.getItemInHand(msg.hand))?.profile
        animation(entity)?.setAim(
            active = msg.active,
            normX = msg.normX,
            normY = msg.normY,
            hand = msg.hand,
            profile = resolved,
            variant = msg.variant,
            strain = msg.strain,
            remote = true,
        )
    }

    fun onPulse(msg: MsgCastPulseS2C) {
        val minecraft = Minecraft.getInstance()
        val entity = minecraft.level?.getEntity(msg.entityId) as? AbstractClientPlayer ?: return
        val isSelf = minecraft.player?.id == msg.entityId
        if (!HexAnimeClientConfig.config.receiveRemoteEvents && !isSelf) return
        val profile = CastItemProfiles.byId(msg.profileId) ?: return
        val kind = CastPulseKind.fromNetwork(msg.kind) ?: return
        animation(entity)?.triggerPulse(
            hand = msg.hand,
            profile = profile,
            variant = msg.variant,
            strain = msg.strain,
            kind = kind,
        )
        if (isSelf) CameraShakeClient.trigger(kind)
    }


    fun frame(player: AbstractClientPlayer, hand: InteractionHand, partialTick: Float): CastHandFrame =
        animation(player)?.frame(hand, partialTick) ?: CastHandFrame.EMPTY

    fun isMishapActive(player: AbstractClientPlayer, hand: InteractionHand): Boolean =
        animation(player)?.isMishapActive(hand) == true

    fun animationOf(player: AbstractClientPlayer): CastItemAnimation? = animation(player)

    private fun animation(player: AbstractClientPlayer): CastItemAnimation? {
        val found = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID)
        return (found as? CastItemLayer)?.animation
    }

}
