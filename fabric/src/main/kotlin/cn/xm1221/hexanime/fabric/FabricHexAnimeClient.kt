package cn.xm1221.hexanime.fabric

import cn.xm1221.hexanime.HexAnimeClient
import net.fabricmc.api.ClientModInitializer

/** Fabric client entrypoint. */
object FabricHexAnimeClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexAnimeClient.init()
    }
}
