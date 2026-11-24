package cn.xm1221.hexanime.client.vfx

import at.petrak.hexcasting.api.casting.math.HexPattern
import net.minecraft.world.phys.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Converts HexPattern polylines (and deterministic 60-degree fallback runes)
 * into unit-normalized 2D coordinates inside [-1, 1] for 3D magic-circle projection.
 */
object HexPatternGeometry {
    data class NormPoint(val x: Float, val y: Float)

    fun fromHexPattern(pattern: HexPattern): List<NormPoint> {
        val raw = pattern.toLines(1f, Vec2.ZERO)
        if (raw.size < 2) return emptyList()
        return normalizePolyline(raw.map { it.x to it.y })
    }

    fun normalizePolyline(points: List<Pair<Float, Float>>): List<NormPoint> {
        if (points.size < 2) return emptyList()
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for ((x, y) in points) {
            if (!x.isFinite() || !y.isFinite()) continue
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (!minX.isFinite() || !maxX.isFinite()) return emptyList()
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val halfExtent = maxOf((maxX - minX) * 0.5f, (maxY - minY) * 0.5f, 0.35f)
        return points.map { (x, y) ->
            NormPoint((x - cx) / halfExtent, (y - cy) / halfExtent)
        }
    }

    /**
     * Generates an authentic 60-degree Hex-lattice glyph for packaged items or remote casters
     * whose GUI pattern list is not available locally.
     */
    fun fallbackGlyph(seed: Int): List<NormPoint> {
        var dir = Math.floorMod(seed, 6)
        var x = 0f
        var y = 0f
        val raw = mutableListOf(x to y)
        var state = seed * 1103515245 + 12345
        val steps = 5
        for (i in 0 until steps) {
            state = state * 1664525 + 1013904223
            val turn = ((state ushr 16) % 5) - 2 // -2..+2 steps of 60 deg (never 180 backtrack)
            dir = Math.floorMod(dir + turn, 6)
            val angle = dir * (PI / 3.0)
            x += cos(angle).toFloat()
            y += sin(angle).toFloat()
            raw += x to y
        }
        return normalizePolyline(raw)
    }
}
