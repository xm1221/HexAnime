package cn.xm1221.hexamine.fabric

import cn.xm1221.hexamine.HexAmineClient
import net.fabricmc.api.ClientModInitializer

/** Fabric client entrypoint. TODO: staff-follows-cursor lives behind [HexAmineClient.init]. */
object FabricHexAmineClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexAmineClient.init()
    }
}
