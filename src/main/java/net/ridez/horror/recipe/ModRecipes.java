package net.ridez.horror.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, HorrorMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<ToolDamagingShapelessRecipe>> tool_damaging_shapeless =
            SERIALIZERS.register("tool_damaging_shapeless", ToolDamagingShapelessRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
