package net.ridez.horror.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// A crossbow-style weapon (hold to load, stays loaded, use again to fire; the Straw) with its own ammo, load time,
// shot speed, projectile, shot sound and firing effects. Extending CrossbowItem reuses vanilla's loading/firing
// logic, loading sounds, tooltip and third-person arm poses. The load time is applied through CrossbowItemMixin,
// since vanilla's is hardcoded; the first-person animations are in client/WeaponAnimations.
public abstract class ModCrossbowItem extends CrossbowItem {
    // When each player last fired one of these (game time), for the first-person recoil and lowering animations.
    private static final Map<UUID, Long> LAST_SHOT = new ConcurrentHashMap<>();

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

    // The sound of a shot, instead of the crossbow's twang.
    protected abstract SoundEvent shotSound();

    protected float shotPitch(LivingEntity pShooter) {
        return 0.9F + pShooter.getRandom().nextFloat() * 0.2F;
    }

    // Particles and the like at the moment of firing (server side, so everyone nearby sees them).
    protected void firedEffects(ServerLevel pLevel, LivingEntity pShooter, InteractionHand pHand) {
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        boolean loaded = isCharged(pPlayer.getItemInHand(pHand));
        InteractionResultHolder<ItemStack> result = super.use(pLevel, pPlayer, pHand);
        if (loaded && pLevel.isClientSide()) {
            LAST_SHOT.put(pPlayer.getUUID(), pLevel.getGameTime());
        }
        return result;
    }

    // Records a shot seen on the client (other players firing: see WeaponAnimations).
    public static void recordShot(UUID pPlayer, long pGameTime) {
        LAST_SHOT.put(pPlayer, pGameTime);
    }

    // Loading or firing changes the item's data; without this the first-person hand would dip and briefly draw the
    // old (unloaded) item, as when switching items, cutting the loading and aiming animations short.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack pOldStack, ItemStack pNewStack, boolean pSlotChanged) {
        return pSlotChanged || !pOldStack.is(pNewStack.getItem());
    }

    // Game time this player last fired one (client side), or far in the past if never.
    public static long lastShotTime(UUID pPlayer) {
        return LAST_SHOT.getOrDefault(pPlayer, Long.MIN_VALUE / 2);
    }

    @Override
    public void performShooting(Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pWeapon, float pVelocity,
                                float pInaccuracy, @Nullable LivingEntity pTarget) {
        boolean loaded = isCharged(pWeapon);
        super.performShooting(pLevel, pShooter, pHand, pWeapon, pVelocity, pInaccuracy, pTarget);
        if (loaded && !isCharged(pWeapon) && pLevel instanceof ServerLevel serverLevel) {
            this.firedEffects(serverLevel, pShooter, pHand);
        }
    }

    // Vanilla's aim (straight where the shooter looks), with this weapon's speed and its own sound.
    @Override
    protected void shootProjectile(LivingEntity pShooter, Projectile pProjectile, int pIndex, float pVelocity, float pInaccuracy,
                                   float pAngle, @Nullable LivingEntity pTarget) {
        if (pTarget != null) {                              // mobs aiming at a target: vanilla's aim (and twang)
            super.shootProjectile(pShooter, pProjectile, pIndex, this.shotVelocity, pInaccuracy, pAngle, pTarget);
            return;
        }
        Vec3 look = pShooter.getViewVector(1.0F);
        pProjectile.shoot(look.x, look.y, look.z, this.shotVelocity, pInaccuracy);
        pShooter.level().playSound(null, pShooter.getX(), pShooter.getY(), pShooter.getZ(), this.shotSound(),
                pShooter.getSoundSource(), 1.0F, this.shotPitch(pShooter));
    }

    // A point in front of the shooter's eyes: forward along the look, to the side of the hand holding the weapon,
    // and down.
    protected static Vec3 inFrontOfEyes(LivingEntity pShooter, InteractionHand pHand, double pForward, double pSide, double pDown) {
        Vec3 look = pShooter.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        boolean rightHand = (pHand == InteractionHand.MAIN_HAND) == (pShooter.getMainArm() == HumanoidArm.RIGHT);
        return pShooter.getEyePosition().add(look.scale(pForward)).add(right.scale(rightHand ? pSide : -pSide)).subtract(up.scale(pDown));
    }
}
