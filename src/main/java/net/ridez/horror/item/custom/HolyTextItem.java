package net.ridez.horror.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// While held in the off hand, gives Luck I and Strength I.
public class HolyTextItem extends Item {
    public HolyTextItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        // Refresh once a second with a 3 second duration, so the effects end shortly after the book leaves the off hand.
        if (!pLevel.isClientSide() && pEntity instanceof Player player
                && player.getOffhandItem() == pStack && pLevel.getGameTime() % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
        super.inventoryTick(pStack, pLevel, pEntity, pSlotId, pIsSelected);
    }
}
