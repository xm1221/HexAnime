package cn.xm1221.hexanime.networking.msg

import net.minecraft.network.FriendlyByteBuf

data class MsgAnimationPreferencesC2S(
    val receiveStaffAim: Boolean,
    val receiveCastEvents: Boolean,
) : HexAnimeMessageC2S {
    companion object : HexAnimeMessageCompanion<MsgAnimationPreferencesC2S> {
        override val type = MsgAnimationPreferencesC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgAnimationPreferencesC2S(
            receiveStaffAim = buf.readBoolean(),
            receiveCastEvents = buf.readBoolean(),
        )

        override fun MsgAnimationPreferencesC2S.encode(buf: FriendlyByteBuf) {
            buf.writeBoolean(receiveStaffAim)
            buf.writeBoolean(receiveCastEvents)
        }
    }
}
