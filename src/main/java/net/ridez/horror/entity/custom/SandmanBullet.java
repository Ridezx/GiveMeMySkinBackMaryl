package net.ridez.horror.entity.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.effect.BleedingEffect;
import net.ridez.horror.effect.ModEffects;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.item.ModArmorMaterials;

// A Sandman's shot: a Dust Ball out of her Toy Gun (so a Skinwalker takes it as one: two kill it). Whatever it hits
// bleeds for 30 seconds. A player takes a set amount by the armor they wear (armor itself doesn't soften it):
//     none 5 hearts, leather or chainmail 4.5, iron or gold 4, diamond 3.5, netherite 3
// (a mixed set lands in between, by the share of each piece), and is knocked back unless their whole set is netherite.
public class SandmanBullet extends DustBallProjectile {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "sandman_shot"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float UNARMORED_DAMAGE = 10.0F;
    private static final double KNOCKBACK = 0.45;

    public SandmanBullet(EntityType<? extends SandmanBullet> type, Level level) {
        super(type, level);
    }

    public SandmanBullet(Level level, LivingEntity shooter) {
        super(ModEntities.sandman_bullet.get(), shooter, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!(pResult.getEntity() instanceof Player player)) {
            super.onHitEntity(pResult);                      // anything else: a Dust Ball's hit
            if (pResult.getEntity() instanceof LivingEntity living && living.isAlive()) {
                bleed(living);
            }
            return;
        }
        DamageSource source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),
                this, this.getOwner());
        if (player.hurt(source, gunshotDamage(player))) {
            bleed(player);
            if (armorTier(player) < 4.0F) {                  // (whole netherite set: it doesn't budge)
                Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(KNOCKBACK);
                player.push(push.x, 0.12, push.z);
                player.hurtMarked = true;
            }
        }
    }

    // (A Skinwalker bleeds for half as long.)
    private static void bleed(LivingEntity pTarget) {
        int duration = pTarget instanceof SkinwalkerEntity ? BleedingEffect.DURATION / 2 : BleedingEffect.DURATION;
        pTarget.addEffect(new MobEffectInstance(ModEffects.bleeding.getHolder().orElseThrow(), duration, 0));
    }

    // The shot's damage to a player by what they wear.
    public static float gunshotDamage(LivingEntity pTarget) {
        return UNARMORED_DAMAGE - armorTier(pTarget);
    }

    // How well the target's armor stops a shot: the average over the four armor slots of each piece's tier
    // (0 nothing, 1 leather or chainmail, 2 iron or gold, 3 diamond, 4 netherite); armor from other mods is placed by
    // its toughness and protection.
    public static float armorTier(LivingEntity pTarget) {
        float total = 0.0F;
        for (EquipmentSlot slot : ARMOR) {
            total += pieceTier(pTarget.getItemBySlot(slot), slot);
        }
        return total / ARMOR.length;
    }

    private static int pieceTier(ItemStack pStack, EquipmentSlot pSlot) {
        if (!(pStack.getItem() instanceof ArmorItem armor)) {
            return 0;
        }
        ArmorMaterial material = armor.getMaterial().value();
        if (material == ArmorMaterials.NETHERITE.value()) {
            return 4;
        }
        if (material == ArmorMaterials.DIAMOND.value() || material == ModArmorMaterials.tactical.get()) {
            return 3;
        }
        if (material == ArmorMaterials.IRON.value() || material == ArmorMaterials.GOLD.value()) {
            return 2;
        }
        if (material == ArmorMaterials.LEATHER.value() || material == ArmorMaterials.CHAIN.value()) {
            return 1;
        }
        float toughness = material.toughness();
        if (toughness >= 3.0F) {
            return 4;
        }
        if (toughness >= 2.0F) {
            return 3;
        }
        int iron = ArmorMaterials.IRON.value().getDefense(armor.getType());
        return armor.getDefense() >= iron ? 2 : 1;
    }
}
