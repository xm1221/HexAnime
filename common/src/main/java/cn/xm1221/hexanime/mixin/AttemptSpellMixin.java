package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import cn.xm1221.hexanime.casting.StaffCastRelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Staff flourish follows a spell that plays the casting sound. Patterns that only
 * touch the stack do not, and closing the spell GUI does not either.
 */
@Mixin(value = OperatorSideEffect.AttemptSpell.class, remap = false)
public abstract class AttemptSpellMixin {
    @Inject(method = "performEffect", at = @At("TAIL"), remap = false)
    private void hexanime$staffCast(CastingVM harness, CallbackInfo ci) {
        OperatorSideEffect.AttemptSpell spell = (OperatorSideEffect.AttemptSpell) (Object) this;
        if (!spell.getHasCastingSound()) return;
        StaffCastRelay.onAttemptSpell(harness);
    }
}
