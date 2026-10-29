package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import cn.xm1221.hexanime.casting.CastPulseRelay;
import cn.xm1221.hexanime.casting.item.CastPulseKind;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OperatorSideEffect.DoMishap.class, remap = false)
public abstract class MishapSideEffectMixin {
    @Inject(method = "performEffect", at = @At("HEAD"), remap = false)
    private void hexanime$mishapAnimation(CastingVM harness, CallbackInfo ci) {
        if (harness.getEnv().getCastingEntity() instanceof ServerPlayer player) {
            CastPulseRelay.broadcast(player, harness.getEnv().getCastingHand(), CastPulseKind.MISHAP);
        }
    }
}
