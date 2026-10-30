package cn.xm1221.hexanime.client

import at.petrak.hexcasting.client.gui.GuiSpellcasting
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.casting.item.StaffMaterial
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.config.HexAnimeServerConfig
import cn.xm1221.hexanime.mixin.client.GuiSpellcastingAccessor
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand

/**
 * Client half of a staff pattern acknowledgement. The micro-pulse is local.
 * A stack-clear finish is only a backup for spells that never played a casting sound;
 * an in-progress cast flourish is left alone.
 */
object StaffPatternClient {
    @JvmStatic
    fun onServerView(success: Boolean, stackClear: Boolean) {
        if (!success) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        if (!HexAnimeClientConfig.config.enabled) return

        val gui = minecraft.screen as? GuiSpellcasting
        val hand = if (gui != null) {
            (gui as GuiSpellcastingAccessor).`hexanime$getHandOpenedWith`()
        } else {
            InteractionHand.MAIN_HAND
        }
        val snapshot = CastItems.snapshot(player.getItemInHand(hand)) ?: return
        if (snapshot.profile.staffMaterial == StaffMaterial.NONE) return

        val animation = CastAnimationClient.animationOf(player) ?: return
        animation.triggerMicroPulse(hand)
        val server = HexAnimeServerConfig.config
        if (stackClear && server.enabled && server.staffPulse) {
            animation.triggerLocalStaffCast(hand, snapshot.profile, snapshot.strain)
        }
    }
}
