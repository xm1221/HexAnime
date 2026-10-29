package cn.xm1221.hexanime.staff

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.items.ItemStaff
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * What counts as a Hex Casting staff, and which playerAnimator bone should follow it.
 *
 * Kept free of client classes so the dedicated server can validate aims.
 * A 1.21.1 port should keep this check on the public staff tag plus [ItemStaff].
 */
object Staffs {
    /** Normalized cursor samples are clamped to the GUI, whose center is 0 and edges are ±1. */
    const val NORM_LIMIT = 1f

    const val RIGHT_ARM = "rightArm"
    const val LEFT_ARM = "leftArm"
    const val RIGHT_ITEM = "rightItem"
    const val LEFT_ITEM = "leftItem"

    fun isStaff(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.`is`(HexTags.Items.STAVES) || stack.item is ItemStaff
    }

    fun holdsStaff(player: Player, hand: InteractionHand): Boolean =
        isStaff(player.getItemInHand(hand))

    /**
     * Right arm for a right-handed main hand. Offhand uses the other arm.
     * Left-handed players swap, via [Player.getMainArm].
     */
    fun armBone(player: Player, hand: InteractionHand): String {
        val main = player.mainArm
        val arm = if (hand == InteractionHand.MAIN_HAND) main else main.opposite
        return if (arm == HumanoidArm.RIGHT) RIGHT_ARM else LEFT_ARM
    }

    fun itemBone(armBone: String): String =
        if (armBone == RIGHT_ARM) RIGHT_ITEM else LEFT_ITEM

    /** @return null when [value] is NaN or infinite. */
    fun clampNorm(value: Float): Float? {
        if (!value.isFinite()) return null
        return value.coerceIn(-NORM_LIMIT, NORM_LIMIT)
    }
}
