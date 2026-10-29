package cn.xm1221.hexanime.networking

import dev.architectury.networking.NetworkChannel
import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.networking.msg.HexAnimeMessageCompanion

object HexAnimeNetworking {
    val CHANNEL: NetworkChannel = NetworkChannel.create(HexAnime.id("networking_channel"))

    fun init() {
        for (subclass in HexAnimeMessageCompanion::class.sealedSubclasses) {
            subclass.objectInstance?.register(CHANNEL)
        }
        HexAnime.LOGGER.info("Registered HexAnime network channel {}", HexAnime.id("networking_channel"))
    }
}
