package cn.xm1221.hexanime.client

import at.petrak.hexcasting.api.HexAPI
import cn.xm1221.hexanime.client.anim.CastHandFrame
import cn.xm1221.hexanime.client.anim.StaffDisplay
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import cn.xm1221.hexanime.staff.Staffs
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.util.FastColor
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * Restrained pigment-colored staff-tip motes while overcast strain is elevated.
 * Avoids noisy vanilla particle types to keep the geometric Hex aesthetic clean.
 */
object StaffTipParticles {
    fun tick(minecraft: Minecraft) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.staffTipParticles) return
        val level = minecraft.level ?: return
        for (player in level.players()) {
            val animation = CastAnimationClient.animationOf(player)
            if (animation?.isActive() != true) continue
            for (hand in InteractionHand.values()) {
                val frame = CastAnimationClient.frame(player, hand, 1f)
                val profile = frame.profile ?: continue
                if (!profile.animatesAsStaff || frame.strain <= StaffDisplay.PARTICLE_STRAIN) {
                    continue
                }
                spawnStrainSparks(player, hand, frame)
            }
        }
    }

    fun handBasePosition(player: AbstractClientPlayer, hand: InteractionHand, partialTick: Float): Vec3 {
        val yawRad = Math.toRadians(-player.getViewYRot(partialTick).toDouble())
        val forward = Vec3(sin(yawRad), 0.0, cos(yawRad)).normalize()
        val right = Vec3(-forward.z, 0.0, forward.x)
        val side = if (Staffs.armBone(player, hand) == Staffs.RIGHT_ARM) 1.0 else -1.0
        val eye = player.getEyePosition(partialTick)
        return eye.add(forward.scale(0.38)).add(right.scale(0.32 * side)).add(0.0, -0.32, 0.0)
    }

    fun tipPosition(
        player: AbstractClientPlayer,
        hand: InteractionHand,
        frame: CastHandFrame,
        partialTick: Float,
    ): Vec3 {
        val look = player.getViewVector(partialTick)
        val side = if (Staffs.armBone(player, hand) == Staffs.RIGHT_ARM) 1.0 else -1.0
        var right = look.cross(Vec3(0.0, 1.0, 0.0))
        if (right.lengthSqr() < 1.0e-4) right = Vec3(1.0, 0.0, 0.0)
        right = right.normalize()
        val up = right.cross(look).normalize()

        val swayRight = (-frame.aimYaw - frame.armYawDelta * 0.45f) * 0.95
        val swayUp = (-frame.aimPitch - frame.armPitchDelta * 0.45f) * 0.85
        return player.getEyePosition(partialTick)
            .add(look.scale(0.88))
            .add(right.scale(0.30 * side + swayRight))
            .add(up.scale(-0.04 + swayUp))
    }

    fun pigmentColor(player: AbstractClientPlayer, pos: Vec3, timeOffset: Float = 0f): Vector3f? {
        val color = try {
            HexAPI.instance().getColorizer(player).colorProvider.getColor(
                player.tickCount.toFloat() + timeOffset,
                pos,
            )
        } catch (_: RuntimeException) {
            return null
        }
        return Vector3f(
            FastColor.ARGB32.red(color) / 255f,
            FastColor.ARGB32.green(color) / 255f,
            FastColor.ARGB32.blue(color) / 255f,
        )
    }

    private fun spawnStrainSparks(player: AbstractClientPlayer, hand: InteractionHand, frame: CastHandFrame) {
        val level = player.level()
        val tip = tipPosition(player, hand, frame, 1f)
        val rgb = pigmentColor(player, tip) ?: return
        val dust = DustParticleOptions(rgb, 0.65f)
        val jitter = player.random
        level.addParticle(
            dust,
            tip.x + (jitter.nextDouble() - 0.5) * 0.06,
            tip.y + (jitter.nextDouble() - 0.5) * 0.06,
            tip.z + (jitter.nextDouble() - 0.5) * 0.06,
            0.0,
            0.012,
            0.0,
        )
        // Sparse pigment flicker between hand and tip when strain is severe.
        if (HexAnimeClientConfig.config.materialVfxAndAudio && frame.strain > 0.82f && player.tickCount % 2 == 0) {
            val base = handBasePosition(player, hand, 1f)
            val p = base.lerp(tip, 0.35 + jitter.nextDouble() * 0.45)
            level.addParticle(
                DustParticleOptions(rgb, 0.50f),
                p.x + (jitter.nextDouble() - 0.5) * 0.09,
                p.y + (jitter.nextDouble() - 0.5) * 0.09,
                p.z + (jitter.nextDouble() - 0.5) * 0.09,
                0.0,
                0.0,
                0.0,
            )
        }
    }
}
