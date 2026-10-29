package cn.xm1221.hexamine.forge

import dev.architectury.platform.forge.EventBuses
import cn.xm1221.hexamine.HexAmine
import cn.xm1221.hexamine.forge.datagen.ForgeHexAmineDatagen
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(HexAmine.MODID)
class ForgeHexAmine {
    init {
        MOD_BUS.apply {
            EventBuses.registerModEventBus(HexAmine.MODID, this)
            addListener(ForgeHexAmineClient::init)
            addListener(ForgeHexAmineDatagen::init)
            addListener(ForgeHexAmineServer::init)
        }
        HexAmine.init()
    }
}
