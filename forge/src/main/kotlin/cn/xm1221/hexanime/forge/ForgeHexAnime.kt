package cn.xm1221.hexanime.forge

import dev.architectury.platform.forge.EventBuses
import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.forge.datagen.ForgeHexAnimeDatagen
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(HexAnime.MODID)
class ForgeHexAnime {
    init {
        MOD_BUS.apply {
            EventBuses.registerModEventBus(HexAnime.MODID, this)
            addListener(ForgeHexAnimeClient::init)
            addListener(ForgeHexAnimeDatagen::init)
            addListener(ForgeHexAnimeServer::init)
        }
        HexAnime.init()
    }
}
