package net.ridez.horror.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

// A crossbow-style weapon (hold to load, stays loaded, use again to fire) with its own ammo, load time,
// shot speed and projectile. Extending CrossbowItem reuses vanilla's loading/firing logic, sounds, tooltip
// and arm poses. The load time is applied through CrossbowItemMixin, since vanilla's is hardcoded.
public abstract class ModCrossbowItem extends CrossbowItem {
    private final Predicate<ItemStack> ammo;
    private final float chargeSeconds;
    private final float shotVelocity;

    protected ModCrossbowItem(Properties pProperties, Predicate<ItemStack> ammo, float chargeSeconds, float shotVelocity) {
        super(pProperties);
        this.ammo = ammo;
        this.chargeSeconds = chargeSeconds;
        this.shotVelocity = shotVelocity;
    }

    public float getChargeSeconds() {
        return this.chargeSeconds;
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return this.ammo;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return this.ammo;
    }

    // Subclasses always create their own projectile, even when creative mode loads vanilla's fallback arrow.
    @Override
    protected abstract Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit);

    @Override
    protected void shootProjectile(LivingEntity pShooter, Projectile pProjectile, int pIndex, float pVelocity, float pInaccuracy, float pAngle, @Nullable LivingEntity pTarget) {
        super.shootProjectile(pShooter, pProjectile, pIndex, this.shotVelocity, pInaccuracy, pAngle, pTarget);
    }
}
