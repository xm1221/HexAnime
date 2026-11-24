package cn.xm1221.hexanime.client.vfx

import at.petrak.hexcasting.client.gui.GuiSpellcasting
import cn.xm1221.hexanime.casting.MishapFlavor
import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.client.GuiSpellcastingState
import cn.xm1221.hexanime.client.StaffTipParticles
import cn.xm1221.hexanime.client.registerWorldRender
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.mixin.client.GuiSpellcastingAccessor
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight 3D pigment-colored geometric circle with progressive multi-ring expansion
 * up to 10+ blocks, thick glowing quad strokes, projected Hex patterns, staff-tip ribbon
 * trails, and instant 3D mishap shatter rings.
 */
object ArcaneGeometryRenderer {
    private const val MAX_TRAIL_POINTS = 10
    private const val MAX_TRAIL_AGE_NANOS = 420_000_000L
    private const val SHATTER_DURATION_NANOS = 520_000_000L
    private const val PATTERN_CACHE_TTL_NANOS = 2_800_000_000L
    private const val MAX_RING_PATTERNS = 8

    private data class TrailKey(val uuid: UUID, val hand: InteractionHand)
    private data class TrailSample(val pos: Vec3, val nanos: Long)
    private data class ShatterBurst(val origin: Vec3, val color: Vector3f, val startNanos: Long, val patternCount: Int)
    private data class PatternSnapshot(
        val ringPatterns: List<List<HexPatternGeometry.NormPoint>>,
        val centerPattern: List<HexPatternGeometry.NormPoint>?,
        val totalCount: Int,
        val updatedNanos: Long,
    )

    private data class CircleVisualState(
        var radius: Float,
        var alpha: Float,
        var spin: Float,
        var lastNanos: Long,
        var resonance: Float,
        var rgb: Vector3f,
        var fallbackSeed: Int,
        var snapshot: PatternSnapshot?,
    )

    private val trails = ConcurrentHashMap<TrailKey, ArrayDeque<TrailSample>>()
    private val shatters = mutableListOf<ShatterBurst>()
    private val circleStates = ConcurrentHashMap<UUID, CircleVisualState>()
    private var localPatternCache: PatternSnapshot? = null
    private var registered = false

    fun init() {
        if (registered) return
        registered = true
        registerWorldRender { poseStack, bufferSource, camera, partialTick ->
            render(poseStack, bufferSource, camera, partialTick)
        }
    }

