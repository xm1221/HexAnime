package cn.xm1221.hexanime.client

import dev.architectury.event.events.client.ClientTickEvent
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import cn.xm1221.hexanime.mixin.client.GuiSpellcastingAccessor
import cn.xm1221.hexanime.networking.msg.MsgStaffAimC2S
import cn.xm1221.hexanime.staff.Staffs
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand

/**
 * Samples the spell-GUI cursor at about 20 Hz and only sends a packet when the sample changes.
 * The local arm is updated every client tick; the network copy is what other players see.
 *
 * The sample is the mouse position relative to [GuiSpellcasting.coordsOffset] (the grid center),
 * divided by half the GUI size so the edges are ±1. Screen +x is right, +y is down.
 */
object CursorSampler {
    /** Matches [cn.xm1221.hexanime.staff.StaffAimRelay.MIN_INTERVAL_NANOS]. One send per 50 ms. */
    const val SEND_INTERVAL_NANOS = 50_000_000L

    /** Skip a packet when the cursor has barely moved. */
    const val CHANGE_EPSILON = 0.01f

    private var casting = false
    private var lastHand = InteractionHand.MAIN_HAND
    private var lastX = 0f
    private var lastY = 0f
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
            if (casting) {
                casting = false
                StaffFollowClient.apply(player, false, lastX, lastY, lastHand)
                send(false, lastX, lastY, lastHand, force = true)
            }
            return
        }

        val hand = (gui as GuiSpellcastingAccessor).`hexanime$getHandOpenedWith`()
        if (!Staffs.holdsStaff(player, hand)) {
            if (casting) {
                casting = false
                StaffFollowClient.apply(player, false, lastX, lastY, hand)
                send(false, lastX, lastY, hand, force = true)
            }
            return
        }

        val (normX, normY) = sample(minecraft, gui)
        casting = true
        lastHand = hand
        lastX = normX
        lastY = normY
        StaffFollowClient.apply(player, true, normX, normY, hand)

        val moved = kotlin.math.abs(normX - lastSentX) > CHANGE_EPSILON ||
            kotlin.math.abs(normY - lastSentY) > CHANGE_EPSILON ||
            hand != lastSentHand ||
            !lastSentActive
        if (!moved) return
        val now = System.nanoTime()
        if (lastSentActive && now - lastSentNanos < SEND_INTERVAL_NANOS) return
        send(true, normX, normY, hand, force = false)
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
        val normX = if (halfW == 0f) 0f else ((mouseX - center.x) / halfW).coerceIn(-Staffs.NORM_LIMIT, Staffs.NORM_LIMIT)
        val normY = if (halfH == 0f) 0f else ((mouseY - center.y) / halfH).coerceIn(-Staffs.NORM_LIMIT, Staffs.NORM_LIMIT)
        return normX to normY
    }

    private fun send(active: Boolean, normX: Float, normY: Float, hand: InteractionHand, force: Boolean) {
        val now = System.nanoTime()
        if (!force && active && now - lastSentNanos < SEND_INTERVAL_NANOS) return
        lastSentActive = active
        lastSentX = normX
        lastSentY = normY
        lastSentHand = hand
        lastSentNanos = now
        MsgStaffAimC2S(active, normX, normY, hand).sendToServer()
    }
}
