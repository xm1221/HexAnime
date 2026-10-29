package cn.xm1221.hexanime.fabric

import cn.xm1221.hexanime.HexAnime
import net.fabricmc.api.DedicatedServerModInitializer

object FabricHexAnimeServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        HexAnime.initServer()
    }
}
