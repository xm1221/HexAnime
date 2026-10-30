package cn.xm1221.hexanime.config

import cn.xm1221.hexanime.HexAnime
import dev.architectury.event.events.client.ClientPlayerEvent
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer

object HexAnimeClientConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = holder.config.client

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register { _ ->
            HexAnimeServerConfig.onSyncConfig(null)
        }
    }

    @Config(name = HexAnime.MODID)
    class GlobalConfig : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = ClientConfig()
    }

    @Config(name = "client")
    class ClientConfig : ConfigData {
        @Tooltip
        var enabled: Boolean = true

        @Tooltip
        var damageAnimations: Boolean = true

        @Tooltip
        var cameraShake: Boolean = true

        @Tooltip
        var firstPersonAnimations: Boolean = true

        @Tooltip
        var syncToServer: Boolean = true

        @Tooltip
        var receiveRemoteAim: Boolean = true

        @Tooltip
        var receiveRemoteEvents: Boolean = true

        @Tooltip
        var staffTipParticles: Boolean = true

        @Tooltip
        var mindspliceHeadPitch: Double = -0.28

        @Tooltip
        var quenchedTremor: Double = 0.016

        @Tooltip
        var quenchedStep: Double = 0.08

        @Tooltip
        var mindspliceDrift: Double = 0.03
    }
}
