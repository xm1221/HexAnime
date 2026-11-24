package cn.xm1221.hexanime.client.vfx

import cn.xm1221.hexanime.casting.MishapFlavor
import cn.xm1221.hexanime.casting.item.CastItemProfile
import cn.xm1221.hexanime.casting.item.CastPulseKind
import cn.xm1221.hexanime.casting.item.StaffMaterial
import cn.xm1221.hexanime.client.CastAnimationClient
import cn.xm1221.hexanime.client.StaffTipParticles
import cn.xm1221.hexanime.config.HexAnimeClientConfig
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

/**
 * Material-specific audio accents and clean pigment-only dust bursts on break/mishap.
 * Avoids mismatched vanilla particles so the 3D geometric circle and trails stay sharp.
 */
object MaterialFeedbackClient {
    fun onPulse(
        player: AbstractClientPlayer,
        hand: InteractionHand,
        profile: CastItemProfile,
        variant: Int,
        strain: Float,
        kind: CastPulseKind,
        resonance: Float,
    ) {
        val config = HexAnimeClientConfig.config
        if (!config.enabled || !config.materialVfxAndAudio) return

        val frame = CastAnimationClient.frame(player, hand, 1f)
        val tip = StaffTipParticles.tipPosition(player, hand, frame, 1f)

        when (kind) {
            CastPulseKind.CAST -> onCastAudio(player, profile, resonance)
            CastPulseKind.BREAK -> onBreakPulse(player, tip)
            CastPulseKind.MISHAP -> onMishapPulse(player, tip, MishapFlavor.fromVariant(variant))
            CastPulseKind.DAMAGE -> if (strain > 0.80f) {
                val rgb = StaffTipParticles.pigmentColor(player, tip) ?: return
                spawnPigmentDust(player, tip, rgb, count = 2, size = 0.55f, speed = 0.04)
            }
        }
    }

    private fun onCastAudio(
        player: AbstractClientPlayer,
        profile: CastItemProfile,
        resonance: Float,
    ) {
        val level = player.level()
        if (profile.animatesAsStaff) {
            when (profile.staffMaterial) {
                StaffMaterial.QUENCHED -> level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.24f,
                    1.25f + resonance * 0.18f,
                    false,
                )
                StaffMaterial.MINDSPLICE -> level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS,
                    0.20f,
                    0.72f + resonance * 0.12f,
                    false,
                )
                else -> level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.AMETHYST_BLOCK_STEP,
                    SoundSource.PLAYERS,
                    0.15f,
                    1.15f + resonance * 0.15f,
                    false,
                )
            }
        } else if (profile.pulseStyle.reentry) {
            level.playLocalSound(
                player.x, player.y, player.z,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.22f,
                1.05f + resonance * 0.15f,
                false,
            )
        }
    }

    private fun onBreakPulse(player: AbstractClientPlayer, tip: Vec3) {
        val level = player.level()
        val rgb = StaffTipParticles.pigmentColor(player, tip) ?: Vector3f(0.85f, 0.35f, 0.95f)
        spawnPigmentDust(player, tip, rgb, count = 7, size = 0.80f, speed = 0.10)
        level.playLocalSound(
            player.x, player.y, player.z,
            SoundEvents.AMETHYST_CLUSTER_BREAK,
            SoundSource.PLAYERS,
            0.30f,
            1.08f,
            false,
        )
    }

    private fun onMishapPulse(player: AbstractClientPlayer, tip: Vec3, flavor: MishapFlavor) {
        val level = player.level()
        val center = player.eyePosition.add(player.lookAngle.scale(0.55))
        when (flavor) {
            MishapFlavor.MEDIA -> {
                spawnPigmentDust(player, center, Vector3f(0.88f, 0.18f, 0.28f), count = 6, size = 0.85f, speed = 0.11)
                level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.PLAYERS,
                    0.34f,
                    0.72f,
                    false,
                )
            }
            MishapFlavor.SPATIAL -> {
                spawnPigmentDust(player, center, Vector3f(0.28f, 0.85f, 0.95f), count = 6, size = 0.85f, speed = 0.12)
                level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    0.22f,
                    1.28f,
                    false,
                )
            }
            MishapFlavor.MIND -> {
                spawnPigmentDust(player, center, Vector3f(0.62f, 0.24f, 0.92f), count = 6, size = 0.85f, speed = 0.10)
                level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS,
                    0.32f,
                    1.35f,
                    false,
                )
            }
            MishapFlavor.RECOIL -> {
                val rgb = StaffTipParticles.pigmentColor(player, tip) ?: Vector3f(0.9f, 0.4f, 0.5f)
                spawnPigmentDust(player, center, rgb, count = 6, size = 0.80f, speed = 0.10)
                level.playLocalSound(
                    player.x, player.y, player.z,
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS,
                    0.22f,
                    0.85f,
                    false,
                )
            }
        }
    }

    private fun spawnPigmentDust(
        player: AbstractClientPlayer,
        origin: Vec3,
        rgb: Vector3f,
        count: Int,
        size: Float,
        speed: Double,
    ) {
        val level = player.level()
        val rand = player.random
        val dust = DustParticleOptions(rgb, size)
        repeat(count) {
            val vx = (rand.nextDouble() - 0.5) * 2.0 * speed
            val vy = (rand.nextDouble() - 0.3) * 2.0 * speed
            val vz = (rand.nextDouble() - 0.5) * 2.0 * speed
            level.addParticle(dust, origin.x, origin.y, origin.z, vx, vy, vz)
        }
    }
}
