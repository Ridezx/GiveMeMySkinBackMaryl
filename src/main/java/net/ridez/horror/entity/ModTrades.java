package net.ridez.horror.entity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.ridez.horror.block.ModBlocks;
import net.ridez.horror.item.ModItems;

import java.util.List;

// Each trader's trades. "Buys" = the player gives the item and gets paid; "sells" = the player pays.
//
// Balance: Dust, Dust Balls (4 Dust) and Dust Bricks (16 Dust) convert freely by crafting, so prices are
// compared per Dust. Gustav pays at most ~0.33 emeralds' worth per Dust (Bricks at 5 = 0.31, Balls 0.25,
// 5 Coal ~0.33 since fishermen buy 15 Coal per emerald); Walter charges 0.5 per Dust on everything.
// Keep Walter's per-Dust price above Gustav's, or players can buy from Walter and sell to Gustav forever.
public class ModTrades {
    // Uses per trade before it locks until the next day's restock.
    private static final int MAX_USES = 12;

    // Gustav buys Dust goods.
    public static List<MerchantOffer> gustav() {
        return List.of(
                trade(ModItems.dust_ball.get(), 4, Items.EMERALD, 4),
                trade(ModBlocks.dustbricks.get(), 1, Items.EMERALD, 5),
                trade(ModItems.dust.get(), 1, Items.COAL, 5),
                trade(ModItems.dust.get(), 1, Items.COOKED_BEEF, 3),
                trade(ModBlocks.dustbricks.get(), 5, ModItems.her_favorite_dress.get(), 1));
    }

    // Walter sells Dust goods.
    public static List<MerchantOffer> walter() {
        return List.of(
                trade(Items.EMERALD, 1, ModItems.dust.get(), 2),
                trade(Items.EMERALD, 20, ModItems.dust_ball.get(), 10),
                trade(Items.EMERALD, 8, ModBlocks.dustbricks.get(), 1));
    }

    // Frank sells the items otherwise only found around the world.
    public static List<MerchantOffer> frank() {
        return List.of(
                trade(Items.EMERALD, 10, ModItems.crank.get(), 1),
                trade(Items.EMERALD, 10, ModItems.heroine.get(), 1),
                trade(Items.EMERALD, 10, ModBlocks.speedy_car.get(), 1),
                trade(Items.IRON_INGOT, 10, ModItems.cloth_strap.get(), 1));
    }

    private static MerchantOffer trade(ItemLike cost, int costCount, ItemLike result, int resultCount) {
        return new MerchantOffer(new ItemCost(cost, costCount), new ItemStack(result, resultCount), MAX_USES, 1, 0.0F);
    }
}
