package net.ridez.horror.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.ridez.horror.item.custom.ModCrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The crossbow's 1.25s load time is hardcoded in a static method (no Forge hook) that vanilla reads for
// loading, the loading sounds and the first-person animation. Swap in each ModCrossbowItem's own base time
// there so all of them stay in sync. Only affects this mod's weapons; real crossbows are untouched.
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Inject(method = "getChargeDuration", at = @At("HEAD"), cancellable = true)
    private static void horrormod$modCrossbowChargeDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ModCrossbowItem weapon) {
            float seconds = EnchantmentHelper.modifyCrossbowChargingTime(stack, entity, weapon.getChargeSeconds());
            cir.setReturnValue(Mth.floor(seconds * 20.0F));
        }
    }
}
