package cn.xm1221.hexanime.staff

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import cn.xm1221.hexanime.HexAnime
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
    /** Upper bound on accepted active packets. 50 ms is 20 Hz. */
    const val MIN_INTERVAL_NANOS = 50_000_000L

    /** How often a still-active aim is repeated so players who just started tracking catch up. */
    const val REFRESH_TICKS = 20

    private data class Published(
        val active: Boolean,
        val normX: Float,
        val normY: Float,
        val hand: InteractionHand,
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
        val normX = Staffs.clampNorm(msg.normX)
        val normY = Staffs.clampNorm(msg.normY)
        if (normX == null || normY == null) {
            HexAnime.LOGGER.debug("Dropping non-finite staff aim from {}", player.scoreboardName)
            return
        }

        val holding = Staffs.holdsStaff(player, msg.hand)
        val active = msg.active && holding
        if (msg.active && !allow(player)) return

        val next = Published(
            active = active,
            normX = if (active) normX else 0f,
            normY = if (active) normY else 0f,
            hand = msg.hand,
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
        if (last != null && now - last < MIN_INTERVAL_NANOS) return false
        lastAcceptedNanos[player.uuid] = now
        return true
    }

    private fun refresh(server: MinecraftServer) {
        if (server.tickCount % REFRESH_TICKS != 0) return
        for (player in server.playerList.players) {
            val aim = published[player.uuid] ?: continue
            if (!aim.active) continue
            if (!Staffs.holdsStaff(player, aim.hand)) {
                val cleared = aim.copy(active = false, normX = 0f, normY = 0f)
                published[player.uuid] = cleared
                broadcast(player, cleared)
                continue
            }
            broadcast(player, aim)
        }
    }

    private fun broadcast(player: ServerPlayer, aim: Published) {
        val watchers = tracking(player)
        if (watchers.isEmpty()) return
        MsgStaffAimS2C(
            entityId = player.id,
            active = aim.active,
            normX = aim.normX,
            normY = aim.normY,
            hand = aim.hand,
        ).sendToPlayers(watchers)
    }

    /**
     * Players in the same dimension inside this entity's vanilla client-tracking range.
     * The caster is not included; their own client already has the sampled aim.
     */
    private fun tracking(player: ServerPlayer): List<ServerPlayer> {
        val level = player.serverLevel()
        val blocks = player.type.clientTrackingRange() * 16.0
        val rangeSqr = blocks * blocks
        val out = ArrayList<ServerPlayer>()
        for (other in level.players()) {
            if (other === player) continue
            if (other.distanceToSqr(player) > rangeSqr) continue
            out.add(other)
        }
        return out
    }
}
