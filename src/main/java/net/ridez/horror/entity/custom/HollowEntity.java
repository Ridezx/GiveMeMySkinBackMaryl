package net.ridez.horror.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.ridez.horror.sound.ModSounds;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

// The Hollow: someone who chose the wrong skin too many times, a corpse in rotten skin (model and animations:
// art/tools/hollow_model.py). Harmless: it trudges slowly about and pays players no attention at all, never
// attacking. It doesn't react to being hurt either: no flinch, no knockback, no cry, no running; it just goes on
// until it falls apart (its death animation breaks it into pieces on the ground). Now and then, standing, it
// twitches. Sandmen hunt it (the horrormod:sandman_hunts tag). It drops 1-3 Rotted Pelt.
// - Salt (the Straw) is the one thing it feels: hit with salt, it screams and curls up on the ground in a ball,
//   knees to its chest and its fists over its head, and stays that way until 45 seconds after the last hit (any
//   hit while it's down starts the 45 seconds over); then it slowly gets back up. Down, it stays where it is: it doesn't
//   turn or get pushed. Killed while curled up, it falls apart where it lies.
// - Killed by a Dust Ball (the Toy Gun's, or a Sandman's shot), it shakes violently for a second before falling apart.
// No natural spawning yet: it's meant to haunt holy places (the Mecca Black Box), which will bring it in once they
// exist; until then only its spawn egg does.
public class HollowEntity extends PathfinderMob implements GeoEntity {
    private static final double STROLL_SPEED = 1.0;
    private static final int STROLL_INTERVAL = 160;                        // sets off about every 8 seconds
    private static final int DEATH_TICKS = 60;                             // the 2.2 s fall apart, then lying there a moment
    private static final int DUST_DEATH_TICKS = 80;                        // a second of shaking first
    private static final int CURLED_DEATH_TICKS = 44;                      // falling apart where it lies curled up
    private static final int CURLED_TICKS = 45 * 20;                       // down this long after the last hit
    private static final int UNCURL_TICKS = 60;                            // getting back up
    private static final int NOT_CURLED = 0, CURLED = 1, UNCURLING = 2;
    private static final EntityDataAccessor<Integer> CURL = SynchedEntityData.defineId(HollowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DUST_DEATH = SynchedEntityData.defineId(HollowEntity.class, EntityDataSerializers.BOOLEAN);
    // How fast the walk animation goes at its own speed (blocks a tick: hollow_model.py's WALK_SPEED); it's played
    // faster or slower to match how fast the Hollow really moves, so its feet stay planted.
    private static final double WALK_ANIM_SPEED = 0.0284;
    private static final int TWITCH_MIN = 8 * 20, TWITCH_MAX = 20 * 20, TWITCH_TICKS = 18;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // client: when it next twitches, and when the last one started
    private int nextTwitchAt = -1, twitchStartedAt = -100;
    private int curlTicks;                                                 // server: how long it stays down / takes to get up
    private float curlYaw;                                                 // which way it lies

    public HollowEntity(EntityType<? extends PathfinderMob> pType, Level pLevel) {
        super(pType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(CURL, NOT_CURLED);
        pBuilder.define(DUST_DEATH, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED) {
            @Override
            public boolean canUse() {
                return !HollowEntity.this.isDown() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !HollowEntity.this.isDown() && super.canContinueToUse();
            }
        };
        stroll.setInterval(STROLL_INTERVAL);
        this.goalSelector.addGoal(5, stroll);
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !HollowEntity.this.isDown() && super.canUse();
            }
        });
    }

    // ---------------------------------------------------------------- it doesn't react to being hurt (except by salt)
    @Override
    public void knockback(double pStrength, double pX, double pZ) {
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean salt = pSource.getDirectEntity() instanceof SaltProjectile;
        if (this.level().isClientSide() || this.isInvulnerableTo(pSource) || !this.isAlive()) {
            return super.hurt(pSource, pAmount);
        }
        if (pSource.getDirectEntity() instanceof DustBallProjectile && pAmount >= this.getHealth()) {
            this.entityData.set(DUST_DEATH, true);            // (before it dies, so its death is the shaking one)
        }
        boolean hurt = super.hurt(pSource, pAmount);
        if (!this.isAlive()) {
            return hurt;
        }
        if (salt && this.entityData.get(CURL) != CURLED) {
            this.playSound(ModSounds.hollow_scream.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
            this.entityData.set(CURL, CURLED);
            this.getNavigation().stop();
            this.curlTicks = CURLED_TICKS;
            this.curlYaw = this.yBodyRot;
        } else if (this.entityData.get(CURL) == CURLED && (hurt || salt)) {
            this.curlTicks = CURLED_TICKS;                    // hit again while down: it stays down
        }
        return hurt;
    }

    // Curled up on the ground or still getting up: it doesn't move, turn or get pushed about.
    public boolean isDown() {
        return this.entityData.get(CURL) != NOT_CURLED;
    }

    @Override
    public boolean isPushable() {
        return !this.isDown() && super.isPushable();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDown() || this.isDeadOrDying() && this.entityData.get(CURL) == CURLED) {
            this.setYRot(this.curlYaw);                       // (it lies as it fell, facing the way it was)
            this.yRotO = this.curlYaw;
            this.yBodyRot = this.yBodyRotO = this.curlYaw;
            this.yHeadRot = this.yHeadRotO = this.curlYaw;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !this.isAlive() || !this.isDown()) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        if (--this.curlTicks <= 0) {
            if (this.entityData.get(CURL) == CURLED) {
                this.entityData.set(CURL, UNCURLING);
                this.curlTicks = UNCURL_TICKS;
            } else {
                this.entityData.set(CURL, NOT_CURLED);
            }
        }
    }

    // Lying curled up, it's low to the ground.
    @Override
    public EntityDimensions getDefaultDimensions(Pose pPose) {
        EntityDimensions size = super.getDefaultDimensions(pPose);
        return this.entityData.get(CURL) == CURLED ? EntityDimensions.scalable(size.width() * 1.5F, 0.75F) : size;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (CURL.equals(pKey)) {
            this.refreshDimensions();
            if (this.entityData.get(CURL) == CURLED) {
                this.curlYaw = this.yBodyRot;                 // (the client too: which way it went down)
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Curl", this.entityData.get(CURL));
        pCompound.putInt("CurlTicks", this.curlTicks);
        pCompound.putFloat("CurlYaw", this.curlYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.entityData.set(CURL, pCompound.getInt("Curl"));
        this.curlTicks = pCompound.getInt("CurlTicks");
        this.curlYaw = pCompound.getFloat("CurlYaw");
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.hollow_ambient.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.hollow_death.get();
    }

    // ---------------------------------------------------------------- death: the body stays while it falls apart
    @Override
    protected void tickDeath() {
        this.deathTime++;
        int ticks = this.entityData.get(CURL) == CURLED ? CURLED_DEATH_TICKS : this.entityData.get(DUST_DEATH) ? DUST_DEATH_TICKS : DEATH_TICKS;
        if (this.deathTime >= ticks && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // ---------------------------------------------------------------- animation (art/tools/hollow_model.py makes them)
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation TWITCH = RawAnimation.begin().thenPlayAndHold("twitch");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation DEATH_DUST = RawAnimation.begin().thenPlayAndHold("death_dust");
    private static final RawAnimation DEATH_CURLED = RawAnimation.begin().thenPlayAndHold("death_curled");
    private static final RawAnimation CURL_UP = RawAnimation.begin().thenPlay("curl").thenLoop("curled");
    private static final RawAnimation UNCURL = RawAnimation.begin().thenPlayAndHold("uncurl");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animate));
    }

    private PlayState animate(AnimationState<HollowEntity> state) {
        state.setControllerSpeed(1.0F);
        if (this.isDeadOrDying()) {
            if (this.entityData.get(CURL) == CURLED) {
                return state.setAndContinue(DEATH_CURLED);   // (falls apart where it lies)
            }
            return state.setAndContinue(this.entityData.get(DUST_DEATH) ? DEATH_DUST : DEATH);
        }
        int curl = this.entityData.get(CURL);
        if (curl != NOT_CURLED) {
            this.twitchStartedAt = -100;
            return state.setAndContinue(curl == CURLED ? CURL_UP : UNCURL);
        }
        double speed = Math.sqrt(Mth.square(this.getX() - this.xo) + Mth.square(this.getZ() - this.zo));
        if (speed > 0.004) {
            this.twitchStartedAt = -100;
            state.setControllerSpeed((float) Mth.clamp(speed / WALK_ANIM_SPEED, 0.5, 2.5));
            return state.setAndContinue(WALK);
        }
        if (this.nextTwitchAt < 0) {
            this.nextTwitchAt = this.tickCount + Mth.nextInt(this.random, TWITCH_MIN, TWITCH_MAX);
        }
        if (this.tickCount >= this.nextTwitchAt) {                         // a spasm, now and then, standing
            this.twitchStartedAt = this.tickCount;
            this.nextTwitchAt = this.tickCount + Mth.nextInt(this.random, TWITCH_MIN, TWITCH_MAX);
            state.getController().forceAnimationReset();
        }
        if (this.tickCount - this.twitchStartedAt < TWITCH_TICKS) {
            return state.setAndContinue(TWITCH);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
