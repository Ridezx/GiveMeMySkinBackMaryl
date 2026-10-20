package net.ridez.horror;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.ridez.horror.block.ModBlocks;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.fluid.ModFluids;
import net.ridez.horror.item.ModCreativeModeTabs;
import net.ridez.horror.item.ModItems;
import net.ridez.horror.network.ModNetwork;
import net.ridez.horror.sound.ModSounds;
import net.ridez.horror.effect.ModEffects;
import net.ridez.horror.item.ModArmorMaterials;
import net.ridez.horror.menu.ModMenus;
import net.ridez.horror.loot.ModLootModifiers;
import net.ridez.horror.recipe.ModRecipes;
import net.ridez.horror.worldgen.ModFeatures;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HorrorMod.MOD_ID)
public class HorrorMod
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "horrormod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public HorrorMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);


        ModItems.register(modEventBus);
        ModNetwork.register();
        ModBlocks.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModFluids.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModMenus.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Nutrinon hardens into Mytoma where it touches water, like lava turning into obsidian.
        // The registry isn't thread-safe, so register on the main thread.
        event.enqueueWork(() -> FluidInteractionRegistry.addInteraction(ModFluids.nutrinon_type.get(),
                new FluidInteractionRegistry.InteractionInformation(ForgeMod.WATER_TYPE.get(), ModBlocks.mytoma.get().defaultBlockState())));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }
}
