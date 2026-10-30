package cn.xm1221.hexanime.casting

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.casting.item.StaffCastMotions
import cn.xm1221.hexanime.staff.StaffAimRelay
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Staff finishing flourish. Fired from a real spell execution, including muted meta
 * evaluations. Repeat executions inside [DEBOUNCE_TICKS] share one pose.
 */
object StaffCastRelay {
    const val DEBOUNCE_TICKS = 10L

    private val lastCastTick = ConcurrentHashMap<UUID, Long>()

    fun init() {
        PlayerEvent.PLAYER_QUIT.register { player -> lastCastTick.remove(player.uuid) }
    }

    @JvmStatic
    fun onAttemptSpell(harness: CastingVM) {
        val player = harness.env.castingEntity as? ServerPlayer ?: return
        if (player.level().isClientSide) return
        val hand = harness.env.castingHand
        val state = CastItems.snapshot(player.getItemInHand(hand)) ?: return
        if (state.packagedHex || !state.profile.animatesAsStaff) return

        val now = player.serverLevel().gameTime
        val last = lastCastTick[player.uuid]
        if (last != null && now - last < DEBOUNCE_TICKS) return
        lastCastTick[player.uuid] = now

        val (normX, normY) = StaffAimRelay.lastCursor(player)
        val variant = StaffCastMotions.variantForCursor(normX, normY)
        CastPulseRelay.broadcast(player, hand, CastPulseKind.CAST, variant)
    }
}
