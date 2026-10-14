package net.ridez.horror.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.item.ModItems;

// Salt fired from the Straw: knockback only, no damage.
public class SaltProjectile extends BurstingProjectile {
    public SaltProjectile(EntityType<? extends SaltProjectile> type, Level level) {
        super(type, level);
    }

    public SaltProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.salt_projectile.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.salt.get();
    }

    @Override
    protected float getDamage() {
        return 0.0F;
    }
}
