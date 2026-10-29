@file:JvmName("HexAnimeAbstractionsImpl")

package cn.xm1221.hexanime.forge

import cn.xm1221.hexanime.registry.HexAnimeRegistrar
import net.minecraftforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun <T : Any> initRegistry(registrar: HexAnimeRegistrar<T>) {
    MOD_BUS.addListener { event: RegisterEvent ->
        event.register(registrar.registryKey) { helper ->
            registrar.init(helper::register)
        }
    }
}
