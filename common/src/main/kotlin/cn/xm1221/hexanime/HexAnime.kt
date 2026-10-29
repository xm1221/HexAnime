package cn.xm1221.hexanime

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.networking.HexAnimeNetworking
import cn.xm1221.hexanime.registry.HexAnimeActions
import cn.xm1221.hexanime.staff.StaffAimRelay

object HexAnime {
    const val MODID = "hexanime"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)

    fun init() {
        HexAnimeServerConfig.init()
        initRegistries(
            HexAnimeActions,
        )
        HexAnimeNetworking.init()
        StaffAimRelay.init()
    }

    fun initServer() {
        HexAnimeServerConfig.initServer()
    }
}
