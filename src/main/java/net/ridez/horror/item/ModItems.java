package net.ridez.horror.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.fluid.ModFluids;
import net.ridez.horror.item.custom.BerryBushItem;
import net.ridez.horror.item.custom.ChiselItem;
import net.ridez.horror.item.custom.FuelItem;
import net.ridez.horror.item.custom.GrapeHugsItem;
import net.ridez.horror.item.custom.HolyTextItem;
import net.ridez.horror.item.custom.ScalpelItem;
import net.ridez.horror.item.custom.StrawItem;
import net.ridez.horror.item.custom.ToyGunItem;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HorrorMod.MOD_ID);

    public static final RegistryObject<Item> rotted_pelt = ITEMS.register("rotted_pelt",
            ()->new Item(new Item.Properties().food(ModFoodProperties.rotted_pelt)));
    public static final RegistryObject<Item> pelt = ITEMS.register("pelt",
            ()->new Item(new Item.Properties().food(ModFoodProperties.pelt)));
    public static final RegistryObject<Item> cloth_strap = ITEMS.register("cloth_strap",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> graft = ITEMS.register("graft",
            ()->new Item(new Item.Properties().food(ModFoodProperties.graft)));
    public static final RegistryObject<Item> heroine = ITEMS.register("heroine",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> dust = ITEMS.register("dust",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> straw = ITEMS.register("straw",
            ()->new StrawItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> toy_gun = ITEMS.register("toy_gun",
            ()->new ToyGunItem(new Item.Properties().durability(150)));
    public static final RegistryObject<Item> crank = ITEMS.register("crank",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> salt = ITEMS.register("salt",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> holy_text = ITEMS.register("holy_text",
            ()->new HolyTextItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> dust_ball = ITEMS.register("dust_ball",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> aspiraton = ITEMS.register("aspiraton",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> aspiraton_drop = ITEMS.register("aspiraton_drop",
            ()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> chisel = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(32)));
    public static final RegistryObject<Item> scalpel = ITEMS.register("scalpel",
            () -> new ScalpelItem(new Item.Properties().durability(238).attributes(ScalpelItem.createAttributes())));
    public static final RegistryObject<Item> grape_hugs = ITEMS.register("grape_hugs",
            () -> new GrapeHugsItem(new Item.Properties().food(ModFoodProperties.grape_hugs)));
    public static final RegistryObject<Item> tar_bucket = ITEMS.register("tar_bucket",
            () -> new BucketItem(ModFluids.tar, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final RegistryObject<Item> her_favorite_dress = ITEMS.register("her_favorite_dress",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> gustav_spawn_egg = ITEMS.register("gustav_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.gustav, 0x2B2B2B, 0xC9A9A6, new Item.Properties()));
    public static final RegistryObject<Item> walter_spawn_egg = ITEMS.register("walter_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.walter, 0x3B5E2B, 0xE8D44D, new Item.Properties()));
    public static final RegistryObject<Item> frank_spawn_egg = ITEMS.register("frank_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.frank, 0x4A3728, 0x9C2A2A, new Item.Properties()));
    public static final RegistryObject<Item> sweet_berry_bush = ITEMS.register("sweet_berry_bush",
            () -> new BerryBushItem(new Item.Properties()));
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