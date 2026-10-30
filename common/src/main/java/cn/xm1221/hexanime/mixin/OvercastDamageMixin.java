package cn.xm1221.hexanime.mixin;

import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.common.lib.HexDamageTypes;
import cn.xm1221.hexanime.casting.OvercastStrainRelay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;

/** Measures overcast damage after Hex Casting applies its final hurt path. */
@Mixin(value = Mishap.class, remap = false)
public abstract class OvercastDamageMixin {
    private static final ThreadLocal<IdentityHashMap<LivingEntity, Float>> HEXANIME$BEFORE_HEALTH =
        ThreadLocal.withInitial(IdentityHashMap::new);

    @Inject(method = "trulyHurt", at = @At("HEAD"), remap = false)
    private static void hexanime$captureHealth(
        LivingEntity entity,
        DamageSource source,
        float amount,
        CallbackInfo ci
    ) {
        if (entity instanceof ServerPlayer &&
            !entity.level().isClientSide &&
            amount > 0f &&
            source.is(HexDamageTypes.OVERCAST)
        ) {
            HEXANIME$BEFORE_HEALTH.get().put(entity, entity.getHealth());
        }
    }

    @Inject(method = "trulyHurt", at = @At("RETURN"), remap = false)
    private static void hexanime$broadcastActualDamage(
        LivingEntity entity,
        DamageSource source,
        float amount,
        CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayer player) || !source.is(HexDamageTypes.OVERCAST)) {
            return;
        }
        Float before = HEXANIME$BEFORE_HEALTH.get().remove(entity);
        if (before == null) {
            return;
        }
        float taken = before - entity.getHealth();
        if (taken > 0f) {
            OvercastStrainRelay.broadcast(player, taken);
        }
    }
}
