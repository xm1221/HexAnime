package cn.xm1221.hexanime.mixin.client;

import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code handOpenedWith} is private on Hex's spell GUI. The staff arm follows that hand
 * (main vs offhand), then the player's main arm picks right vs left.
 */
@Mixin(value = GuiSpellcasting.class, remap = false)
public interface GuiSpellcastingAccessor {
    @Accessor("handOpenedWith")
    InteractionHand hexanime$getHandOpenedWith();
}
