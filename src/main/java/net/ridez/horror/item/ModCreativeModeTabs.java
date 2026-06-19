package net.ridez.horror.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.block.ModBlocks;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HorrorMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> give_me_my_skin_back_maryl_items_tab = CREATIVE_MODE_TABS.register("give_me_my_skin_back_maryl_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.pelt.get()))
                    .title(Component.translatable("creativetab.horrormod.give_me_my_skin_back_maryl_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.pelt.get());
                        output.accept(ModItems.cloth_strap.get());
                        output.accept(ModItems.dust.get());
                        output.accept(ModItems.dust_ball.get());
                        output.accept(ModItems.salt.get());
                        output.accept(ModItems.heroine.get());
                        output.accept(ModItems.straw.get());
                        output.accept(ModItems.crank.get());
                        output.accept(ModItems.speedy_car.get());
                        output.accept(ModItems.holy_text.get());
                        output.accept(ModItems.graft.get());
                        output.accept(ModItems.aspiraton.get());
                    }).build());

    public static final RegistryObject<CreativeModeTab> give_me_my_skin_back_maryl_blocks_tab = CREATIVE_MODE_TABS.register("give_me_my_skin_back_maryl_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.veinseep.get()))
                    .withTabsBefore(give_me_my_skin_back_maryl_items_tab.getId())
                    .title(Component.translatable("creativetab.horrormod.give_me_my_skin_back_maryl_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.veinseep.get());
                        output.accept(ModBlocks.veinseep_deepslate.get());
                        output.accept(ModBlocks.avolition.get());
                        output.accept(ModBlocks.dustbricks.get());
                        output.accept(ModBlocks.aspiraton_block.get());
                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}