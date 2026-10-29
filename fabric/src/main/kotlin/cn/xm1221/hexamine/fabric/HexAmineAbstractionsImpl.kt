@file:JvmName("HexAmineAbstractionsImpl")

package cn.xm1221.hexamine.fabric

import cn.xm1221.hexamine.registry.HexAmineRegistrar
import net.minecraft.core.Registry

fun <T : Any> initRegistry(registrar: HexAmineRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
