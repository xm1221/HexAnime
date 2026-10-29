package cn.xm1221.hexamine.forge

import cn.xm1221.hexamine.HexAmine
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent

object ForgeHexAmineServer {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLDedicatedServerSetupEvent) {
        HexAmine.initServer()
    }
}
