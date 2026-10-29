@file:JvmName("HexAmineAbstractionsImpl")

package cn.xm1221.hexamine.forge

import cn.xm1221.hexamine.registry.HexAmineRegistrar
import net.minecraftforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun <T : Any> initRegistry(registrar: HexAmineRegistrar<T>) {
    MOD_BUS.addListener { event: RegisterEvent ->
        event.register(registrar.registryKey) { helper ->
            registrar.init(helper::register)
        }
    }
}
