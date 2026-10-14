package net.ridez.horror.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.item.ModItems;

// A Dust Ball fired from the Toy Gun: 16 damage (8 hearts).
public class DustBallProjectile extends BurstingProjectile {
    private static final int MAX_LIFETIME_TICKS = 20;

    public DustBallProjectile(EntityType<? extends DustBallProjectile> type, Level level) {
        super(type, level);
    }

    public DustBallProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.dust_ball_projectile.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.dust_ball.get();
    }

    @Override
    protected float getDamage() {
        return 16.0F;
    }

    // At Toy Gun speed it covers ~300 blocks in 1 second; a miss disappears then instead of flying on
    // into unloaded chunks and hitting something later.
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }
}
