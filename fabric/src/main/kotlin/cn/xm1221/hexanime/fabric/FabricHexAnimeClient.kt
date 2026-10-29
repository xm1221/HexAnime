package cn.xm1221.hexanime.fabric

import cn.xm1221.hexanime.HexAnimeClient
import net.fabricmc.api.ClientModInitializer

/** Fabric client entrypoint. TODO: staff-follows-cursor lives behind [HexAnimeClient.init]. */
object FabricHexAnimeClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexAnimeClient.init()
    }
}
