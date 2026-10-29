@file:JvmName("HexAmineAbstractions")

package cn.xm1221.hexamine

import dev.architectury.injectables.annotations.ExpectPlatform
import cn.xm1221.hexamine.registry.HexAmineRegistrar

fun initRegistries(vararg registries: HexAmineRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
fun <T : Any> initRegistry(registrar: HexAmineRegistrar<T>) {
    throw AssertionError()
}
