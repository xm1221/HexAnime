package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.casting.item.CastItems
import cn.xm1221.hexanime.staff.Staffs
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import kotlin.math.sin

/**
 * Offhand pose while the other hand is working a staff.
 * Supports reading Patchouli/Lore, channeling Hex media items, open-palm energy
 * gathering when empty, and cross-guard dual-wield synergy.
 */
object OffhandRitual {
    enum class Kind { EMPTY, PATCHOULI, LORE, HEX_MEDIA, DUAL_CAST_ITEM, OTHER }

    data class Pose(val yaw: Float, val pitch: Float, val roll: Float)

    private val hexMediaPaths = setOf(
        "focus",
        "spellbook",
        "cypher",
        "ancient_cypher",
        "trinket",
        "artifact",
        "abacus",
        "lens",
        "amethyst_dust",
        "charged_amethyst",
    )

    fun classify(empty: Boolean, namespace: String, path: String): Kind = when {
        empty -> Kind.EMPTY
        namespace == "patchouli" -> Kind.PATCHOULI
        namespace == "hexcasting" && path == "lore_fragment" -> Kind.LORE
        namespace == "hexcasting" && path in hexMediaPaths -> Kind.HEX_MEDIA
        namespace == "minecraft" && path == "amethyst_shard" -> Kind.HEX_MEDIA
        else -> Kind.OTHER
    }

    fun pose(
        kind: Kind,
        resonance: Float = 0f,
        offhandEnergyEnabled: Boolean = true,
        phase: Float = 0f,
    ): Pose? {
        val r = if (resonance.isFinite()) resonance.coerceIn(0f, 1f) else 0f
        val wave = if (phase.isFinite()) sin((phase * 0.16f).toDouble()).toFloat() else 0f
        return when (kind) {
            Kind.EMPTY -> {
                if (!offhandEnergyEnabled || r < 0.20f) return null
                Pose(
                    yaw = 0.26f + 0.06f * r,
                    pitch = -0.82f - 0.16f * r + wave * 0.04f,
                    roll = -0.22f - 0.06f * r,
                )
            }
            Kind.PATCHOULI, Kind.LORE -> Pose(
                yaw = 0.16f,
                pitch = -1.05f + wave * 0.025f * r,
                roll = -0.08f,
            )
            Kind.HEX_MEDIA -> {
                if (!offhandEnergyEnabled) return null
                Pose(
                    yaw = 0.22f + 0.05f * r,
                    pitch = -0.94f - 0.10f * r + wave * 0.035f,
                    roll = -0.14f,
                )
            }
            Kind.DUAL_CAST_ITEM -> Pose(
                yaw = 0.28f + 0.05f * r,
                pitch = -0.78f - 0.10f * r + wave * 0.03f,
                roll = -0.22f,
            )
            Kind.OTHER -> null
        }
    }

    fun poseFor(
        player: Player,
        castingHand: InteractionHand,
        resonance: Float = 0f,
        offhandEnergyEnabled: Boolean = true,
        phase: Float = 0f,
        dualWieldEnabled: Boolean = false,
    ): Pose? {
        val offhand = castingHand.other()
        val stack = player.getItemInHand(offhand)
        val kind = if (dualWieldEnabled && !stack.isEmpty && CastItems.snapshot(stack) != null) {
            Kind.DUAL_CAST_ITEM
        } else {
            val id = if (stack.isEmpty) null else BuiltInRegistries.ITEM.getKey(stack.item)
            classify(stack.isEmpty, id?.namespace.orEmpty(), id?.path.orEmpty())
        }
        val base = pose(kind, resonance, offhandEnergyEnabled, phase) ?: return null
        val inward = if (Staffs.armBone(player, offhand) == Staffs.RIGHT_ARM) -1f else 1f
        return base.copy(yaw = base.yaw * inward, roll = base.roll * inward)
    }

    private fun InteractionHand.other(): InteractionHand =
        if (this == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
}
