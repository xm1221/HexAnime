package cn.xm1221.hexanime.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import cn.xm1221.hexanime.HexAnimeClient

object FabricHexAnimeModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexAnimeClient::getConfigScreen)
}
