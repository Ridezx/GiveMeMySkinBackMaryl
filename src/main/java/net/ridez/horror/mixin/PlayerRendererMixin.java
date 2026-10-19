package net.ridez.horror.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.ridez.horror.client.WeaponAnimations;
import net.ridez.horror.item.custom.ModCrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The game gives any crossbow its own arm poses before Forge's hook for custom ones
// (IClientItemExtensions.getArmPose) is reached. For this mod's crossbow-style weapons only, use their own poses
// (client/WeaponAnimations), so other players see them aim, reload and fire.
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void horrormod$weaponPoses(AbstractClientPlayer pPlayer, InteractionHand pHand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (stack.getItem() instanceof ModCrossbowItem) {
            cir.setReturnValue(WeaponAnimations.armPose(pPlayer, pHand, stack));
        }
    }
}
