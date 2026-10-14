package net.ridez.horror.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Furnaces normally delete the whole input item. For this mod's own smelting recipes only, leave the
// input's container behind instead, e.g. smelting a water bucket into salt keeps the empty bucket.
// Other mods' recipes are untouched, and an @Inject (not @Redirect) can't clash with other mods' furnace mixins.
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    // Runs just before vanilla shrinks the input. Swapping the slot's stack for its container leaves vanilla
    // shrinking the old, now-detached stack.
    @Inject(method = "burn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void horrormod$keepContainer(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe,
                                         NonNullList<ItemStack> items, int maxStackSize,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (recipe == null || !recipe.id().getNamespace().equals(HorrorMod.MOD_ID)) {
            return;
        }
        ItemStack input = items.get(0);
        if (input.getCount() == 1 && input.hasCraftingRemainingItem()) {
            items.set(0, input.getCraftingRemainingItem());
        }
    }
}
