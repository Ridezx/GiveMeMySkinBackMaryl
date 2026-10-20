package net.ridez.horror.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ridez.horror.effect.ModEffects;

// A tourniquet: used while bleeding (right-click), it's tied on and the bleeding stops. One strap each time.
public class ClothStrapItem extends Item {
    public ClothStrapItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        var bleeding = ModEffects.bleeding.getHolder().orElseThrow();
        if (!pPlayer.hasEffect(bleeding)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!pLevel.isClientSide()) {
            pPlayer.removeEffect(bleeding);
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(),
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            stack.consume(1, pPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, pLevel.isClientSide());
    }
}
