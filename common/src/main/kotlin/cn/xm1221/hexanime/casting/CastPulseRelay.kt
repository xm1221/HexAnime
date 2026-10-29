package cn.xm1221.hexanime.casting

import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.networking.msg.MsgCastPulseS2C
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CastPulseRelay {
    private const val MISHAP_SUPPRESSION_NANOS = 500_000_000L
    private val recentMishapUntil = ConcurrentHashMap<UUID, Long>()

    fun init() {
        PlayerEvent.PLAYER_QUIT.register { player -> recentMishapUntil.remove(player.uuid) }
    }

    @JvmStatic
    fun broadcast(player: ServerPlayer, hand: InteractionHand, kind: CastPulseKind) {
        val now = System.nanoTime()
        if (kind == CastPulseKind.MISHAP) {
            recentMishapUntil[player.uuid] = now + MISHAP_SUPPRESSION_NANOS
        } else if (kind == CastPulseKind.CAST || kind == CastPulseKind.BREAK) {
            val mishapUntil = recentMishapUntil[player.uuid]
            if (mishapUntil != null && now < mishapUntil) return
            if (mishapUntil != null) recentMishapUntil.remove(player.uuid, mishapUntil)
        }

        val config = HexAnimeServerConfig.config
        if (!config.enabled) return
        when (kind) {
            CastPulseKind.CAST, CastPulseKind.BREAK -> if (!config.packagedHexPulse) return
            CastPulseKind.MISHAP -> if (!config.mishapPulse) return
            CastPulseKind.DAMAGE -> return
        }

        val held = player.getItemInHand(hand)
        // A breaking stack may have count zero while retaining item identity and components.
        val sample = held.copy().also { it.count = 1 }
        val state = CastItems.snapshot(sample) ?: return
        if (!state.packagedHex && kind in setOf(CastPulseKind.CAST, CastPulseKind.BREAK)) return

        val msg = MsgCastPulseS2C(
            entityId = player.id,
            hand = hand,
            profileId = state.profile.id.toString(),
            variant = state.variant,
            strain = state.strain,
            kind = kind.networkId,
        )
        // The caster always receives their own event; remote subscription only controls watchers.
        msg.sendToPlayer(player)
        val watchers = PlayerTracking.watchers(player)
            .filter(AnimationPreferences::acceptsEvents)
        if (watchers.isNotEmpty()) msg.sendToPlayers(watchers)
    }
}
