@file:JvmName("HexAnimeAbstractionsImpl")

package cn.xm1221.hexanime.fabric

import cn.xm1221.hexanime.registry.HexAnimeRegistrar
import net.minecraft.core.Registry

fun <T : Any> initRegistry(registrar: HexAnimeRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
