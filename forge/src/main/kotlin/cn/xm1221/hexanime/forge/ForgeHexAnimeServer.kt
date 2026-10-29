package cn.xm1221.hexanime.forge

import cn.xm1221.hexanime.HexAnime
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent

object ForgeHexAnimeServer {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLDedicatedServerSetupEvent) {
        HexAnime.initServer()
    }
}
