package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.mixin.client.CameraAccessor
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import kotlin.math.PI
import kotlin.math.sin

object CameraShakeClient {
    private var startNanos = 0L
    private var durationNanos = 0L
    private var yawAmplitude = 0f
    private var pitchAmplitude = 0f
    private var rollAmplitude = 0f
    private var fovAmplitude = 0f
    private var yawCycles = 9.0
    private var pitchCycles = 12.0
    private var rollCycles = 1.5f
    private var pitchPhase = 1.3
    private var aimFovOffset = 0f

    fun trigger(kind: CastPulseKind) {
        trigger(kind, variant = 0, resonance = 0f)
    }

    fun trigger(kind: CastPulseKind, variant: Int, resonance: Float) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || (!config.cameraShake && !config.cameraRollAndFov)) return
        val boost = 1f + resonance.coerceIn(0f, 1f) * 0.35f
        when (kind) {
            CastPulseKind.MISHAP -> {
                durationNanos = 700_000_000L
                yawAmplitude = 4.8f
                pitchAmplitude = 3.4f
                rollAmplitude = if (variant % 2 == 0) 5.2f else -5.2f
                fovAmplitude = -4.2f
                yawCycles = 9.0
                pitchCycles = 12.0
                rollCycles = 2.5f
                pitchPhase = 1.3
            }
            CastPulseKind.BREAK -> {
                durationNanos = 350_000_000L
                yawAmplitude = 1.8f
                pitchAmplitude = 1.2f
                rollAmplitude = 3.2f
                fovAmplitude = 3.8f
                yawCycles = 7.0
                pitchCycles = 9.0
                rollCycles = 2.0f
                pitchPhase = 1.1
            }
            // Wood-staff / packaged cast hit: short rotational impulse + outward FOV kick.
            CastPulseKind.CAST -> {
                durationNanos = 240_000_000L
                yawAmplitude = 1.15f * boost
                pitchAmplitude = 0.70f * boost
                rollAmplitude = when (Math.floorMod(variant, 4)) {
                    3 -> 2.4f
                    1 -> -1.4f
                    2 -> 1.4f
                    else -> 1.0f
                } * boost
                fovAmplitude = 2.6f * boost
                yawCycles = 2.0
                pitchCycles = 2.0
                rollCycles = 1.5f
                pitchPhase = 0.4
            }
            else -> return
        }
        startNanos = System.nanoTime()
    }

    fun tickAim(minecraft: Minecraft) {
        val config = HexAnimeClientConfig.config
        val player = minecraft.player
        if (player == null || !config.enabled || !config.cameraRollAndFov) {
            aimFovOffset = Mth.lerp(0.25f, aimFovOffset, 0f)
            if (kotlin.math.abs(aimFovOffset) < 0.01f) aimFovOffset = 0f
            return
        }
        val anim = CastAnimationClient.animationOf(player)
        val aiming = anim?.isAiming() == true
        val resonance = anim?.maxResonance(1f) ?: 0f
        val target = if (aiming) -2.4f * resonance else 0f
        aimFovOffset = Mth.lerp(0.18f, aimFovOffset, target)
        if (!aiming && kotlin.math.abs(aimFovOffset) < 0.01f) aimFovOffset = 0f
    }

    @JvmStatic
    fun apply(camera: Camera) {
        if (durationNanos == 0L) return
        val player = Minecraft.getInstance().player ?: return
        if (camera.entity?.id != player.id) return
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.cameraShake) return

        val progress = activeProgress() ?: return
        val fade = CameraShakeCurves.attackFade(progress)
        val yawOffset = sin(progress * PI * yawCycles).toFloat() * yawAmplitude * fade
        val pitchOffset = sin(progress * PI * pitchCycles + pitchPhase).toFloat() * pitchAmplitude * fade
        val accessor = camera as CameraAccessor
        accessor.`hexanime$setRotation`(
            camera.getYRot() + yawOffset,
            camera.getXRot() + pitchOffset,
        )
    }

    @JvmStatic
    fun currentRoll(): Float {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.cameraRollAndFov) return 0f
        val progress = activeProgress() ?: return 0f
        return CameraShakeCurves.rollWave(progress, rollCycles) * rollAmplitude
    }

    @JvmStatic
    fun currentFovOffset(): Float {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.cameraRollAndFov) return 0f
        val burst = activeProgress()?.let { CameraShakeCurves.fovKickEnvelope(it) * fovAmplitude } ?: 0f
        return aimFovOffset + burst
    }

    private fun activeProgress(): Float? {
        if (durationNanos == 0L) return null
        val elapsed = System.nanoTime() - startNanos
        if (elapsed < 0L || elapsed >= durationNanos) {
            durationNanos = 0L
            return null
        }
        return elapsed.toFloat() / durationNanos
    }
}
