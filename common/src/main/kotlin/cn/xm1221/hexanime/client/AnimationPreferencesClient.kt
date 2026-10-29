package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.networking.msg.MsgAnimationPreferencesC2S
import dev.architectury.event.events.client.ClientPlayerEvent
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionResult

object AnimationPreferencesClient {
    fun init() {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register { _ -> send() }
        HexAnimeClientConfig.holder.registerSaveListener { _, _ ->
            send()
            InteractionResult.PASS
        }
    }

    fun send() {
        if (Minecraft.getInstance().player == null) return
        val config = HexAnimeClientConfig.config
        MsgAnimationPreferencesC2S(
            receiveStaffAim = config.receiveRemoteAim,
            receiveCastEvents = config.receiveRemoteEvents,
        ).sendToServer()
        if (!config.syncToServer) CursorSampler.clearServerAim()
    }
}
