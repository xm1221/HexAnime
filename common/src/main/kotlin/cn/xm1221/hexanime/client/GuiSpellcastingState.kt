package cn.xm1221.hexanime.client

import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Shared cached access to GuiSpellcasting's private drawState so both live stroke mirroring
 * and the 3D Hex pattern circle inspect active strokes without duplicating reflection.
 */
object GuiSpellcastingState {
    private var drawStateField: Field? = null
    private var drawStateResolved = false
    private var wipPatternGetter: Method? = null
    private var wipGetterResolved = false

    fun isDrawing(gui: GuiSpellcasting): Boolean {
        val state = rawDrawState(gui) ?: return false
        return state.javaClass.simpleName == "Drawing"
    }

    fun wipPattern(gui: GuiSpellcasting): HexPattern? {
        val drawState = rawDrawState(gui) ?: return null
        if (drawState.javaClass.simpleName != "Drawing") return null
        if (!wipGetterResolved) {
            wipGetterResolved = true
            wipPatternGetter = runCatching {
                drawState.javaClass.getMethod("getWipPattern").apply { isAccessible = true }
            }.getOrNull()
        }
        return runCatching { wipPatternGetter?.invoke(drawState) as? HexPattern }.getOrNull()
    }

    private fun rawDrawState(gui: GuiSpellcasting): Any? {
        if (!drawStateResolved) {
            drawStateResolved = true
            drawStateField = runCatching {
                GuiSpellcasting::class.java.getDeclaredField("drawState").apply { isAccessible = true }
            }.getOrNull()
        }
        return runCatching { drawStateField?.get(gui) }.getOrNull()
    }
}
