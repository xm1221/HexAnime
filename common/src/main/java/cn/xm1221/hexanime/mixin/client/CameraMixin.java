package cn.xm1221.hexanime.mixin.client;

import cn.xm1221.hexanime.client.CameraShakeClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void hexanime$applyShake(
        BlockGetter level,
        Entity entity,
        boolean detached,
        boolean thirdPersonReverse,
        float partialTick,
        CallbackInfo ci
    ) {
        CameraShakeClient.apply((Camera) (Object) this);
    }
}
