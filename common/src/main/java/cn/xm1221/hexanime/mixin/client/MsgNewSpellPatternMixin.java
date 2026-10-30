package cn.xm1221.hexanime.mixin.client;

import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C;
import cn.xm1221.hexanime.client.StaffPatternClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Local-only follow-up to Hex's pattern acknowledgement. Runs on the client thread
 * before Hex applies the same packet, so the spell GUI is still open.
 */
@Mixin(value = MsgNewSpellPatternS2C.class, remap = false)
public abstract class MsgNewSpellPatternMixin {
    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void hexanime$patternConfirmed(MsgNewSpellPatternS2C self, CallbackInfo ci) {
        boolean success = self.info().getResolutionType().getSuccess();
        boolean stackClear = self.info().isStackClear();
        Minecraft.getInstance().execute(() -> StaffPatternClient.onServerView(success, stackClear));
    }
}
