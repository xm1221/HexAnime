package cn.xm1221.hexanime.networking.msg

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.networking.HexAnimeNetworking
import cn.xm1221.hexanime.networking.handler.applyOnClient
import cn.xm1221.hexanime.networking.handler.applyOnServer
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface HexAnimeMessage

sealed interface HexAnimeMessageC2S : HexAnimeMessage {
    fun sendToServer() {
        HexAnimeNetworking.CHANNEL.sendToServer(this)
    }
}

sealed interface HexAnimeMessageS2C : HexAnimeMessage {
    fun sendToPlayer(player: ServerPlayer) {
        HexAnimeNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        HexAnimeNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

sealed interface HexAnimeMessageCompanion<T : HexAnimeMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun apply(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        when (ctx.env) {
            EnvType.SERVER, null -> {
                HexAnime.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
                when (msg) {
                    is HexAnimeMessageC2S -> msg.applyOnServer(ctx)
                    else -> HexAnime.LOGGER.warn("Message not handled on server: {}", msg::class)
                }
            }
            EnvType.CLIENT -> {
                HexAnime.LOGGER.debug("Client received packet: {}", this)
                when (msg) {
                    is HexAnimeMessageS2C -> msg.applyOnClient(ctx)
                    else -> HexAnime.LOGGER.warn("Message not handled on client: {}", msg::class)
                }
            }
        }
    }

    fun register(channel: NetworkChannel) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, ::apply)
    }
}
