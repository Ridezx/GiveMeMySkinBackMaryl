package net.ridez.horror.event;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SandmanBullet;
import net.ridez.horror.entity.custom.SkinwalkerEntity;
import net.ridez.horror.item.custom.TacticalVestItem;

// The Tactical Vest's buff: half the damage from Skinwalkers' hits and from Sandmen's shots while it's worn.
// (The Tactical Boots' buff, walking through Tar unslowed, is in TarFluidType.)
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class TacticalGearEvents {
    private static final float VEST_REDUCTION = 0.5F;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if ((event.getSource().is(SkinwalkerEntity.SKINWALKER_DAMAGE) || event.getSource().is(SandmanBullet.DAMAGE))
                && TacticalVestItem.isWorn(event.getEntity())) {
            event.setAmount(event.getAmount() * VEST_REDUCTION);
        }
    }
}
