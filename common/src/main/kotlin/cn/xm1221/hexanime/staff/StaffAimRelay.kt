package cn.xm1221.hexanime.staff

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import cn.xm1221.hexanime.HexAnime
import cn.xm1221.hexanime.casting.PlayerTracking
import cn.xm1221.hexanime.casting.AnimationPreferences
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.networking.msg.MsgStaffAimC2S
import cn.xm1221.hexanime.networking.msg.MsgStaffAimS2C
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import java.util.UUID

/**
 * Server side of staff aim. Clients may only claim an aim while they are actually holding a staff.
 * Active updates are capped at about 20 Hz. Values are clamped. Tracking players receive an S2C copy.
 */
object StaffAimRelay {
    private data class Published(
        val active: Boolean,
        val normX: Float,
        val normY: Float,
        val hand: InteractionHand,
        val profileId: String,
        val variant: Int,
        val strain: Float,
    )

    private val published = HashMap<UUID, Published>()
    private val lastAcceptedNanos = HashMap<UUID, Long>()

    fun init() {
        PlayerEvent.PLAYER_QUIT.register { player ->
            published.remove(player.uuid)
            lastAcceptedNanos.remove(player.uuid)
        }
        TickEvent.SERVER_POST.register(::refresh)
    }

    fun handle(player: ServerPlayer, msg: MsgStaffAimC2S) {
        val config = HexAnimeServerConfig.config
        if (!config.enabled || !config.staffAim) {
            published[player.uuid]?.let { old ->
                val cleared = old.copy(active = false, normX = 0f, normY = 0f)
                published[player.uuid] = cleared
                broadcast(player, cleared)
            }
            return
        }

        val normX = Staffs.clampNorm(msg.normX)
        val normY = Staffs.clampNorm(msg.normY)
        if (normX == null || normY == null) {
            HexAnime.LOGGER.debug("Dropping non-finite staff aim from {}", player.scoreboardName)
            return
        }

        val sample = player.getItemInHand(msg.hand)
        val state = CastItems.snapshot(sample)
        val holding = state?.profile?.supportsAim == true
        val active = msg.active && holding
        if (msg.active && !allow(player)) return
        if (state == null) {
            val old = published[player.uuid] ?: return
            val cleared = old.copy(active = false, normX = 0f, normY = 0f)
            published[player.uuid] = cleared
            broadcast(player, cleared)
            return
        }

        val next = Published(
            active = active,
            normX = if (active) normX else 0f,
            normY = if (active) normY else 0f,
            hand = msg.hand,
            profileId = state.profile.id.toString(),
            variant = state.variant,
            strain = state.strain,
        )
        if (published[player.uuid] == next) return
        published[player.uuid] = next
        if (!holding && msg.active) {
            HexAnime.LOGGER.debug("Rejected staff aim from {}: not holding a staff in {}", player.scoreboardName, msg.hand)
        }
        broadcast(player, next)
    }

    private fun allow(player: ServerPlayer): Boolean {
        val now = System.nanoTime()
        val last = lastAcceptedNanos[player.uuid]
        if (last != null && now - last < HexAnimeServerConfig.config.minIntervalNanos()) return false
        lastAcceptedNanos[player.uuid] = now
        return true
    }

    private fun refresh(server: MinecraftServer) {
        val config = HexAnimeServerConfig.config
        if (!config.enabled || !config.staffAim || server.tickCount % config.refreshTicks() != 0) return
        for (player in server.playerList.players) {
            val aim = published[player.uuid] ?: continue
            if (!aim.active) continue
            val state = CastItems.snapshot(player.getItemInHand(aim.hand))
            if (state?.profile?.supportsAim != true) {
                val cleared = aim.copy(active = false, normX = 0f, normY = 0f)
                published[player.uuid] = cleared
                broadcast(player, cleared)
                continue
            }
            val refreshed = aim.copy(
                profileId = state.profile.id.toString(),
                variant = state.variant,
                strain = state.strain,
            )
            published[player.uuid] = refreshed
            broadcast(player, refreshed)
        }
    }

    private fun broadcast(player: ServerPlayer, aim: Published) {
        val watchers = PlayerTracking.watchers(player)
            .filter(AnimationPreferences::acceptsAim)
        if (watchers.isEmpty()) return
        MsgStaffAimS2C(
            entityId = player.id,
            active = aim.active,
            normX = aim.normX,
            normY = aim.normY,
            hand = aim.hand,
            profileId = aim.profileId,
            variant = aim.variant,
            strain = aim.strain,
        ).sendToPlayers(watchers)
    }
}
