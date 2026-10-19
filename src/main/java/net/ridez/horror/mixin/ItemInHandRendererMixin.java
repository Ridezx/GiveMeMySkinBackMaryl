package net.ridez.horror.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.ridez.horror.client.WeaponAnimations;
import net.ridez.horror.item.custom.ModCrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The game draws anything that is a crossbow in first person with its own fixed code, before Forge's hook for
// custom item animations (IClientItemExtensions.applyForgeHandTransform) is reached. For this mod's crossbow-style
// weapons only, draw them with their own animations instead (client/WeaponAnimations).
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void horrormod$weaponAnimations(AbstractClientPlayer pPlayer, float pPartialTicks, float pPitch, InteractionHand pHand,
                                            float pSwingProgress, ItemStack pStack, float pEquippedProgress, PoseStack pPoseStack,
                                            MultiBufferSource pBuffer, int pCombinedLight, CallbackInfo ci) {
        if (pStack.getItem() instanceof ModCrossbowItem && !pPlayer.isScoping()) {
            WeaponAnimations.renderFirstPerson((ItemInHandRenderer) (Object) this, pPlayer, pPartialTicks, pHand, pSwingProgress, pStack,
                    pEquippedProgress, pPoseStack, pBuffer, pCombinedLight);
            ci.cancel();
        }
    }
}
