package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.MishapFlavor
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import dev.architectury.event.events.client.ClientGuiEvent
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object MishapScreenEffectPolicy {
    const val COOLDOWN_MILLIS = 20_000L
    const val DURATION_MILLIS = 2_200f

    fun supports(flavor: MishapFlavor): Boolean = flavor != MishapFlavor.RECOIL

    fun shouldTrigger(
        flavor: MishapFlavor,
        force: Boolean,
        millisSinceLast: Long,
    ): Boolean = supports(flavor) && (force || millisSinceLast >= COOLDOWN_MILLIS)

    fun envelope(progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        val attack = (t / 0.12f).coerceAtMost(1f)
        val release = ((1f - t) / 0.30f).coerceAtMost(1f)
        return minOf(attack, release).coerceIn(0f, 1f)
    }
}

/** Rare screen-space patterns; deliberately not a global shader replacement. */
object MishapScreenEffectClient {
    private var activeFlavor: MishapFlavor? = null
    private var startNanos = 0L
    private var lastTriggerNanos = 0L

    fun init() {
        ClientGuiEvent.RENDER_HUD.register { graphics, _ -> render(graphics) }
    }

    fun trigger(flavor: MishapFlavor, force: Boolean = false) {
        if (!HexAnimeClientConfig.config.enabled ||
            !HexAnimeClientConfig.config.mishapScreenEffects
        ) {
            return
        }
        val now = System.nanoTime()
        val elapsedMillis = (now - lastTriggerNanos) / 1_000_000L
        if (!MishapScreenEffectPolicy.shouldTrigger(flavor, force, elapsedMillis)) return
        lastTriggerNanos = now
        startNanos = now
        activeFlavor = flavor
    }

    private fun render(graphics: GuiGraphics) {
        if (!HexAnimeClientConfig.config.enabled ||
            !HexAnimeClientConfig.config.mishapScreenEffects
        ) {
            activeFlavor = null
            return
        }
        val flavor = activeFlavor ?: return
        val progress = (System.nanoTime() - startNanos) / 1_000_000f /
            MishapScreenEffectPolicy.DURATION_MILLIS
        if (progress >= 1f) {
            activeFlavor = null
            return
        }
        val strength = MishapScreenEffectPolicy.envelope(progress)
        when (flavor) {
            MishapFlavor.MEDIA -> renderMedia(graphics, progress, strength)
            MishapFlavor.SPATIAL -> renderSpatial(graphics, progress, strength)
            MishapFlavor.MIND -> renderMind(graphics, progress, strength)
            MishapFlavor.RECOIL -> Unit
        }
    }

    private fun renderMedia(graphics: GuiGraphics, progress: Float, strength: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val centerX = width / 2f
        val centerY = height * 0.48f
        val base = minOf(width, height).toFloat()
        val pulse = 0.65f + 0.35f * sin(progress * PI.toFloat() * 6f)
        graphics.fill(0, 0, width, height, argb((18 * strength).toInt(), 0x3A0710))
        vignette(graphics, width, height, strength, 0x5A1420)
        radialSpokes(
            graphics,
            centerX,
            centerY,
            base * (0.08f + progress * 0.08f),
            base * (0.50f + progress * 0.12f),
            18,
            -progress * 0.25f,
            argb((52 * strength * pulse).toInt(), 0xB62840),
        )
        polygonRing(
            graphics,
            centerX,
            centerY,
            base * (0.12f + progress * 0.14f),
            12,
            progress * 0.18f,
            argb((78 * strength * pulse).toInt(), 0xD8455C),
        )
        repeat(7) { index ->
            val x = width * (index + 0.5f) / 7f +
                sin(progress * PI.toFloat() * 2f + index).toFloat() * width * 0.025f
            val length = height * (0.18f + 0.06f * sin(index * 1.7f)) * strength
            graphics.fill(
                x.toInt(),
                0,
                (x + 2f).toInt().coerceAtLeast(x.toInt() + 1),
                length.toInt().coerceAtLeast(0),
                argb((55 * strength).toInt(), 0x9E2638),
            )
        }
    }

    private fun renderSpatial(graphics: GuiGraphics, progress: Float, strength: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        graphics.fill(0, 0, width, height, argb((14 * strength).toInt(), 0x071C2A))
        val cyan = argb((80 * strength).toInt(), 0x49CDE8)
        val magenta = argb((54 * strength).toInt(), 0xD34FC2)
        val slide = ((progress * 2f) % 1f) * width
        repeat(7) { index ->
            val y = ((index * 0.19f + progress * 0.45f) % 1f) * height
            val bandHeight = (height * (0.008f + index * 0.002f)).toInt().coerceAtLeast(2)
            graphics.fill(
                0,
                y.toInt(),
                width,
                (y.toInt() + bandHeight).coerceAtMost(height),
                if (index % 2 == 0) cyan else magenta,
            )
        }
        repeat(9) { index ->
            val offset = index * width / 8f - slide
            line(graphics, offset.toInt(), 0, (offset + height * 0.55f).toInt(), height, cyan)
            line(
                graphics,
                (offset + width * 0.45f).toInt(),
                0,
                (offset + width * 0.45f - height * 0.32f).toInt(),
                height,
                magenta,
            )
        }
        graphics.fill(
            0,
            (height * 0.47f).toInt(),
            width,
            (height * 0.49f).toInt() + 1,
            argb((32 * strength).toInt(), 0xBFEFFF),
        )
        val centerX = width / 2f
        val centerY = height * 0.5f
        val size = minOf(width, height) * 0.18f
        line(
            graphics,
            (centerX - size).toInt(),
            centerY.toInt(),
            centerX.toInt(),
            (centerY - size * 0.45f).toInt(),
            argb((120 * strength).toInt(), 0xE3FDFF),
        )
        line(
            graphics,
            centerX.toInt(),
            (centerY - size * 0.45f).toInt(),
            (centerX + size).toInt(),
            centerY.toInt(),
            argb((120 * strength).toInt(), 0xE3FDFF),
        )
        line(
            graphics,
            (centerX + size).toInt(),
            centerY.toInt(),
            centerX.toInt(),
            (centerY + size * 0.45f).toInt(),
            argb((120 * strength).toInt(), 0xE3FDFF),
        )
        line(
            graphics,
            centerX.toInt(),
            (centerY + size * 0.45f).toInt(),
            (centerX - size).toInt(),
            centerY.toInt(),
            argb((120 * strength).toInt(), 0xE3FDFF),
        )
    }

