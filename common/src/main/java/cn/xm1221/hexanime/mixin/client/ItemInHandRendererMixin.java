package cn.xm1221.hexanime.mixin.client;

import cn.xm1221.hexanime.client.StaffFirstPerson;
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

/**
 * Rotates the vanilla first-person staff hand with the spell cursor.
 *
 * {@code renderArmWithItem} has one push and one pop, and the scoping return happens before the
 * push, so the pair stays balanced. The extra pose is outside vanilla's, in the world hand pass,
 * which is drawn before {@code Screen.render}. First person mode on the playerAnimator layer stays
 * {@code NONE}, so this does not change the third-person bones.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Unique
    private boolean hexanime$aimed;

    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
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
