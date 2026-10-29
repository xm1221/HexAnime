@file:JvmName("HexAnimeAbstractions")

package cn.xm1221.hexanime

import dev.architectury.injectables.annotations.ExpectPlatform
import cn.xm1221.hexanime.registry.HexAnimeRegistrar

fun initRegistries(vararg registries: HexAnimeRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
@Suppress("UNUSED_PARAMETER")
fun <T : Any> initRegistry(registrar: HexAnimeRegistrar<T>) {
    throw AssertionError()
}
