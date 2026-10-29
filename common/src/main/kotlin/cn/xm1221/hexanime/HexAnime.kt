package cn.xm1221.hexanime

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.casting.item.CastItemProfiles
import cn.xm1221.hexanime.networking.HexAnimeNetworking
import cn.xm1221.hexanime.staff.StaffAimRelay
import cn.xm1221.hexanime.casting.AnimationPreferences
import cn.xm1221.hexanime.casting.CastPulseRelay

object HexAnime {
    const val MODID = "hexanime"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)

    fun init() {
        HexAnimeServerConfig.init()
        CastItemProfiles.bootstrap()
        HexAnimeNetworking.init()
        AnimationPreferences.init()
        CastPulseRelay.init()
        StaffAimRelay.init()
    }

    fun initServer() {
        HexAnimeServerConfig.initServer()
    }
}
