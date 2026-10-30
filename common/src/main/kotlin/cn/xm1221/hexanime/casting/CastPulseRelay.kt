package cn.xm1221.hexanime.casting

import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.casting.item.CastItemProfiles
import cn.xm1221.hexanime.casting.item.StaffCastMotions
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.networking.msg.MsgCastPulseS2C
import cn.xm1221.hexanime.staff.StaffAimRelay
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CastPulseRelay {
    private const val MISHAP_SUPPRESSION_NANOS = 500_000_000L
    private data class MishapKey(val player: UUID, val hand: InteractionHand)

    private val recentMishapUntil = ConcurrentHashMap<MishapKey, Long>()

    fun init() {
        PlayerEvent.PLAYER_QUIT.register { player ->
            recentMishapUntil.keys.removeIf { it.player == player.uuid }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun broadcast(
        player: ServerPlayer,
        hand: InteractionHand,
        kind: CastPulseKind,
        variantOverride: Int? = null,
        forceScreenEffect: Boolean = false,
    ) {
        broadcastInternal(player, hand, kind, variantOverride, forceScreenEffect, null)
    }

    @JvmStatic
    @JvmOverloads
    fun broadcastMishap(
        player: ServerPlayer,
        hand: InteractionHand,
        variant: Int,
        staffEnvironment: Boolean,
        forceScreenEffect: Boolean = false,
    ) {
        broadcastInternal(
            player,
            hand,
            CastPulseKind.MISHAP,
            variant,
            forceScreenEffect,
            staffEnvironment,
        )
    }

    private fun broadcastInternal(
        player: ServerPlayer,
        hand: InteractionHand,
        kind: CastPulseKind,
        variantOverride: Int?,
        forceScreenEffect: Boolean,
        mishapFallbackStaff: Boolean?,
    ) {
        val now = System.nanoTime()
        val mishapKey = MishapKey(player.uuid, hand)
        if (kind == CastPulseKind.CAST || kind == CastPulseKind.BREAK) {
            val mishapUntil = recentMishapUntil[mishapKey]
            if (mishapUntil != null && now < mishapUntil) return
            if (mishapUntil != null) recentMishapUntil.remove(mishapKey, mishapUntil)
        }

        val config = HexAnimeServerConfig.config
        if (!config.enabled) return

        val held = player.getItemInHand(hand)
        // A breaking stack may have count zero while retaining item identity and components.
        val sample = held.copy().also { it.count = 1 }
        val state = CastItems.snapshot(sample)
        val environmentState = when (mishapFallbackStaff) {
            true -> state?.takeIf { !it.packagedHex && it.profile.animatesAsStaff }
            false -> state?.takeIf { it.packagedHex }
            null -> state
        }
        if (environmentState == null &&
            !(kind == CastPulseKind.MISHAP && mishapFallbackStaff != null)
        ) {
            return
        }
        val fallbackId = mishapFallbackStaff?.let { CastItemProfiles.mishapFallbackId(it) }
        val profile = environmentState?.profile
            ?: fallbackId?.let { CastItemProfiles.byId(it.toString()) }
            ?: return
        val packagedHex = environmentState?.packagedHex ?: false
        val strain = environmentState?.strain ?: 0f
        val staff = !packagedHex && profile.animatesAsStaff
        when (kind) {
            CastPulseKind.CAST, CastPulseKind.BREAK -> {
                val allowed = if (staff) config.staffPulse else if (packagedHex) config.packagedHexPulse else false
                if (!allowed) return
            }
            CastPulseKind.MISHAP -> if (!config.mishapPulse) return
            CastPulseKind.DAMAGE -> return
        }
        if (kind == CastPulseKind.BREAK && !profile.pulseStyle.allowBreak) return
        if (kind == CastPulseKind.MISHAP) {
            recentMishapUntil[mishapKey] = now + MISHAP_SUPPRESSION_NANOS
        }

        val variant = when {
            variantOverride != null -> variantOverride.coerceAtLeast(0)
            staff && kind == CastPulseKind.CAST -> {
                val (normX, normY) = StaffAimRelay.lastCursor(player)
                StaffCastMotions.variantForCursor(normX, normY)
            }
            else -> environmentState?.variant ?: 0
        }

        val msg = MsgCastPulseS2C(
            entityId = player.id,
            hand = hand,
            profileId = profile.id.toString(),
            variant = variant,
            strain = strain,
            kind = kind.networkId,
            forceScreenEffect = forceScreenEffect,
            displayStack = sample,
        )
        // The caster always receives their own event; remote subscription only controls watchers.
        msg.sendToPlayer(player)
        val watchers = PlayerTracking.watchers(player)
            .filter(AnimationPreferences::acceptsEvents)
        if (watchers.isNotEmpty()) msg.sendToPlayers(watchers)
    }
}
