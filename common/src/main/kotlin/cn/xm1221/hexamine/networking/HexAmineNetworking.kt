package cn.xm1221.hexamine.networking

import dev.architectury.networking.NetworkChannel
import cn.xm1221.hexamine.HexAmine
import cn.xm1221.hexamine.networking.msg.HexAmineMessageCompanion

object HexAmineNetworking {
    val CHANNEL: NetworkChannel = NetworkChannel.create(HexAmine.id("networking_channel"))

    fun init() {
        for (subclass in HexAmineMessageCompanion::class.sealedSubclasses) {
            subclass.objectInstance?.register(CHANNEL)
        }
    }
}