    private fun renderMind(graphics: GuiGraphics, progress: Float, strength: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        graphics.fill(0, 0, width, height, argb((20 * strength).toInt(), 0x1D0B35))
        val centerX = width / 2f
        val centerY = height * 0.46f
        val base = minOf(width, height).toFloat()
        val rotation = progress * PI.toFloat() * 0.8f
        val pulse = 0.70f + 0.30f * sin(progress * PI.toFloat() * 10f)
        hexRing(
            graphics,
            centerX,
            centerY,
            base * (0.24f + progress * 0.08f),
            rotation,
            argb((115 * strength).toInt(), 0xB58BFF),
        )
        hexRing(
            graphics,
            centerX,
            centerY,
            base * (0.36f + progress * 0.04f),
            -rotation * 0.65f,
            argb((62 * strength).toInt(), 0x6D50C8),
        )
        polygonRing(
            graphics,
            centerX,
            centerY,
            base * (0.15f + progress * 0.07f),
            3,
            -rotation * 1.6f,
            argb((110 * strength * pulse).toInt(), 0xE4CCFF),
        )
        radialSpokes(
            graphics,
            centerX,
            centerY,
            base * 0.06f,
            base * (0.42f + progress * 0.08f),
            12,
            rotation * 1.4f,
            argb((42 * strength * pulse).toInt(), 0x9C79EA),
        )
        vignette(graphics, width, height, strength, 0x35205F)
    }

    private fun hexRing(
        graphics: GuiGraphics,
        centerX: Float,
        centerY: Float,
        radius: Float,
        rotation: Float,
        color: Int,
    ) {
        polygonRing(graphics, centerX, centerY, radius, 6, rotation, color)
    }

    private fun polygonRing(
        graphics: GuiGraphics,
        centerX: Float,
        centerY: Float,
        radius: Float,
        sides: Int,
        rotation: Float,
        color: Int,
    ) {
        var previousX = centerX + cos(rotation) * radius
        var previousY = centerY + sin(rotation) * radius
        val step = PI.toFloat() * 2f / sides
        for (corner in 1..sides) {
            val angle = rotation + corner * step
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            line(graphics, previousX.toInt(), previousY.toInt(), x.toInt(), y.toInt(), color)
            previousX = x
            previousY = y
        }
    }

    private fun radialSpokes(
        graphics: GuiGraphics,
        centerX: Float,
        centerY: Float,
        innerRadius: Float,
        outerRadius: Float,
        count: Int,
        rotation: Float,
        color: Int,
    ) {
        repeat(count) { index ->
            val angle = rotation + index * PI.toFloat() * 2f / count.toFloat()
            val innerX = centerX + cos(angle) * innerRadius
            val innerY = centerY + sin(angle) * innerRadius
            val outerX = centerX + cos(angle) * outerRadius
            val outerY = centerY + sin(angle) * outerRadius
            line(graphics, outerX.toInt(), outerY.toInt(), innerX.toInt(), innerY.toInt(), color)
        }
    }

    private fun vignette(
        graphics: GuiGraphics,
        width: Int,
        height: Int,
        strength: Float,
        rgb: Int,
    ) {
        val depth = (minOf(width, height) * 0.12f).toInt().coerceAtLeast(8)
        repeat(8) { layer ->
            val color = argb(((layer + 1) * 5f * strength).toInt(), rgb)
            val inset = depth * layer / 8
            val thickness = (depth / 8).coerceAtLeast(1)
            graphics.fill(0, inset, width, inset + thickness, color)
            graphics.fill(0, height - inset - thickness, width, height - inset, color)
            graphics.fill(inset, 0, inset + thickness, height, color)
            graphics.fill(width - inset - thickness, 0, width - inset, height, color)
        }
    }

    private fun line(
        graphics: GuiGraphics,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        color: Int,
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val steps = max(abs(dx), abs(dy)).coerceAtLeast(1)
        for (step in 0..steps) {
            val x = x0 + dx * step / steps
            val y = y0 + dy * step / steps
            graphics.fill(x, y, x + 1, y + 1, color)
        }
    }

    private fun argb(alpha: Int, rgb: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)
}
