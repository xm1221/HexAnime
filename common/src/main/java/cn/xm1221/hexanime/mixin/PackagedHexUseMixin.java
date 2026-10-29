package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.common.items.magic.ItemPackagedHex;
import cn.xm1221.hexanime.casting.CastPulseRelay;
import cn.xm1221.hexanime.casting.item.CastPulseKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemPackagedHex.class, remap = false)
public abstract class PackagedHexUseMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void hexanime$afterPackagedUse(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;

        var result = cir.getReturnValue().getResult();
        if (result == InteractionResult.SUCCESS) {
            CastPulseRelay.broadcast(serverPlayer, hand, CastPulseKind.CAST);
        } else if (result == InteractionResult.CONSUME) {
            CastPulseRelay.broadcast(serverPlayer, hand, CastPulseKind.BREAK);
        }
    }
}
