package net.ridez.horror.fluid;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.block.ModBlocks;
import net.ridez.horror.item.ModItems;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, HorrorMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, HorrorMod.MOD_ID);

    public static final RegistryObject<FluidType> tar_type = FLUID_TYPES.register("tar", TarFluidType::new);
    public static final RegistryObject<FlowingFluid> tar = FLUIDS.register("tar",
            () -> new TarFluid(tarProperties()));
    public static final RegistryObject<FlowingFluid> flowing_tar = FLUIDS.register("flowing_tar",
            () -> new ForgeFlowingFluid.Flowing(tarProperties()));

    public static final RegistryObject<FluidType> nutrinon_type = FLUID_TYPES.register("nutrinon", NutrinonFluidType::new);
    public static final RegistryObject<FlowingFluid> nutrinon = FLUIDS.register("nutrinon",
            () -> new ForgeFlowingFluid.Source(nutrinonProperties()));
    public static final RegistryObject<FlowingFluid> flowing_nutrinon = FLUIDS.register("flowing_nutrinon",
            () -> new FlowingNutrinonFluid(nutrinonProperties()));

    // Lava's overworld spread: slow ticks, short reach.
    private static ForgeFlowingFluid.Properties tarProperties() {
        return new ForgeFlowingFluid.Properties(tar_type, tar, flowing_tar)
                .bucket(ModItems.tar_bucket)
                .block(ModBlocks.tar)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    // Water's spread: fast ticks, 7-block reach.
    private static ForgeFlowingFluid.Properties nutrinonProperties() {
        return new ForgeFlowingFluid.Properties(nutrinon_type, nutrinon, flowing_nutrinon)
                .bucket(ModItems.nutrinon_bucket)
                .block(ModBlocks.nutrinon)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
