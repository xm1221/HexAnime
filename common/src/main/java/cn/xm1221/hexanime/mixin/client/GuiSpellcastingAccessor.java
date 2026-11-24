package cn.xm1221.hexanime.mixin.client;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes private casting GUI fields: which hand opened the grid and drawn patterns.
 */
@Mixin(value = GuiSpellcasting.class, remap = false)
public interface GuiSpellcastingAccessor {
    @Accessor("handOpenedWith")
    InteractionHand hexanime$getHandOpenedWith();

    @Accessor("patterns")
    List<ResolvedPattern> hexanime$getPatterns();
}
