package net.ridez.horror;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.ridez.horror.block.ModBlocks;
import net.ridez.horror.item.ModCreativeModeTabs;
import net.ridez.horror.item.ModItems;
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
        ModBlocks.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.cloth_strap);
            event.accept(ModItems.dust);
            event.accept(ModItems.dust_ball);
        }
        if(event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS)
        {
            event.accept(ModBlocks.veinseep);
            event.accept(ModBlocks.veinseep_deepslate);
            event.accept(ModItems.salt);
            event.accept(ModItems.aspiraton);
        }
        if(event.getTabKey() == CreativeModeTabs.COLORED_BLOCKS)
        {

        }
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
        {
            event.accept(ModItems.heroine);
            event.accept(ModItems.straw);
            event.accept(ModItems.crank);
            event.accept(ModItems.speedy_car);
            event.accept(ModItems.holy_text);
        }
        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        {
            event.accept(ModBlocks.avolition);
            event.accept(ModBlocks.dustbricks);
            event.accept(ModBlocks.aspiraton_block);
        }
        if(event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS)
        {

        }
        if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
        {

        }
        if(event.getTabKey() == CreativeModeTabs.COMBAT)
        {

        }
        if(event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS)
        {
            event.accept(ModItems.pelt);
            event.accept(ModItems.graft);
        }
        if(event.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
        {

        }
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
