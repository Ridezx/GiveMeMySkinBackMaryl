package net.ridez.horror.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.ridez.horror.item.custom.ScalpelItem;

// A normal shapeless recipe, except any Scalpel in the grid is handed back with 1 durability used
// (or breaks on its last use) instead of being consumed.
// Done per recipe rather than as an item-wide crafting remainder: a global remainder would also apply to
// vanilla's "combine two damaged tools" repair recipe and duplicate scalpels.
public class ToolDamagingShapelessRecipe extends ShapelessRecipe {
    public ToolDamagingShapelessRecipe(ShapelessRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof ScalpelItem) {
                ItemStack damaged = stack.copy();
                damaged.setDamageValue(damaged.getDamageValue() + 1);
                remaining.set(i, damaged.getDamageValue() >= damaged.getMaxDamage() ? ItemStack.EMPTY : damaged);
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.tool_damaging_shapeless.get();
    }

    // Same JSON and network format as minecraft:crafting_shapeless.
    public static class Serializer implements RecipeSerializer<ToolDamagingShapelessRecipe> {
        private static final MapCodec<ToolDamagingShapelessRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(ToolDamagingShapelessRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, ToolDamagingShapelessRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(ToolDamagingShapelessRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<ToolDamagingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolDamagingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
