package cn.xm1221.hexanime.mixin.client;

import cn.xm1221.hexanime.client.CameraShakeClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void hexanime$applyCameraRoll(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        float roll = CameraShakeClient.currentRoll();
        if (roll != 0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void hexanime$applyFovKick(
        Camera camera,
        float partialTicks,
        boolean useFOVSetting,
        CallbackInfoReturnable<Double> cir
    ) {
        if (!useFOVSetting) return;
        float offset = CameraShakeClient.currentFovOffset();
        if (offset != 0f) {
            double adjusted = Math.max(20.0, Math.min(150.0, cir.getReturnValueD() + offset));
            cir.setReturnValue(adjusted);
        }
    }
}
