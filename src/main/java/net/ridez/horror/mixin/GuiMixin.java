package net.ridez.horror.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.ridez.horror.client.OdieFlash;
import net.ridez.horror.client.ToyGunClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Forge 1.21 has no event for drawing on the HUD (it was removed in 1.20.5). Before the HUD, the Toy Gun's scope
// overlay (under the hotbar, like the spyglass's); after it, Odie's "Careful." flash on top. Both draw nothing
// unless they're showing.
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void horrormod$toyGunScope(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker, CallbackInfo ci) {
        ToyGunClient.renderScope(pGuiGraphics);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void horrormod$odieFlash(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker, CallbackInfo ci) {
        OdieFlash.render(pGuiGraphics);
    }
}
