package cn.xm1221.hexanime.fabric

import cn.xm1221.hexanime.HexAnime
import net.fabricmc.api.ModInitializer

object FabricHexAnime : ModInitializer {
    override fun onInitialize() {
        HexAnime.init()
    }
}
