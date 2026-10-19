package net.ridez.horror.item.custom;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.entity.custom.SaltProjectile;
import net.ridez.horror.item.ModItems;

// Early defense weapon, a spitball shooter: loads Salt in 0.5s (the free hand drops a pinch of salt into the straw;
// it goes up to the mouth once it's loaded) and spits it at about an arrow's speed. Unbreakable: it has no
// durability, so the crossbow's per-shot damage does nothing.
public class StrawItem extends ModCrossbowItem {
    private static final float CHARGE_SECONDS = 0.5F;
    // Twice a thrown snowball's launch speed, just under a crossbow arrow's 3.15.
    private static final float SHOT_VELOCITY = 3.0F;

    public StrawItem(Properties pProperties) {
        super(pProperties, stack -> stack.is(ModItems.salt.get()), CHARGE_SECONDS, SHOT_VELOCITY);
    }

    @Override
    protected Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit) {
        return new SaltProjectile(pLevel, pShooter);
    }

    @Override
    protected SoundEvent shotSound() {
        return SoundEvents.LLAMA_SPIT;
    }

    // A spitball: a little spit and a spray of salt grains out of the end of the straw.
    @Override
    protected void firedEffects(ServerLevel pLevel, LivingEntity pShooter, InteractionHand pHand) {
        Vec3 tip = inFrontOfEyes(pShooter, pHand, 0.9, 0.0, 0.15);
        Vec3 look = pShooter.getViewVector(1.0F);
        pLevel.sendParticles(ParticleTypes.SPIT, tip.x, tip.y, tip.z, 0, look.x, look.y, look.z, 0.25);
        pLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.salt.get())),
                tip.x, tip.y, tip.z, 6, 0.04, 0.04, 0.04, 0.06);
    }
}
