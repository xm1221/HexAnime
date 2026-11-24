@file:JvmName("HexAnimeClientAbstractionsImpl")

package cn.xm1221.hexanime.client.fabric

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource

fun registerWorldRender(callback: (PoseStack, MultiBufferSource, Camera, Float) -> Unit) {
    WorldRenderEvents.AFTER_ENTITIES.register { context ->
        val buffers = context.consumers() ?: Minecraft.getInstance().renderBuffers().bufferSource()
        callback(context.matrixStack(), buffers, context.camera(), context.tickDelta())
    }
}
