package cn.xm1221.hexanime.casting.item

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * A complete full-body cast motion. Packaged Hex items select one of these by [variant], never by
 * item class, so all artifacts/cyphers/trinkets with the same visual variant move coherently.
 */
data class VariantMotion(
    val variant: Int,
    val durationTicks: Int,
    val anticipation: Float = 0.18f,
    val bodyYaw: Float = 0f,
    val bodyPitch: Float = 0f,
    val bodyRoll: Float = 0f,
    val bodyX: Float = 0f,
    val bodyY: Float = 0f,
    val bodyZ: Float = 0f,
    val headYaw: Float = 0f,
    val headPitch: Float = 0f,
    val headRoll: Float = 0f,
    val leftLegYaw: Float = 0f,
    val leftLegPitch: Float = 0f,
    val leftLegRoll: Float = 0f,
    val rightLegYaw: Float = 0f,
    val rightLegPitch: Float = 0f,
    val rightLegRoll: Float = 0f,
    val armYaw: Float = 0f,
    val armPitch: Float = 0f,
    val armRoll: Float = 0f,
    val itemYaw: Float = 0f,
    val itemPitch: Float = 0f,
    val itemRoll: Float = 0f,
    val itemX: Float = 0f,
    val itemY: Float = 0f,
    val itemZ: Float = 0f,
    val offhandYaw: Float = 0f,
    val offhandPitch: Float = 0f,
    val offhandRoll: Float = 0f,
    val firstPersonYaw: Float = 0f,
    val firstPersonPitch: Float = 0f,
    val firstPersonRoll: Float = 0f,
    val firstPersonX: Float = 0f,
    val firstPersonY: Float = 0f,
    val firstPersonZ: Float = 0f,
    val secondaryBeat: Float = 0f,
)

/** Continuous feedback for low media or item durability. */
data class DamagePose(
    val threshold: Float = 0.58f,
    val pitchDroop: Float = 0.12f,
    val tremorPitch: Float = 0.075f,
    val tremorYaw: Float = 0.06f,
    val tremorRoll: Float = 0.05f,
    val frequency: Float = 1.35f,
)

enum class CastPulseKind(val networkId: Int, val priority: Int) {
    DAMAGE(2, 0),
    CAST(0, 1),
    BREAK(1, 2),
    MISHAP(3, 3);

    companion object {
        fun fromNetwork(id: Int): CastPulseKind? = entries.firstOrNull { it.networkId == id }
    }
}

/**
 * A selectable animation contract for one item family. Packaged Hex variants are resolved by
 * [VariantMotion], while a profile only decides whether the item participates and how aim/damage
 * behave.
 */
class CastItemProfile(
    val id: ResourceLocation,
    val priority: Int,
    val supportsAim: Boolean,
    val supportsPulse: Boolean,
    val damage: DamagePose,
    private val matcher: (ItemStack) -> Boolean,
) {
    fun matches(stack: ItemStack): Boolean = matcher(stack)
}

data class CastItemState(
    val profile: CastItemProfile,
    val variant: Int,
    val strain: Float,
    val packagedHex: Boolean,
    val damageValue: Int,
    val mediaFullness: Float,
)
