package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.common.items.magic.ItemPackagedHex;
import cn.xm1221.hexanime.casting.CastPulseRelay;
import cn.xm1221.hexanime.casting.item.PackagedHexPulsePolicy;
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

/**
 * Hex Casting 0.11.4 {@code ItemPackagedHex.use} (server): CONSUME only when
 * {@code breakAfterDepletion() && getMedia(stack) == 0}, after shrinking the stack.
 * Every other successful cast returns SUCCESS, including a cypher that still holds media.
 * The client path returns SUCCESS early and is ignored here.
 */
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
            CastPulseRelay.broadcast(serverPlayer, hand, PackagedHexPulsePolicy.kindForBroken(false));
        } else if (result == InteractionResult.CONSUME) {
            CastPulseRelay.broadcast(serverPlayer, hand, PackagedHexPulsePolicy.kindForBroken(true));
        }
    }
}
