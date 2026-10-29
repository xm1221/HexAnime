package cn.xm1221.hexamine.forge

import cn.xm1221.hexamine.HexAmineClient
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

/** Forge client setup. TODO: staff-follows-cursor lives behind [HexAmineClient.init]. */
object ForgeHexAmineClient {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLClientSetupEvent) {
        HexAmineClient.init()
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> HexAmineClient.getConfigScreen(parent) }
        }
    }
}
