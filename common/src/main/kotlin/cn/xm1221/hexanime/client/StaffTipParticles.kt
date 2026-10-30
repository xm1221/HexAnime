package cn.xm1221.hexanime.client

import at.petrak.hexcasting.api.HexAPI
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

/** Staff-tip dust in the caster's pigment while overcast strain is high. Client only. */
object StaffTipParticles {
    fun tick(minecraft: Minecraft) {
        if (!HexAnimeClientConfig.config.enabled || !HexAnimeClientConfig.config.staffTipParticles) return
        val level = minecraft.level ?: return
        for (player in level.players()) {
            val animation = CastAnimationClient.animationOf(player)
            if (animation?.isActive() != true) continue
            for (hand in InteractionHand.values()) {
                val frame = CastAnimationClient.frame(player, hand, 1f)
                if (frame.profile?.animatesAsStaff != true ||
                    frame.strain <= StaffDisplay.PARTICLE_STRAIN
                ) {
                    continue
                }
                spawn(player, hand)
            }
        }
    }

    private fun spawn(player: AbstractClientPlayer, hand: InteractionHand) {
        val level = player.level()
        val look = player.lookAngle
        val side = if (Staffs.armBone(player, hand) == Staffs.RIGHT_ARM) 1.0 else -1.0
        var right = look.cross(Vec3(0.0, 1.0, 0.0))
        if (right.lengthSqr() < 1.0e-4) right = Vec3(1.0, 0.0, 0.0)
        right = right.normalize()
        val tip = player.eyePosition.add(look.scale(0.85)).add(right.scale(0.28 * side))
        val color = try {
            HexAPI.instance().getColorizer(player).colorProvider.getColor(
                player.tickCount.toFloat(),
                tip,
            )
        } catch (_: RuntimeException) {
            return
        }
        val rgb = Vector3f(
            FastColor.ARGB32.red(color) / 255f,
            FastColor.ARGB32.green(color) / 255f,
            FastColor.ARGB32.blue(color) / 255f,
        )
        val dust = DustParticleOptions(rgb, 0.7f)
        repeat(2) {
            val jitter = player.random
            level.addParticle(
                dust,
                tip.x + (jitter.nextDouble() - 0.5) * 0.06,
                tip.y + (jitter.nextDouble() - 0.5) * 0.06,
                tip.z + (jitter.nextDouble() - 0.5) * 0.06,
                0.0,
                0.01,
                0.0,
            )
        }
    }
}
