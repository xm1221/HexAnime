package cn.xm1221.hexamine.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import cn.xm1221.hexamine.HexAmineClient

object FabricHexAmineModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexAmineClient::getConfigScreen)
}
