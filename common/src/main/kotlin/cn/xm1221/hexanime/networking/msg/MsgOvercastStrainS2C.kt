package cn.xm1221.hexanime.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** Temporary staff strain after a real overcast hit. Clients decay it locally. */
data class MsgOvercastStrainS2C(
    val entityId: Int,
    val strain: Float,
) : HexAnimeMessageS2C {
    companion object : HexAnimeMessageCompanion<MsgOvercastStrainS2C> {
        override val type = MsgOvercastStrainS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgOvercastStrainS2C(
            entityId = buf.readVarInt(),
            strain = buf.readFloat(),
        )

        override fun MsgOvercastStrainS2C.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(entityId)
            buf.writeFloat(strain)
        }
    }
}
