package cn.xm1221.hexanime.casting

import cn.xm1221.hexanime.networking.msg.MsgAnimationPreferencesC2S
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ClientAnimationPreferences(
    val receiveStaffAim: Boolean,
    val receiveCastEvents: Boolean,
)

object AnimationPreferences {
    private val all = ClientAnimationPreferences(receiveStaffAim = true, receiveCastEvents = true)
    private val preferences = ConcurrentHashMap<UUID, ClientAnimationPreferences>()

    fun init() {
        PlayerEvent.PLAYER_QUIT.register { player -> preferences.remove(player.uuid) }
    }

    fun update(player: ServerPlayer, msg: MsgAnimationPreferencesC2S) {
        preferences[player.uuid] = ClientAnimationPreferences(
            receiveStaffAim = msg.receiveStaffAim,
            receiveCastEvents = msg.receiveCastEvents,
        )
    }

    fun acceptsAim(player: ServerPlayer): Boolean =
        (preferences[player.uuid] ?: all).receiveStaffAim

    fun acceptsEvents(player: ServerPlayer): Boolean =
        (preferences[player.uuid] ?: all).receiveCastEvents
}
