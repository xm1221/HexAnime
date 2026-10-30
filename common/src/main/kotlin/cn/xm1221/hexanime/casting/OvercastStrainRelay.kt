package cn.xm1221.hexanime.casting

import cn.xm1221.hexanime.networking.msg.MsgOvercastStrainS2C
import net.minecraft.server.level.ServerPlayer

/** Raises staff strain only after Hex Casting has applied overcast damage. */
object OvercastStrainRelay {
    @JvmStatic
    fun broadcast(player: ServerPlayer, amount: Float) {
        val strain = OvercastStrain.initialForDamage(amount)
        if (strain <= 0f) return
        val msg = MsgOvercastStrainS2C(player.id, strain)
        msg.sendToPlayer(player)
        val watchers = PlayerTracking.watchers(player).filter(AnimationPreferences::acceptsEvents)
        if (watchers.isNotEmpty()) msg.sendToPlayers(watchers)
    }
}
