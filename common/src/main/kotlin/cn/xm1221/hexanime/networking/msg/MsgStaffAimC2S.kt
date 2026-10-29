package cn.xm1221.hexanime.networking.msg

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionHand

/**
 * Local cursor sample while Hex's spell GUI is open.
 * [normX] and [normY] are relative to the GUI center: +x is screen-right, +y is screen-down, edges at ±1.
 */
data class MsgStaffAimC2S(
    val active: Boolean,
    val normX: Float,
    val normY: Float,
    val hand: InteractionHand,
) : HexAnimeMessageC2S {
    companion object : HexAnimeMessageCompanion<MsgStaffAimC2S> {
        override val type = MsgStaffAimC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgStaffAimC2S(
            active = buf.readBoolean(),
            normX = buf.readFloat(),
            normY = buf.readFloat(),
            hand = buf.readEnum(InteractionHand::class.java),
        )

        override fun MsgStaffAimC2S.encode(buf: FriendlyByteBuf) {
            buf.writeBoolean(active)
            buf.writeFloat(normX)
            buf.writeFloat(normY)
            buf.writeEnum(hand)
        }
    }
}
