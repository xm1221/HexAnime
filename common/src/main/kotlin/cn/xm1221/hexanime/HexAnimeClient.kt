package cn.xm1221.hexanime

import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexAnimeClient {
    fun init() {
        HexAnimeClientConfig.init()
        CastAnimationClient.init()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexAnimeClientConfig.GlobalConfig::class.java, parent).get()
    }
}
