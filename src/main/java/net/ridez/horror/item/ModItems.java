package net.ridez.horror.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.custom.ChiselItem;
import net.ridez.horror.item.custom.FuelItem;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HorrorMod.MOD_ID);

    public static final RegistryObject<Item> pelt = ITEMS.register("pelt",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> cloth_strap = ITEMS.register("cloth_strap",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> graft = ITEMS.register("graft",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> heroine = ITEMS.register("heroine",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> dust = ITEMS.register("dust",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> straw = ITEMS.register("straw",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> crank = ITEMS.register("crank",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> speedy_car = ITEMS.register("speedy_car",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> salt = ITEMS.register("salt",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> holy_text = ITEMS.register("holy_text",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> dust_ball = ITEMS.register("dust_ball",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> aspiraton = ITEMS.register("aspiraton",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> chisel = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(32)));
    public static final RegistryObject<Item> placeholderfood = ITEMS.register("placeholderfood",
        () -> new Item(new Item.Properties().food(ModFoodProperties.placeholderfood)) {
            @Override
            public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                pTooltipComponents.add(Component.translatable("tooltip.horrormod.placeholderfood"));
                super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
            }
        });
    public static final RegistryObject<Item> placeholderfuel = ITEMS.register("placeholderfuel",
            () -> new FuelItem(new Item.Properties(),1200));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}