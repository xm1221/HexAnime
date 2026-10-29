package cn.xm1221.hexanime.client

import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.mixin.client.CameraAccessor
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import kotlin.math.PI
import kotlin.math.sin

object CameraShakeClient {
    private var startNanos = 0L
    private var durationNanos = 0L
    private var yawAmplitude = 0f
    private var pitchAmplitude = 0f

    fun trigger(kind: CastPulseKind) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.cameraShake) return
        when (kind) {
            CastPulseKind.MISHAP -> {
                durationNanos = 700_000_000L
                yawAmplitude = 4.8f
                pitchAmplitude = 3.4f
            }
            CastPulseKind.BREAK -> {
                durationNanos = 350_000_000L
                yawAmplitude = 1.8f
                pitchAmplitude = 1.2f
            }
            else -> return
        }
        startNanos = System.nanoTime()
    }

    @JvmStatic
    fun apply(camera: Camera) {
        if (durationNanos == 0L) return
        val player = Minecraft.getInstance().player ?: return
        if (camera.entity?.id != player.id) return
        if (!HexAnimeClientConfig.config.cameraShake) return

        val elapsed = System.nanoTime() - startNanos
        if (elapsed < 0L || elapsed >= durationNanos) {
            durationNanos = 0L
            return
        }
        val progress = elapsed.toFloat() / durationNanos
        val fade = (1f - progress) * (1f - progress)
        val yawOffset = sin(progress * PI * 9.0).toFloat() * yawAmplitude * fade
        val pitchOffset = sin(progress * PI * 12.0 + 1.3).toFloat() * pitchAmplitude * fade
        val accessor = camera as CameraAccessor
        accessor.`hexanime$setRotation`(
            camera.getYRot() + yawOffset,
            camera.getXRot() + pitchOffset,
        )
    }
}
