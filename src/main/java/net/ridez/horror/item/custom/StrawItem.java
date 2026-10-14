package net.ridez.horror.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ridez.horror.entity.custom.SaltProjectile;
import net.ridez.horror.item.ModItems;

// Early defense weapon: loads Salt in 0.5s and fires it at a thrown snowball's speed.
// Unbreakable: it has no durability, so the crossbow's per-shot damage does nothing.
public class StrawItem extends ModCrossbowItem {
    private static final float CHARGE_SECONDS = 0.5F;
    // A thrown snowball's launch speed (a crossbow arrow is 3.15).
    private static final float SHOT_VELOCITY = 1.5F;

    public StrawItem(Properties pProperties) {
        super(pProperties, stack -> stack.is(ModItems.salt.get()), CHARGE_SECONDS, SHOT_VELOCITY);
    }

    @Override
    protected Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit) {
        return new SaltProjectile(pLevel, pShooter);
    }
}