    fun tick(minecraft: Minecraft) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.arcaneCircleAndTrail) {
            trails.clear()
            circleStates.clear()
            localPatternCache = null
            return
        }
        val level = minecraft.level ?: run {
            trails.clear()
            circleStates.clear()
            localPatternCache = null
            return
        }
        val now = System.nanoTime()
        if (config.circleHexPatterns) {
            updateLocalPatternSnapshot(minecraft, now)
        } else {
            localPatternCache = null
        }

        for (player in level.players()) {
            val anim = CastAnimationClient.animationOf(player)
            if (anim?.isActive() != true) continue
            for (hand in InteractionHand.values()) {
                val frame = CastAnimationClient.frame(player, hand, 1f)
                val profile = frame.profile ?: continue
                if (!profile.animatesAsStaff) continue
                if (frame.aimWeight <= 0.05f && !frame.hasMotion) continue
                val tip = StaffTipParticles.tipPosition(player, hand, frame, 1f)
                val key = TrailKey(player.uuid, hand)
                val deque = trails.getOrPut(key) { ArrayDeque(MAX_TRAIL_POINTS) }
                val last = deque.firstOrNull()
                if (last == null || last.pos.distanceToSqr(tip) > 0.0008) {
                    deque.addFirst(TrailSample(tip, now))
                    while (deque.size > MAX_TRAIL_POINTS) deque.removeLast()
                }
            }
        }
        trails.entries.removeIf { (_, deque) ->
            while (deque.isNotEmpty() && now - deque.last().nanos > MAX_TRAIL_AGE_NANOS) {
                deque.removeLast()
            }
            deque.isEmpty()
        }
    }

    private fun updateLocalPatternSnapshot(minecraft: Minecraft, now: Long) {
        val gui = minecraft.screen as? GuiSpellcasting
        if (gui != null) {
            val accessor = gui as GuiSpellcastingAccessor
            val rawPatterns = accessor.`hexanime$getPatterns`()
            val resolved = rawPatterns
                .takeLast(MAX_RING_PATTERNS)
                .mapNotNull { entry ->
                    HexPatternGeometry.fromHexPattern(entry.pattern).takeIf { it.size >= 2 }
                }
            val wipPattern = GuiSpellcastingState.wipPattern(gui)
            val wip = if (wipPattern != null) {
                HexPatternGeometry.fromHexPattern(wipPattern).takeIf { it.size >= 2 }
            } else {
                null
            }
            val center = wip ?: resolved.lastOrNull()
            val totalCount = rawPatterns.size + (if (wip != null) 1 else 0)
            if (resolved.isNotEmpty() || center != null) {
                localPatternCache = PatternSnapshot(resolved, center, totalCount, now)
            }
        } else {
            val cached = localPatternCache
            if (cached != null && now - cached.updatedNanos > PATTERN_CACHE_TTL_NANOS) {
                localPatternCache = null
            }
        }
    }

    fun triggerShatter(player: AbstractClientPlayer, flavor: MishapFlavor) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.arcaneCircleAndTrail) return
        val origin = player.position().add(0.0, 0.08, 0.0)
        val color = when (flavor) {
            MishapFlavor.MEDIA -> Vector3f(0.92f, 0.20f, 0.34f)
            MishapFlavor.SPATIAL -> Vector3f(0.28f, 0.85f, 0.96f)
            MishapFlavor.MIND -> Vector3f(0.68f, 0.28f, 0.95f)
            MishapFlavor.RECOIL -> StaffTipParticles.pigmentColor(player, origin) ?: Vector3f(0.95f, 0.45f, 0.55f)
        }
        val count = maxOf(
            localPatternCache?.totalCount ?: 0,
            CastAnimationClient.animationOf(player)?.maxPatternCount() ?: 0,
        )
        synchronized(shatters) {
            shatters += ShatterBurst(origin, color, System.nanoTime(), count)
        }
    }

    private fun render(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        camera: Camera,
        partialTick: Float,
    ) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.arcaneCircleAndTrail) return
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val localPlayer = minecraft.player
        val isFirstPersonCam = minecraft.options.cameraType.isFirstPerson
        val camPos = camera.position
        val now = System.nanoTime()
        val showPatterns = config.circleHexPatterns

        poseStack.pushPose()
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z)

        val quadConsumer = bufferSource.getBuffer(RenderType.lightning())
        val mat = poseStack.last().pose()

        for (player in level.players()) {
            val isLocalPlayer = localPlayer?.id == player.id
            val isLocalFirst = isFirstPersonCam && isLocalPlayer
            val anim = CastAnimationClient.animationOf(player)

            val px = Mth.lerp(partialTick.toDouble(), player.xo, player.x)
            val py = Mth.lerp(partialTick.toDouble(), player.yo, player.y) + 0.04
            val pz = Mth.lerp(partialTick.toDouble(), player.zo, player.z)
            val origin = Vec3(px, py, pz)

            var activeWeight = 0f
            var resonance = 0f
            var patternCount = 0
            var snapshot: PatternSnapshot? = null
            var variantSeed = player.id * 7
            if (anim != null && anim.isActive()) {
                val mainFrame = CastAnimationClient.frame(player, InteractionHand.MAIN_HAND, partialTick)
                val offFrame = CastAnimationClient.frame(player, InteractionHand.OFF_HAND, partialTick)
                activeWeight = maxOf(
                    mainFrame.aimWeight,
                    offFrame.aimWeight,
                    if (mainFrame.hasMotion || offFrame.hasMotion) 0.65f else 0f,
                )
                if (activeWeight > 0.04f) {
                    resonance = maxOf(mainFrame.resonance, offFrame.resonance)
                    snapshot = if (isLocalPlayer && showPatterns) localPatternCache else null
                    patternCount = maxOf(snapshot?.totalCount ?: 0, anim.maxPatternCount())
                    variantSeed = (mainFrame.motion?.variant ?: offFrame.motion?.variant ?: 0) + player.id * 7
                }
            }

            val existing = circleStates[player.uuid]
            if (activeWeight <= 0.04f && existing == null) continue

            val targetAlpha = if (activeWeight > 0.04f) {
                ArcaneGeometryMath.circleAlpha(activeWeight, resonance, isLocalFirst)
            } else {
                0f
            }
            val state = if (existing == null) {
                val initRgb = StaffTipParticles.pigmentColor(player, origin, partialTick)
                    ?: Vector3f(0.75f, 0.55f, 1.0f)
                val initRadius = ArcaneGeometryMath.circleRadius(
                    aimWeight = activeWeight,
                    resonance = resonance,
                    patternCount = patternCount,
                    expandingEnabled = config.expandingMagicCircle,
                )
                CircleVisualState(
                    radius = initRadius * 0.65f,
                    alpha = 0f,
                    spin = 0f,
                    lastNanos = now,
                    resonance = resonance,
                    rgb = initRgb,
                    fallbackSeed = variantSeed,
                    snapshot = snapshot,
                ).also { circleStates[player.uuid] = it }
            } else {
                existing
            }

            val dtSec = ((now - state.lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
            state.lastNanos = now

            if (activeWeight > 0.04f) {
                val targetRadius = ArcaneGeometryMath.circleRadius(
                    aimWeight = activeWeight,
                    resonance = resonance,
                    patternCount = patternCount,
                    expandingEnabled = config.expandingMagicCircle,
                )
                val radRate = if (targetRadius >= state.radius) {
                    ArcaneGeometryMath.EXPANSION_RATE_PER_SEC
                } else {
                    ArcaneGeometryMath.RETRACTION_RATE_PER_SEC
                }
                state.radius = ArcaneGeometryMath.approachDt(state.radius, targetRadius, radRate, dtSec)
                state.resonance = ArcaneGeometryMath.approachDt(state.resonance, resonance, 3.0f, dtSec)
                state.rgb = StaffTipParticles.pigmentColor(player, origin, partialTick) ?: state.rgb
                state.fallbackSeed = variantSeed
                if (snapshot != null) state.snapshot = snapshot
            } else {
                // Gently contract while lingering and fading out.
                state.radius = ArcaneGeometryMath.approachDt(
                    state.radius,
                    state.radius * 0.72f,
                    ArcaneGeometryMath.RETRACTION_RATE_PER_SEC * 0.55f,
                    dtSec,
                )
                if (!showPatterns) state.snapshot = null
            }

            val alphaRate = if (targetAlpha >= state.alpha) {
                ArcaneGeometryMath.ALPHA_ATTACK_PER_SEC
            } else {
                ArcaneGeometryMath.ALPHA_RELEASE_PER_SEC
            }
            state.alpha = ArcaneGeometryMath.approachDt(state.alpha, targetAlpha, alphaRate, dtSec)
            state.spin += dtSec * (0.70f + 0.90f * state.resonance)

            if (targetAlpha <= 0f && state.alpha <= 0.012f) {
                circleStates.remove(player.uuid)
                continue
            }

            if (state.alpha > 0.015f) {
                drawMagicCircle(
                    mat = mat,
                    consumer = quadConsumer,
                    origin = origin,
                    radius = state.radius,
                    spin = state.spin,
                    rgb = state.rgb,
                    alpha = state.alpha,
                    resonance = state.resonance,
                    minimalFirstPerson = isLocalFirst,
                    showPatterns = showPatterns,
                    snapshot = if (showPatterns) state.snapshot else null,
                    fallbackSeed = state.fallbackSeed,
                )
            }
        }

        // Remove states belonging to players who left the level.
        circleStates.keys.removeIf { uuid -> level.getPlayerByUUID(uuid) == null }

        // Mishap 3D shatter rings.
        synchronized(shatters) {
            val iter = shatters.iterator()
            while (iter.hasNext()) {
                val burst = iter.next()
                val elapsed = now - burst.startNanos
                if (elapsed < 0L || elapsed >= SHATTER_DURATION_NANOS) {
                    iter.remove()
                    continue
                }
                val progress = elapsed.toFloat() / SHATTER_DURATION_NANOS
                drawShatterRing(
                    mat,
                    quadConsumer,
                    burst.origin,
                    progress,
                    burst.color,
                    burst.patternCount,
                    config.expandingMagicCircle,
                )
            }
        }

        // Staff-tip pigment ribbon trails.
        val camLook = Vec3(camera.lookVector.x.toDouble(), camera.lookVector.y.toDouble(), camera.lookVector.z.toDouble())
        for ((key, deque) in trails) {
            if (deque.size < 2) continue
            val player = level.getPlayerByUUID(key.uuid) as? AbstractClientPlayer ?: continue
            val isLocalFirst = isFirstPersonCam && localPlayer?.id == player.id
            val resonance = CastAnimationClient.resonanceOf(player, partialTick)
            val head = deque.first()
            val rgb = StaffTipParticles.pigmentColor(player, head.pos, partialTick)
                ?: Vector3f(0.75f, 0.55f, 1.0f)
            drawTrailRibbon(mat, quadConsumer, deque, camLook, now, rgb, resonance, isLocalFirst)
        }

        poseStack.popPose()
        if (bufferSource is MultiBufferSource.BufferSource) {
            bufferSource.endBatch(RenderType.lightning())
        }
    }

    private fun drawMagicCircle(
        mat: Matrix4f,
        consumer: VertexConsumer,
        origin: Vec3,
        radius: Float,
        spin: Float,
        rgb: Vector3f,
        alpha: Float,
        resonance: Float,
        minimalFirstPerson: Boolean,
        showPatterns: Boolean,
        snapshot: PatternSnapshot?,
        fallbackSeed: Int,
    ) {
        val ox = origin.x.toFloat()
        val oy = origin.y.toFloat()
        val oz = origin.z.toFloat()
        val layers = ArcaneGeometryMath.layerCount(radius)
        val strokeW = ArcaneGeometryMath.ringHalfWidth(radius)

        // Layer 1: Inner core ring & hexagon.
        val coreRadius = if (layers > 1) minOf(radius * 0.34f, 1.45f) else radius
        val coreW = ArcaneGeometryMath.ringHalfWidth(coreRadius)
        drawPolygon(mat, consumer, ox, oy, oz, coreRadius, 24, spin, coreW, rgb.x, rgb.y, rgb.z, alpha)
        if (!minimalFirstPerson) {
            drawPolygon(mat, consumer, ox, oy + 0.008f, oz, coreRadius * 0.74f, 6, -spin * 1.4f, coreW * 0.85f, rgb.x, rgb.y, rgb.z, alpha * 0.85f)
        }

        // Layer 2 (1.65+ blocks): Mid-inner counter-rotating ring & hexagram star.
        if (layers >= 2) {
            val r2 = if (layers == 2) radius else radius * 0.52f
            val a2 = alpha * ((radius - 1.55f) / 0.6f).coerceIn(0.25f, 0.95f)
            val w2 = strokeW * 0.90f
            drawPolygon(mat, consumer, ox, oy + 0.008f, oz, r2, 32, -spin * 0.8f, w2, rgb.x, rgb.y, rgb.z, a2)
            if (!minimalFirstPerson) {
                drawPolygon(mat, consumer, ox, oy + 0.012f, oz, r2 * 0.90f, 6, spin * 0.9f, w2 * 0.75f, rgb.x, rgb.y, rgb.z, a2 * 0.55f)
                drawPolygon(mat, consumer, ox, oy + 0.012f, oz, r2 * 0.90f, 6, spin * 0.9f + (PI / 6.0).toFloat(), w2 * 0.75f, rgb.x, rgb.y, rgb.z, a2 * 0.55f)
            }
        }

        // Layer 3 (3.60+ blocks): Mid-outer twin ring & radial lattice ticks.
        if (layers >= 3) {
            val r3 = if (layers == 3) radius else radius * 0.76f
            val a3 = alpha * ((radius - 3.45f) / 0.9f).coerceIn(0.25f, 0.92f)
            drawPolygon(mat, consumer, ox, oy + 0.010f, oz, r3, 40, spin * 0.55f, strokeW, rgb.x, rgb.y, rgb.z, a3)
            drawPolygon(mat, consumer, ox, oy + 0.010f, oz, r3 * 0.95f, 40, -spin * 0.45f, strokeW * 0.72f, rgb.x, rgb.y, rgb.z, a3 * 0.65f)
            drawRadialTicks(mat, consumer, ox, oy + 0.012f, oz, r3 * 0.95f, r3, 12, spin * 0.55f, strokeW * 0.80f, rgb, a3 * 0.75f)
        }

        // Layer 4 (6.40+ to 12.0 blocks): Grand outer boundary ring & dodecagram.
        if (layers >= 4) {
            val a4 = alpha * ((radius - 6.20f) / 1.2f).coerceIn(0.25f, 0.95f)
            drawPolygon(mat, consumer, ox, oy + 0.015f, oz, radius, 48, -spin * 0.35f, strokeW * 1.15f, rgb.x, rgb.y, rgb.z, a4)
            drawPolygon(mat, consumer, ox, oy + 0.015f, oz, radius * 0.96f, 48, spin * 0.30f, strokeW * 0.80f, rgb.x, rgb.y, rgb.z, a4 * 0.70f)
            drawRadialTicks(mat, consumer, ox, oy + 0.016f, oz, radius * 0.91f, radius, 24, -spin * 0.35f, strokeW * 0.90f, rgb, a4 * 0.80f)
        }

        if (!showPatterns) return

        // Center Hex pattern inside Layer 1.
        val centerGlyph = snapshot?.centerPattern ?: if (!minimalFirstPerson) {
            HexPatternGeometry.fallbackGlyph(fallbackSeed)
        } else {
            null
        }
        if (centerGlyph != null) {
            val centerScale = coreRadius * 0.48f
            drawNormGlyph(
                mat, consumer,
                ox, oy + 0.020f, oz,
                scale = centerScale,
                angle = if (minimalFirstPerson) spin * 0.4f else -spin * 0.6f,
                halfWidth = ArcaneGeometryMath.glyphHalfWidth(centerScale),
                points = centerGlyph,
                r = rgb.x, g = rgb.y, b = rgb.z,
                a = (alpha * (if (minimalFirstPerson) 1.3f else 1.15f)).coerceAtMost(if (minimalFirstPerson) 0.55f else 0.95f),
            )
        }

        if (minimalFirstPerson) return

        // Outer ring Hex pattern slots anchored onto the expanding outer layer.
        val ringGlyphs = when {
            snapshot != null && snapshot.ringPatterns.isNotEmpty() -> snapshot.ringPatterns
            resonance >= 0.25f -> List(if (resonance >= 0.65f) 6 else 3) { idx ->
                HexPatternGeometry.fallbackGlyph(fallbackSeed + idx * 17)
            }
            else -> emptyList()
        }
        if (ringGlyphs.isNotEmpty()) {
            val patternAnchorRadius = when (layers) {
                1 -> coreRadius * 0.88f
                2 -> radius * 0.72f
                else -> radius * 0.84f
            }
            val slotRadius = (patternAnchorRadius * 0.16f).coerceIn(0.16f, 0.88f)
            drawPatternRing(
                mat, consumer,
                ox, oy + 0.022f, oz,
                ringRadius = patternAnchorRadius,
                slotRadius = slotRadius,
                spin = spin * 0.55f,
                glyphs = ringGlyphs,
                rgb = rgb,
                alpha = alpha,
            )
        }
    }

    private fun drawPatternRing(
        mat: Matrix4f,
        consumer: VertexConsumer,
        ox: Float,
        oy: Float,
        oz: Float,
        ringRadius: Float,
        slotRadius: Float,
        spin: Float,
        glyphs: List<List<HexPatternGeometry.NormPoint>>,
        rgb: Vector3f,
        alpha: Float,
    ) {
        val slotCount = glyphs.size
        val glyphW = ArcaneGeometryMath.glyphHalfWidth(slotRadius)
        val frameW = glyphW * 0.78f
        for ((index, glyph) in glyphs.withIndex()) {
            val theta = spin + index * (2.0 * PI / slotCount).toFloat()
            val sx = ox + cos(theta) * ringRadius
            val sz = oz + sin(theta) * ringRadius
            drawPolygon(
                mat, consumer,
                sx, oy, sz,
                slotRadius * 1.28f, 6, theta, frameW,
                rgb.x, rgb.y, rgb.z, alpha * 0.58f,
            )
            drawNormGlyph(
                mat, consumer,
                sx, oy + 0.006f, sz,
                scale = slotRadius,
                angle = theta,
                halfWidth = glyphW,
                points = glyph,
                r = rgb.x, g = rgb.y, b = rgb.z, a = (alpha * 1.12f).coerceAtMost(0.95f),
            )
        }
    }

    private fun drawRadialTicks(
        mat: Matrix4f,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        innerR: Float,
        outerR: Float,
        count: Int,
        angleOffset: Float,
        halfWidth: Float,
        rgb: Vector3f,
        alpha: Float,
    ) {
        val step = (2.0 * PI / count).toFloat()
        for (i in 0 until count) {
            val a = angleOffset + i * step
            val ca = cos(a)
            val sa = sin(a)
            emitGroundStroke(
                mat, consumer,
                cx + ca * innerR, cy, cz + sa * innerR,
                cx + ca * outerR, cy, cz + sa * outerR,
                halfWidth,
                rgb.x, rgb.y, rgb.z, alpha,
            )
        }
    }

    private fun drawNormGlyph(
        mat: Matrix4f,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        scale: Float,
        angle: Float,
        halfWidth: Float,
        points: List<HexPatternGeometry.NormPoint>,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        if (points.size < 2) return
        val cosA = cos(angle)
        val sinA = sin(angle)
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val x1 = cx + (p1.x * cosA - p1.y * sinA) * scale
            val z1 = cz + (p1.x * sinA + p1.y * cosA) * scale
            val x2 = cx + (p2.x * cosA - p2.y * sinA) * scale
            val z2 = cz + (p2.x * sinA + p2.y * cosA) * scale
            emitGroundStroke(mat, consumer, x1, cy, z1, x2, cy, z2, halfWidth, r, g, b, a)
            if (i > 0) {
                emitJointCap(mat, consumer, x1, cy, z1, halfWidth * 1.15f, r, g, b, a * 0.85f)
            }
        }
    }

    private fun drawShatterRing(
        mat: Matrix4f,
        consumer: VertexConsumer,
        origin: Vec3,
        progress: Float,
        rgb: Vector3f,
        patternCount: Int,
        expandingEnabled: Boolean,
    ) {
        val radius = ArcaneGeometryMath.circleRadius(1f, 0.6f, patternCount, expandingEnabled, progress)
        val alpha = ((1f - progress) * 0.88f).coerceIn(0f, 1f)
        val strokeW = ArcaneGeometryMath.ringHalfWidth(radius) * (1.2f - 0.4f * progress)
        val ox = origin.x.toFloat()
        val oy = origin.y.toFloat() + progress * 0.25f
        val oz = origin.z.toFloat()
        val segments = 8
        for (i in 0 until segments) {
            val baseAngle = i * (2.0 * PI / segments) + progress * 0.75
            val spread = 0.24
            val lift = sin(i * 1.7 + progress * 4.0) * 0.16 * progress
            val x1 = ox + cos(baseAngle - spread).toFloat() * radius
            val z1 = oz + sin(baseAngle - spread).toFloat() * radius
            val x2 = ox + cos(baseAngle + spread).toFloat() * radius
            val z2 = oz + sin(baseAngle + spread).toFloat() * radius
            emitGroundStroke(
                mat, consumer,
                x1, oy + lift.toFloat(), z1,
                x2, oy - lift.toFloat(), z2,
                strokeW,
                rgb.x, rgb.y, rgb.z, alpha,
            )
        }
    }

    private fun drawPolygon(
        mat: Matrix4f,
        consumer: VertexConsumer,
        cx: Float,
        cy: Float,
        cz: Float,
        radius: Float,
        sides: Int,
        angleOffset: Float,
        halfWidth: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        val step = (2.0 * PI / sides).toFloat()
        for (i in 0 until sides) {
            val a1 = angleOffset + i * step
            val a2 = angleOffset + (i + 1) * step
            val x1 = cx + cos(a1) * radius
            val z1 = cz + sin(a1) * radius
            val x2 = cx + cos(a2) * radius
            val z2 = cz + sin(a2) * radius
            emitGroundStroke(mat, consumer, x1, cy, z1, x2, cy, z2, halfWidth, r, g, b, a)
        }
    }

    /** Emits a thick horizontal quad stroke in the XZ plane so width is independent of GL line width. */
    private fun emitGroundStroke(
        mat: Matrix4f,
        consumer: VertexConsumer,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float,
        halfWidth: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        val dx = x2 - x1
        val dz = z2 - z1
        val len = sqrt(dx * dx + dz * dz)
        if (len < 1.0e-4f) return
        val px = (-dz / len) * halfWidth
        val pz = (dx / len) * halfWidth

        consumer.vertex(mat, x1 + px, y1, z1 + pz).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x2 + px, y2, z2 + pz).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x2 - px, y2, z2 - pz).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x1 - px, y1, z1 - pz).color(r, g, b, a).endVertex()
    }

    /** Small diamond joint cap at polyline corners so sharp 60-degree bends connect seamlessly. */
    private fun emitJointCap(
        mat: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        radius: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        consumer.vertex(mat, x + radius, y, z).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x, y, z + radius).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x - radius, y, z).color(r, g, b, a).endVertex()
        consumer.vertex(mat, x, y, z - radius).color(r, g, b, a).endVertex()
    }

    private fun drawTrailRibbon(
        mat: Matrix4f,
        consumer: VertexConsumer,
        samples: ArrayDeque<TrailSample>,
        camLook: Vec3,
        now: Long,
        rgb: Vector3f,
        resonance: Float,
        isLocalFirstPerson: Boolean,
    ) {
        val list = samples.toList()
        for (i in 0 until list.size - 1) {
            val s0 = list[i]
            val s1 = list[i + 1]
            val f0 = i.toFloat() / (list.size - 1)
            val f1 = (i + 1).toFloat() / (list.size - 1)
            val age0 = ((now - s0.nanos).toFloat() / MAX_TRAIL_AGE_NANOS).coerceIn(0f, 1f)
            val age1 = ((now - s1.nanos).toFloat() / MAX_TRAIL_AGE_NANOS).coerceIn(0f, 1f)
            val a0 = ArcaneGeometryMath.trailAlpha(f0, age0, isLocalFirstPerson)
            val a1 = ArcaneGeometryMath.trailAlpha(f1, age1, isLocalFirstPerson)
            if (a0 <= 0.01f && a1 <= 0.01f) continue

            val dir = s1.pos.subtract(s0.pos)
            if (dir.lengthSqr() < 1.0e-6) continue
            var side = dir.cross(camLook)
            if (side.lengthSqr() < 1.0e-6) side = Vec3(0.0, 1.0, 0.0)
            side = side.normalize()

            val w0 = ArcaneGeometryMath.trailWidth(f0, resonance).toDouble()
            val w1 = ArcaneGeometryMath.trailWidth(f1, resonance).toDouble()

            val p0a = s0.pos.add(side.scale(w0))
            val p0b = s0.pos.subtract(side.scale(w0))
            val p1a = s1.pos.add(side.scale(w1))
            val p1b = s1.pos.subtract(side.scale(w1))

            consumer.vertex(mat, p0a.x.toFloat(), p0a.y.toFloat(), p0a.z.toFloat()).color(rgb.x, rgb.y, rgb.z, a0).endVertex()
            consumer.vertex(mat, p1a.x.toFloat(), p1a.y.toFloat(), p1a.z.toFloat()).color(rgb.x, rgb.y, rgb.z, a1).endVertex()
            consumer.vertex(mat, p1b.x.toFloat(), p1b.y.toFloat(), p1b.z.toFloat()).color(rgb.x, rgb.y, rgb.z, a1).endVertex()
            consumer.vertex(mat, p0b.x.toFloat(), p0b.y.toFloat(), p0b.z.toFloat()).color(rgb.x, rgb.y, rgb.z, a0).endVertex()
        }
    }
}
