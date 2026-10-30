package cn.xm1221.hexanime.config

import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.networking.msg.MsgSyncConfigS2C
import dev.architectury.event.events.common.PlayerEvent
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
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionResult

object HexAnimeServerConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = syncedServerConfig ?: holder.config.server

    private var syncedServerConfig: ServerConfig? = null

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )
        holder.registerSaveListener { _, _ -> InteractionResult.FAIL }
    }

    fun initServer() {
        PlayerEvent.PLAYER_JOIN.register { player ->
            MsgSyncConfigS2C(holder.config.server).sendToPlayer(player)
        }
    }

    fun onSyncConfig(serverConfig: ServerConfig?) {
        syncedServerConfig = serverConfig
    }

    @Config(name = HexAnime.MODID)
    class GlobalConfig(
        @Category("server")
        @TransitiveObject
        val server: ServerConfig = ServerConfig(),
    ) : GlobalData()

    @Config(name = "server")
    class ServerConfig : ConfigData {
        @Tooltip
        var enabled: Boolean = true

        @Tooltip
        var staffAim: Boolean = true

        @Tooltip
        var packagedHexPulse: Boolean = true

        @Tooltip
        var staffPulse: Boolean = true

        @Tooltip
        var mishapPulse: Boolean = true

        @Tooltip
        var refreshIntervalTicks: Int = 20
            private set

        @Tooltip
        var maxUpdatesPerSecond: Int = 20
            private set

        fun refreshTicks(): Int = refreshIntervalTicks.coerceIn(5, 100)

        fun minIntervalNanos(): Long {
            val rate = maxUpdatesPerSecond.coerceIn(1, 40)
            return 1_000_000_000L / rate
        }

        fun encode(buf: FriendlyByteBuf) {
            buf.writeBoolean(enabled)
            buf.writeBoolean(staffAim)
            buf.writeBoolean(packagedHexPulse)
            buf.writeBoolean(mishapPulse)
            buf.writeVarInt(refreshTicks())
            buf.writeVarInt(maxUpdatesPerSecond.coerceIn(1, 40))
            buf.writeBoolean(staffPulse)
        }

        companion object {
            fun decode(buf: FriendlyByteBuf): ServerConfig = ServerConfig().also {
                it.enabled = buf.readBoolean()
                it.staffAim = buf.readBoolean()
                it.packagedHexPulse = buf.readBoolean()
                it.mishapPulse = buf.readBoolean()
                it.refreshIntervalTicks = buf.readVarInt()
                it.maxUpdatesPerSecond = buf.readVarInt()
                it.staffPulse = buf.readBoolean()
            }
        }
    }
}
