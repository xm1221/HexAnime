package cn.xm1221.hexanime.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.networking.msg.*

fun HexAnimeMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> {
            HexAnimeServerConfig.onSyncConfig(serverConfig)
        }

        is MsgStaffAimS2C -> CastAnimationClient.onRemote(this)
        is MsgCastPulseS2C -> CastAnimationClient.onPulse(this)
        is MsgOvercastStrainS2C -> CastAnimationClient.onOvercast(this)
    }
}
