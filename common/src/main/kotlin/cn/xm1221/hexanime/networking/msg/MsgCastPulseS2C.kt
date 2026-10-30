package cn.xm1221.hexanime.networking.msg

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

/** One-shot visual event after a packaged Hex is used or depleted. */
data class MsgCastPulseS2C(
    val entityId: Int,
    val hand: InteractionHand,
    val profileId: String,
    val variant: Int,
    val strain: Float,
    val kind: Int,
    val forceScreenEffect: Boolean,
    val displayStack: ItemStack,
) : HexAnimeMessageS2C {
    companion object : HexAnimeMessageCompanion<MsgCastPulseS2C> {
        override val type = MsgCastPulseS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgCastPulseS2C(
            entityId = buf.readVarInt(),
            hand = buf.readEnum(InteractionHand::class.java),
            profileId = buf.readUtf(128),
            variant = buf.readVarInt(),
            strain = buf.readFloat(),
            kind = buf.readVarInt(),
            forceScreenEffect = buf.readBoolean(),
            displayStack = buf.readItem(),
        )

        override fun MsgCastPulseS2C.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(entityId)
            buf.writeEnum(hand)
            buf.writeUtf(profileId, 128)
            buf.writeVarInt(variant)
            buf.writeFloat(strain)
            buf.writeVarInt(kind)
            buf.writeBoolean(forceScreenEffect)
            buf.writeItem(displayStack)
        }
    }
}
