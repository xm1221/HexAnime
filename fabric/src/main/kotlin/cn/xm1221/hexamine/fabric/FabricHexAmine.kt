package cn.xm1221.hexamine.fabric

import cn.xm1221.hexamine.HexAmine
import net.fabricmc.api.ModInitializer

object FabricHexAmine : ModInitializer {
    override fun onInitialize() {
        HexAmine.init()
    }
}
