package cn.xm1221.hexamine.fabric

import cn.xm1221.hexamine.HexAmine
import net.fabricmc.api.DedicatedServerModInitializer

object FabricHexAmineServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        HexAmine.initServer()
    }
}
