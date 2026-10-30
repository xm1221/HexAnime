package cn.xm1221.hexanime.client.anim

import cn.xm1221.hexanime.staff.Staffs
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player

/**
 * Offhand pose while the other hand is working a staff.
 * Empty hand seals. A Patchouli book or a Hex lore fragment is lifted to be read.
 */
object OffhandRitual {
    enum class Kind { EMPTY, PATCHOULI, LORE, OTHER }

    data class Pose(val yaw: Float, val pitch: Float, val roll: Float)

    fun classify(empty: Boolean, namespace: String, path: String): Kind = when {
        empty -> Kind.EMPTY
        namespace == "patchouli" -> Kind.PATCHOULI
        namespace == "hexcasting" && path == "lore_fragment" -> Kind.LORE
        else -> Kind.OTHER
    }

    fun pose(kind: Kind): Pose? = when (kind) {
        Kind.EMPTY -> Pose(yaw = 0.34f, pitch = -0.58f, roll = 0.22f)
        Kind.PATCHOULI, Kind.LORE -> Pose(yaw = 0.16f, pitch = -1.05f, roll = -0.08f)
        Kind.OTHER -> null
    }

    fun poseFor(player: Player, castingHand: InteractionHand): Pose? {
        val offhand = castingHand.other()
        val stack = player.getItemInHand(offhand)
        val id = if (stack.isEmpty) null else BuiltInRegistries.ITEM.getKey(stack.item)
        val kind = classify(stack.isEmpty, id?.namespace.orEmpty(), id?.path.orEmpty())
        val base = pose(kind) ?: return null
        val inward = if (Staffs.armBone(player, offhand) == Staffs.RIGHT_ARM) -1f else 1f
        return base.copy(yaw = base.yaw * inward)
    }

    private fun InteractionHand.other(): InteractionHand =
        if (this == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
}
