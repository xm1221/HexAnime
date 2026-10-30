package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.item.CastItemProfile
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack

object CastItemActivationPolicy {
    const val COOLDOWN_TICKS = 200
    const val MIN_CAST_STRAIN = 0.82f

    fun shouldShow(kind: CastPulseKind, strain: Float, ticksSinceLast: Int): Boolean = when (kind) {
        CastPulseKind.BREAK -> true
        CastPulseKind.CAST -> strain >= MIN_CAST_STRAIN && ticksSinceLast >= COOLDOWN_TICKS
        CastPulseKind.MISHAP, CastPulseKind.DAMAGE -> false
    }
}

/** Reuses vanilla's center-screen item activation, the same presentation used by a totem. */
object CastItemActivation {
    private var lastActivationTick = -10_000

    fun show(stack: ItemStack, profile: CastItemProfile, kind: CastPulseKind, strain: Float) {
        if (!HexAnimeClientConfig.config.enabled || profile.animatesAsStaff) return
        if (stack.isEmpty) return
        val player = Minecraft.getInstance().player ?: return
        val ticksSinceLast = player.tickCount - lastActivationTick
        if (!CastItemActivationPolicy.shouldShow(kind, strain, ticksSinceLast)) return
        lastActivationTick = player.tickCount
        Minecraft.getInstance().gameRenderer.displayItemActivation(stack.copy())
    }
}
