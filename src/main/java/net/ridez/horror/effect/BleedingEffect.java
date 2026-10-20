package net.ridez.horror.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.ridez.horror.HorrorMod;

// Bleeding (from a Sandman's bullet): half a heart every 4 seconds while it lasts (30 seconds from a shot), whatever
// armor the target wears. Milk and anything else that clears effects stops it, and so does a Cloth Strap tied on as a
// tourniquet (see ClothStrapItem).
public class BleedingEffect extends MobEffect {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "bleeding"));
    public static final int DURATION = 30 * 20;
    private static final int INTERVAL = 80;
    private static final float DAMAGE_PER_TICK = 1.0F;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A0A0A);
    }

    @Override
    public boolean applyEffectTick(LivingEntity pEntity, int pAmplifier) {
        DamageSource source = new DamageSource(pEntity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
        pEntity.hurt(source, DAMAGE_PER_TICK);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return pDuration % INTERVAL == 0;
    }
}
