package net.ridez.horror.entity.custom;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// An item fired from a ModCrossbowItem. Flies with arrow drop, damages what it hits (0 damage still
// knocks back, like a snowball), then bursts into particles of its item and is gone, so it can't be
// picked up. Rendered as its item (see ModEntityRenderers).
public abstract class BurstingProjectile extends ThrowableItemProjectile {
    private static final byte HIT_EVENT = 3;

    protected BurstingProjectile(EntityType<? extends BurstingProjectile> type, Level level) {
        super(type, level);
    }

    protected BurstingProjectile(EntityType<? extends BurstingProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    protected abstract float getDamage();

    // Arrow gravity instead of the lighter snowball/egg gravity.
    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), this.getDamage());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, HIT_EVENT);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == HIT_EVENT) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }
}
