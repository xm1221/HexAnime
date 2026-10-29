package cn.xm1221.hexanime.casting.item

import at.petrak.hexcasting.api.item.MediaHolderItem
import at.petrak.hexcasting.api.item.VariantItem
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import net.minecraft.world.item.ItemStack
import kotlin.math.max

object CastItems {
    fun snapshot(stack: ItemStack): CastItemState? {
        if (stack.isEmpty) return null
        val profile = CastItemProfiles.resolve(stack) ?: return null
        val variant = (stack.item as? VariantItem)?.getVariant(stack)?.coerceAtLeast(0) ?: 0
        return CastItemState(
            profile = profile,
            variant = variant,
            strain = strainOf(stack),
            packagedHex = stack.item is ItemPackagedHex,
            damageValue = if (stack.isDamageableItem) stack.damageValue else 0,
            mediaFullness = mediaFullnessOf(stack),
        )
    }

    /**
     * A single 0..1 feedback value shared by visuals. Media depletion and vanilla durability are
     * both useful "damage" signals, so take the stronger one instead of adding them together.
     */
    fun strainOf(stack: ItemStack): Float {
        if (stack.isEmpty) return 0f
        var strain = 0f
        if (stack.isDamageableItem) {
            val maxDamage = stack.maxDamage
            if (maxDamage > 0) {
                strain = stack.damageValue.toFloat() / maxDamage
            }
        }
        val mediaFullness = mediaFullnessOf(stack)
        if (mediaFullness >= 0f) {
            strain = max(strain, 1f - mediaFullness)
        }
        return strain.coerceIn(0f, 1f)
    }

    /** Returns -1 for items that do not hold Hex media. */
    fun mediaFullnessOf(stack: ItemStack): Float {
        val mediaHolder = stack.item as? MediaHolderItem ?: return -1f
        val maxMedia = mediaHolder.getMaxMedia(stack)
        if (maxMedia <= 0) return -1f
        return mediaHolder.getMediaFullness(stack).coerceIn(0f, 1f)
    }
}
