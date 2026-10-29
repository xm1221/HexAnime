package cn.xm1221.hexamine.networking.msg

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.hexamine.HexAmine
import cn.xm1221.hexamine.networking.HexAmineNetworking
import cn.xm1221.hexamine.networking.handler.applyOnClient
import cn.xm1221.hexamine.networking.handler.applyOnServer
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface HexAmineMessage

sealed interface HexAmineMessageC2S : HexAmineMessage {
    fun sendToServer() {
        HexAmineNetworking.CHANNEL.sendToServer(this)
    }
}

sealed interface HexAmineMessageS2C : HexAmineMessage {
    fun sendToPlayer(player: ServerPlayer) {
        HexAmineNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        HexAmineNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

sealed interface HexAmineMessageCompanion<T : HexAmineMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun apply(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        when (ctx.env) {
            EnvType.SERVER, null -> {
                HexAmine.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
                when (msg) {
                    is HexAmineMessageC2S -> msg.applyOnServer(ctx)
                    else -> HexAmine.LOGGER.warn("Message not handled on server: {}", msg::class)
                }
            }
            EnvType.CLIENT -> {
                HexAmine.LOGGER.debug("Client received packet: {}", this)
                when (msg) {
                    is HexAmineMessageS2C -> msg.applyOnClient(ctx)
                    else -> HexAmine.LOGGER.warn("Message not handled on client: {}", msg::class)
                }
            }
        }
    }

    fun register(channel: NetworkChannel) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, ::apply)
    }
}
