package cn.xm1221.hexanime.networking.msg

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionHand

/** Validated staff aim for one player, sent to clients that are tracking [entityId]. */
data class MsgStaffAimS2C(
    val entityId: Int,
    val active: Boolean,
    val normX: Float,
    val normY: Float,
    val hand: InteractionHand,
    val profileId: String,
    val variant: Int,
    val strain: Float,
) : HexAnimeMessageS2C {
    companion object : HexAnimeMessageCompanion<MsgStaffAimS2C> {
        override val type = MsgStaffAimS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgStaffAimS2C(
            entityId = buf.readVarInt(),
            active = buf.readBoolean(),
            normX = buf.readFloat(),
            normY = buf.readFloat(),
            hand = buf.readEnum(InteractionHand::class.java),
            profileId = buf.readUtf(128),
            variant = buf.readVarInt(),
            strain = buf.readFloat(),
        )

        override fun MsgStaffAimS2C.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(entityId)
            buf.writeBoolean(active)
            buf.writeFloat(normX)
            buf.writeFloat(normY)
            buf.writeEnum(hand)
            buf.writeUtf(profileId, 128)
            buf.writeVarInt(variant)
            buf.writeFloat(strain)
        }
    }
}
