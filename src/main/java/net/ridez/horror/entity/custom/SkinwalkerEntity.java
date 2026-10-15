package net.ridez.horror.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

// A towering, hunched creature wearing skin thrown over itself that isn't its own. That skin doesn't work
// for it, so it wants to take yours and see if it fits.
// - 70% of Skinwalkers hunt players on sight; the rest ignore them until hurt.
// - Very tanky: normal attacks only do a quarter of their damage, so players are meant to run.
// - When a player hurts it, it screams, then either flees or fights (50/50).
// - Salt (Straw): no damage, but the third hit makes it scream and flee.
// - Dust Ball (Toy Gun): full damage, and the second hit kills it outright. A first hit has a 10% chance to
//   enrage it instead: it freezes and screams at the shooter for 1.5 seconds, then charges them at high
//   speed for a few seconds.
public class SkinwalkerEntity extends Monster {
    private static final float HUNTS_ON_SIGHT_CHANCE = 0.7F;
    private static final float NORMAL_DAMAGE_MULTIPLIER = 0.25F;
    private static final int SALT_HITS_TO_FLEE = 3;
    private static final int DUST_BALL_HITS_TO_KILL = 2;
    // 15 seconds of running once scared.
    private static final int FLEE_TICKS = 300;
    // Flee speed relative to its chase speed: ~2x the setting gives ~26 blocks/s, almost 4x a sprint-jumping
    // player, so a scared Skinwalker is gone in moments.
    private static final double FLEE_SPEED = 2.0;
    // How far each fresh escape point is picked from the player it's running from.
    private static final int FLEE_RANGE = 32;
    // Enraged by the Toy Gun: chance per surviving Dust Ball hit, how long the charge lasts, and how much
    // faster it runs meanwhile (+60% is ~16 blocks/s, well beyond any player).
    private static final float ENRAGE_CHANCE = 0.1F;
    // The frozen, screaming wind-up before the charge (1.5 seconds), with a second scream halfway through.
    private static final int WINDUP_TICKS = 30;
    private static final int RAGE_TICKS = 100;
    private static final AttributeModifier RAGE_SPEED = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "skinwalker_rage"), 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    private boolean huntsOnSight;
    private int saltHits;
    private int dustBallHits;
    private int fleeTicks;
    private int windupTicks;
    private int rageTicks;
    // Who scared it; not saved, so after a reload it runs from the nearest player instead.
    @Nullable
    private Player scaredBy;

    public SkinwalkerEntity(EntityType<? extends Monster> pType, Level pLevel) {
        super(pType, pLevel);
        // Rolled once when created; a saved Skinwalker keeps its choice (see readAdditionalSaveData).
        this.huntsOnSight = this.random.nextFloat() < HUNTS_ON_SIGHT_CHANCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                // ~6.4 blocks/s when chasing: faster than a sprinting player on flat ground (~5.6) and a baby
                // zombie (~5.2), just under a sprint-jumping player (~7), so only sprint-jumping escapes it.
                // (Mob ground speed scales with this value squared.)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new WindupGoal());
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> this.huntsOnSight && !this.isFleeing()));
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.level().isClientSide() || this.isInvulnerableTo(pSource)) {
            return super.hurt(pSource, pAmount);
        }
        boolean salt = pSource.getDirectEntity() instanceof SaltProjectile;
        boolean dustBall = pSource.getDirectEntity() instanceof DustBallProjectile;
        if (dustBall) {
            if (++this.dustBallHits >= DUST_BALL_HITS_TO_KILL) {
                pAmount = this.getHealth() + this.getMaxHealth();
            }
        } else if (salt) {
            if (++this.saltHits >= SALT_HITS_TO_FLEE) {
                this.saltHits = 0;
                this.scream();
                this.startFleeing(pSource.getEntity() instanceof Player player ? player : null);
            }
        } else if (!pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            pAmount *= NORMAL_DAMAGE_MULTIPLIER;
        }

        boolean hurt = super.hurt(pSource, pAmount);
        if (hurt && dustBall && this.isAlive() && pSource.getEntity() instanceof Player shooter
                && this.random.nextFloat() < ENRAGE_CHANCE) {
            this.scream();
            this.enrage(shooter);
            return true;
        }
        // Salt only counts toward its own three-hit flee, it doesn't provoke the scream-and-decide reaction.
        if (hurt && !salt && this.isAlive() && !this.isFleeing() && pSource.getEntity() instanceof Player player && this.getTarget() != player) {
            this.scream();
            if (this.random.nextBoolean()) {
                this.startFleeing(player);
            } else {
                this.setTarget(player);
            }
        }
        return hurt;
    }

    // Fear wins over rage: scaring it off (e.g. a third Salt hit) cancels a wind-up or charge.
    private void startFleeing(@Nullable Player from) {
        this.windupTicks = 0;
        this.rageTicks = 0;
        this.fleeTicks = FLEE_TICKS;
        this.scaredBy = from;
        this.setTarget(null);
    }

    public boolean isFleeing() {
        return this.fleeTicks > 0;
    }

    private void enrage(Player target) {
        this.fleeTicks = 0;
        this.scaredBy = null;
        this.rageTicks = 0;
        this.windupTicks = WINDUP_TICKS;
        this.setTarget(target);
    }

    // Holds it in place, staring at its target, while it winds up to charge. Its MOVE and LOOK flags keep
    // the attack and wander goals from running meanwhile.
    private class WindupGoal extends Goal {
        WindupGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return SkinwalkerEntity.this.windupTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            self.getNavigation().stop();
            self.setDeltaMovement(0.0, self.getDeltaMovement().y, 0.0);
            LivingEntity target = self.getTarget();
            if (target != null) {
                self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    // Runs flat out away from whoever scared it for the whole flee time, picking a fresh escape point far
    // from them whenever it arrives or every second, even when it can no longer see them.
    private class FleeGoal extends Goal {
        private int repathCooldown;

        FleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return SkinwalkerEntity.this.isFleeing();
        }

        @Override
        public void start() {
            this.repathCooldown = 0;
        }

        @Override
        public void stop() {
            SkinwalkerEntity.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            if (--this.repathCooldown > 0 && !self.getNavigation().isDone()) {
                return;
            }
            this.repathCooldown = 20;
            Player from = self.scaredBy;
            if (from == null || !from.isAlive() || from.level() != self.level()) {
                from = self.level().getNearestPlayer(self, FLEE_RANGE * 2);
            }
            if (from == null) {
                return;
            }
            Vec3 away = DefaultRandomPos.getPosAway(self, FLEE_RANGE, 10, from.position());
            if (away != null) {
                self.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }

    // Placeholder: a deep, slowed enderman scream until the mod has its own sounds.
    private void scream() {
        this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.5F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
        }
        if (!this.level().isClientSide() && this.windupTicks > 0) {
            this.windupTicks--;
            if (this.windupTicks == WINDUP_TICKS / 2) {
                this.scream();
            }
            if (this.windupTicks == 0) {
                this.rageTicks = RAGE_TICKS;
            }
        }
        if (!this.level().isClientSide()) {
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (this.rageTicks > 0) {
                this.rageTicks--;
                speed.addOrUpdateTransientModifier(RAGE_SPEED);
            } else {
                speed.removeModifier(RAGE_SPEED.id());
            }
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        super.setTarget(this.isFleeing() ? null : pTarget);
    }

    // Placeholder sounds (deep, slowed enderman) until the mod has its own.
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("HuntsOnSight", this.huntsOnSight);
        pCompound.putInt("SaltHits", this.saltHits);
        pCompound.putInt("DustBallHits", this.dustBallHits);
        pCompound.putInt("FleeTicks", this.fleeTicks);
        pCompound.putInt("WindupTicks", this.windupTicks);
        pCompound.putInt("RageTicks", this.rageTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.huntsOnSight = pCompound.getBoolean("HuntsOnSight");
        this.saltHits = pCompound.getInt("SaltHits");
        this.dustBallHits = pCompound.getInt("DustBallHits");
        this.fleeTicks = pCompound.getInt("FleeTicks");
        this.windupTicks = pCompound.getInt("WindupTicks");
        this.rageTicks = pCompound.getInt("RageTicks");
    }
}
