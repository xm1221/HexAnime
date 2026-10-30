package cn.xm1221.hexanime.casting

import at.petrak.hexcasting.common.lib.HexDamageTypes
import cn.xm1221.hexanime.networking.msg.MsgOvercastStrainS2C
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import net.minecraft.server.level.ServerPlayer

/** Raises staff strain only when Hex actually deals overcast damage. */
object OvercastStrainRelay {
    fun init() {
        EntityEvent.LIVING_HURT.register(EntityEvent.LivingHurt { entity, source, amount ->
            if (!entity.level().isClientSide &&
                entity is ServerPlayer &&
                amount > 0f &&
                source.`is`(HexDamageTypes.OVERCAST)
            ) {
                val strain = OvercastStrain.initialForDamage(amount)
                if (strain > 0f) {
                    val msg = MsgOvercastStrainS2C(entity.id, strain)
                    msg.sendToPlayer(entity)
                    val watchers = PlayerTracking.watchers(entity).filter(AnimationPreferences::acceptsEvents)
                    if (watchers.isNotEmpty()) msg.sendToPlayers(watchers)
                }
            }
            EventResult.pass()
        })
    }
}
