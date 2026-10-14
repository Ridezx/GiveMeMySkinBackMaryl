package net.ridez.horror.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ridez.horror.entity.custom.DustBallProjectile;
import net.ridez.horror.item.ModItems;

// Rare, uncraftable weapon: loads a Dust Ball in 3s and fires it fast enough to act almost like hitscan.
// Loses 1 durability per shot (vanilla crossbow behavior).
public class ToyGunItem extends ModCrossbowItem {
    private static final float CHARGE_SECONDS = 3.0F;
    // Blocks per tick; a crossbow arrow is 3.15. Projectiles check their whole path each tick, so this
    // can't skip through targets: anything within ~15 blocks is hit the same tick it's fired.
    private static final float SHOT_VELOCITY = 15.0F;

    public ToyGunItem(Properties pProperties) {
        super(pProperties, stack -> stack.is(ModItems.dust_ball.get()), CHARGE_SECONDS, SHOT_VELOCITY);
    }

    @Override
    protected Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit) {
        return new DustBallProjectile(pLevel, pShooter);
    }
}
