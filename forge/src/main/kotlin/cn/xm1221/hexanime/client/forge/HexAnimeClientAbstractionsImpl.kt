@file:JvmName("HexAnimeClientAbstractionsImpl")

package cn.xm1221.hexanime.client.forge

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.common.MinecraftForge

fun registerWorldRender(callback: (PoseStack, MultiBufferSource, Camera, Float) -> Unit) {
    MinecraftForge.EVENT_BUS.addListener { event: RenderLevelStageEvent ->
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return@addListener
        val buffers = Minecraft.getInstance().renderBuffers().bufferSource()
        callback(event.poseStack, buffers, event.camera, event.partialTick)
    }
}
