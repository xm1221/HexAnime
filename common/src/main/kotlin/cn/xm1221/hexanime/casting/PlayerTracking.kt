package cn.xm1221.hexanime.casting

import net.minecraft.server.level.ServerPlayer

object PlayerTracking {
    /**
     * Players in the same dimension inside the target's vanilla client-tracking range. The target
     * is not included, because local animation is driven directly.
     */
    fun watchers(player: ServerPlayer): List<ServerPlayer> {
        val level = player.serverLevel()
        val blocks = player.type.clientTrackingRange() * 16.0
        val rangeSqr = blocks * blocks
        return buildList {
            for (other in level.players()) {
                if (other === player) continue
                if (other.distanceToSqr(player) > rangeSqr) continue
                add(other)
            }
        }
    }
}
