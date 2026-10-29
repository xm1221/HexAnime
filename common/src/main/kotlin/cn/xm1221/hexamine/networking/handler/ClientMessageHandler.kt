package cn.xm1221.hexamine.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.hexamine.config.HexAmineServerConfig
import cn.xm1221.hexamine.networking.msg.*

fun HexAmineMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> {
            HexAmineServerConfig.onSyncConfig(serverConfig)
        }

        // add more client-side message handlers here
    }
}
