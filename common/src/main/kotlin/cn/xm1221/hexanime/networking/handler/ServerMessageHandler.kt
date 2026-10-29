package cn.xm1221.hexanime.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.hexanime.networking.msg.*
import cn.xm1221.hexanime.casting.AnimationPreferences
import cn.xm1221.hexanime.staff.StaffAimRelay
import net.minecraft.server.level.ServerPlayer

fun HexAnimeMessageC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgStaffAimC2S -> {
            val player = ctx.player
            if (player is ServerPlayer) StaffAimRelay.handle(player, this)
        }

        is MsgAnimationPreferencesC2S -> {
            val player = ctx.player
            if (player is ServerPlayer) AnimationPreferences.update(player, this)
        }

    }
}
