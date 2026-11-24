@file:JvmName("HexAnimeClientAbstractions")

package cn.xm1221.hexanime.client

import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource

@ExpectPlatform
@Suppress("UNUSED_PARAMETER")
fun registerWorldRender(callback: (PoseStack, MultiBufferSource, Camera, Float) -> Unit) {
    throw AssertionError()
}
