package cn.xm1221.hexamine

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cn.xm1221.hexamine.config.HexAmineServerConfig
import cn.xm1221.hexamine.networking.HexAmineNetworking
import cn.xm1221.hexamine.registry.HexAmineActions

object HexAmine {
    const val MODID = "hexamine"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)

    fun init() {
        HexAmineServerConfig.init()
        initRegistries(
            HexAmineActions,
        )
        HexAmineNetworking.init()
    }

    fun initServer() {
        HexAmineServerConfig.initServer()
    }
}
