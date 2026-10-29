package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.common.items.magic.ItemPackagedHex;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HexAnime owns the packaged-Hex pose, so suppress Hex's vanilla BLOCK use animation.
 */
@Mixin(value = ItemPackagedHex.class, remap = false)
public abstract class PackagedHexUseAnimationMixin {
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true, remap = false)
    private void hexanime$disableVanillaUseAnimation(CallbackInfoReturnable<UseAnim> cir) {
        cir.setReturnValue(UseAnim.NONE);
    }
}
