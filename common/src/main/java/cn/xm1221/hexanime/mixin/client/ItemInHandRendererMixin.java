package cn.xm1221.hexanime.mixin.client;

import cn.xm1221.hexanime.client.anim.StaffFirstPerson;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Unique
    private boolean hexanime$aimed;

    /**
     * Pin the outer {@code pushPose} in {@code renderArmWithItem}. Vanilla 1.20.1 has one push/pop
     * pair here; an unpinned injector would also match a later push and leave the stack unbalanced.
     */
    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            ordinal = 0,
            remap = false
        )
    )
    private void hexanime$beforeHandPush(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swing,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        hexanime$aimed = StaffFirstPerson.pushAim(poseStack, player, hand, partialTicks);
    }

    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false
        )
    )
    private void hexanime$afterHandPop(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swing,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        if (hexanime$aimed) {
            poseStack.popPose();
            hexanime$aimed = false;
        }
    }
}
