package cn.xm1221.hexamine

import cn.xm1221.hexamine.client.StaffFollowClient
import cn.xm1221.hexamine.config.HexAmineClientConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexAmineClient {
    fun init() {
        HexAmineClientConfig.init()
        // TODO: staff-follows-cursor feature (stubs only for now)
        StaffFollowClient.init()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(HexAmineClientConfig.GlobalConfig::class.java, parent).get()
    }
}
