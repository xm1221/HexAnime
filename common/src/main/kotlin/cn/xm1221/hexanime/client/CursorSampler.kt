package cn.xm1221.hexanime.client

import at.petrak.hexcasting.client.gui.GuiSpellcasting
import cn.xm1221.hexanime.casting.item.CastItemState
import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.mixin.client.GuiSpellcastingAccessor
import cn.xm1221.hexanime.networking.msg.MsgStaffAimC2S
import cn.xm1221.hexanime.staff.Staffs
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand
import kotlin.math.abs

/**
 * Samples the spell GUI at client-tick rate. The wire value remains normalised cursor space; the
 * angular interpretation is profile-specific and happens on each receiving client.
 */
object CursorSampler {
    const val SEND_INTERVAL_NANOS = 50_000_000L
    const val CHANGE_EPSILON = 0.01f

    private var casting = false
    private var lastHand = InteractionHand.MAIN_HAND
    private var lastX = 0f
    private var lastY = 0f
    private var lastState: CastItemState? = null
    private var lastSentActive = false
    private var lastSentX = 0f
    private var lastSentY = 0f
    private var lastSentHand = InteractionHand.MAIN_HAND
    private var lastSentNanos = 0L

    fun init() {
        ClientTickEvent.CLIENT_POST.register { minecraft -> tick(minecraft) }
    }

    private fun tick(minecraft: Minecraft) {
        val player = minecraft.player
        if (player == null || minecraft.level == null) {
            casting = false
            return
        }

        val gui = minecraft.screen as? GuiSpellcasting
        if (gui == null) {
            stop(player)
            return
        }

        val hand = (gui as GuiSpellcastingAccessor).`hexanime$getHandOpenedWith`()
        val state = CastItems.snapshot(player.getItemInHand(hand))
        if (state?.profile?.supportsAim != true) {
            stop(player, hand)
            return
        }

        val (normX, normY) = sample(minecraft, gui)
        casting = true
        lastHand = hand
        lastState = state
        lastX = normX
        lastY = normY
        CastAnimationClient.apply(
            player,
            HexAnimeClientConfig.config.enabled,
            normX,
            normY,
            hand,
            state,
        )

        if (!HexAnimeClientConfig.config.syncToServer) {
            if (lastSentActive) {
                lastSentActive = false
                MsgStaffAimC2S(false, 0f, 0f, hand).sendToServer()
            }
            return
        }

        val moved = abs(normX - lastSentX) > CHANGE_EPSILON ||
            abs(normY - lastSentY) > CHANGE_EPSILON ||
            hand != lastSentHand ||
            !lastSentActive
        if (!moved) return
        val now = System.nanoTime()
        if (lastSentActive && now - lastSentNanos < SEND_INTERVAL_NANOS) return
        send(true, normX, normY, hand, force = false)
    }

    private fun stop(player: AbstractClientPlayer, hand: InteractionHand = lastHand) {
        if (!casting) return
        casting = false
        CastAnimationClient.apply(player, false, lastX, lastY, hand, lastState)
        send(false, lastX, lastY, hand, force = true)
    }

    private fun sample(minecraft: Minecraft, gui: GuiSpellcasting): Pair<Float, Float> {
        val window = minecraft.window
        val mouse = minecraft.mouseHandler
        val screenW = window.screenWidth.toDouble().coerceAtLeast(1.0)
        val screenH = window.screenHeight.toDouble().coerceAtLeast(1.0)
        val mouseX = (mouse.xpos() * window.guiScaledWidth / screenW).toFloat()
        val mouseY = (mouse.ypos() * window.guiScaledHeight / screenH).toFloat()
        val center = gui.coordsOffset()
        val halfW = gui.width * 0.5f
        val halfH = gui.height * 0.5f
        val normX = if (halfW == 0f) 0f else
            ((mouseX - center.x) / halfW).coerceIn(-Staffs.NORM_LIMIT, Staffs.NORM_LIMIT)
        val normY = if (halfH == 0f) 0f else
            ((mouseY - center.y) / halfH).coerceIn(-Staffs.NORM_LIMIT, Staffs.NORM_LIMIT)
        return normX to normY
    }

    private fun send(active: Boolean, normX: Float, normY: Float, hand: InteractionHand, force: Boolean) {
        if (!HexAnimeClientConfig.config.syncToServer) return
        val now = System.nanoTime()
        if (!force && active && now - lastSentNanos < SEND_INTERVAL_NANOS) return
        lastSentActive = active
        lastSentX = normX
        lastSentY = normY
        lastSentHand = hand
        lastSentNanos = now
        MsgStaffAimC2S(active, normX, normY, hand).sendToServer()
    }

    fun clearServerAim() {
        if (!lastSentActive) return
        lastSentActive = false
        MsgStaffAimC2S(false, 0f, 0f, lastSentHand).sendToServer()
    }
}
