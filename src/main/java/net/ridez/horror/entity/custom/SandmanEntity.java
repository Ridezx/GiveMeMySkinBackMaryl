package net.ridez.horror.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.ModItems;
import net.ridez.horror.item.custom.TacticalVestItem;
import net.ridez.horror.item.custom.ToyGunItem;
import net.ridez.horror.worldgen.HolyPlaces;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The Sandman: the drug squad, a ghost in a bulletproof vest with a head of cold fire (model: art/tools/sandman_model.py).
// Merciless about drugs and nothing else:
// - Anyone carrying anything in the horrormod:contraband item tag (Dust, Dust Balls, Dust Bricks, Crank, Heroine,
//   Speedy Car, Grape Hugs; also a loaded Toy Gun, and whatever is in a Tactical Vest's pockets) gets chased and
//   attacked, however little they have. Being caught, hurting or killing a Sandman and every trade with Gustav,
//   Walter or Frank adds to a player's record (killing a Skinwalker takes some off; see SandmanRecordEvents): with any record at all, every Sandman hunts them from then on, contraband or not, even
//   after death; turning their life around changes nothing. The bigger it is, the harder they're sought: from
//   RECORD_SOUGHT up, Sandmen are sent after them out in the open world (SandmanSpawner).
//   Creative and spectator players are left alone, and so is everyone without a record.
// - Whatever is in the horrormod:sandman_hunts entity tag (Skinwalkers, pillagers, and Hollows once they exist) is
//   attacked on sight. (A Skinwalker she hits screams and flees.)
// Every 10-14 seconds she can stop, raise her Toy Gun and fire (as accurate as a skeleton, much faster; see
// SandmanBullet for its damage and the bleeding); up close she strikes with the gun's stock (1 heart). She opens
// wooden doors; a closed iron door in her way she shoots apart. She spawns in villages and outside pillager outposts and woodland mansions (SandmanSpawner).
// Around holy places (HolyPlaces: in their grounds or within HOLY_PEACE blocks of them) she keeps the peace: she starts
// nothing there and drops any fight with a mob; a player she was already chasing she follows in, and a player who
// hurts her there she goes after. On patrol she doesn't head into them (though, unlike a Skinwalker, she'll cross
// their edges). 18 hearts: three critical hits of a netherite sword.
public class SandmanEntity extends PathfinderMob implements GeoEntity {
    public static final TagKey<net.minecraft.world.item.Item> CONTRABAND = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "contraband"));
    public static final TagKey<EntityType<?>> HUNTS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "sandman_hunts"));

    private static final int SHOT_COOLDOWN_MIN = 10 * 20, SHOT_COOLDOWN_MAX = 14 * 20;
    private static final int FIRST_SHOT_MIN = 30, FIRST_SHOT_MAX = 90;     // once she has a target, the first comes soon
    private static final int AIM_TICKS = 14;                               // raising the gun and taking aim
    private static final double SHOT_RANGE = 28.0;
    private static final float BULLET_SPEED = 3.5F;                        // a skeleton's arrow is 1.6
    private static final int STRIKE_COOLDOWN = 50;                        // a light strike with the stock, every 2.5 s
    private static final double STRIKE_REACH = 2.4;
    private static final double CHASE_SPEED = 1.15, PATROL_SPEED = 0.35;
    private static final int PATROL_INTERVAL = 400;                        // on patrol she sets off about every 20 s
    // A player's record (points in their persistent data, kept through death by SandmanRecordEvents): above 0, every
    // Sandman hunts them; from RECORD_SOUGHT, Sandmen come looking for them anywhere (SandmanSpawner).
    public static final String RECORD = "horrormod_sandman_record";
    public static final int RECORD_TRADE = 1, RECORD_HURT = 2, RECORD_CAUGHT = 5, RECORD_KILL_SANDMAN = 10;
    public static final int RECORD_KILL_SKINWALKER = -5;
    public static final int RECORD_SOUGHT = 10;
    private static final double DOOR_SEARCH = 3.0;                         // iron doors this close, when she can't see her target
    private static final int DEATH_TICKS = 40;                             // collapsing and fading away
    private static final int HOLY_PEACE = 24;

    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(SandmanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SandmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_COUNT = SynchedEntityData.defineId(SandmanEntity.class, EntityDataSerializers.INT);
    private static final List<String> ACTIONS = List.of("shoot", "strike", "hurt");
    private static final float[] ACTION_SECONDS = {0.5F, 0.7F, 0.35F};

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int shotCooldown;
    private int aimTicks;
    private int strikeCooldown;
    @Nullable
    private BlockPos doorTarget;                                           // the iron door she's taking aim at
    // client: the one-off action being shown and when it started
    @Nullable
    private String shownAction;
    private int actionStartedAt;
    private int shownActionCount;
    private boolean actionCountSeen;
    private boolean nearHoly;                                              // (checked once a second)
    private int nextBlinkAt = -1, blinkStartedAt = -100;

    public SandmanEntity(EntityType<? extends PathfinderMob> pType, Level pLevel) {
        super(pType, pLevel);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.toy_gun.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)                                 // three critical hits of a netherite sword
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);                              // (the stock: 1 heart; the gun is the threat)
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(AIMING, false);
        pBuilder.define(ACTION, -1);
        pBuilder.define(ACTION_COUNT, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, pLevel);
        navigation.setCanOpenDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new AttackGoal());
        WaterAvoidingRandomStrollGoal patrol = new WaterAvoidingRandomStrollGoal(this, PATROL_SPEED) {
            @Nullable
            @Override
            protected Vec3 getPosition() {                  // (on patrol she skirts holy places, not into them)
                Vec3 to = super.getPosition();
                return to != null && SandmanEntity.this.level() instanceof ServerLevel level
                        && HolyPlaces.inside(level, BlockPos.containing(to)) ? null : to;
            }
        };
        patrol.setInterval(PATROL_INTERVAL);
        this.goalSelector.addGoal(5, patrol);
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {                    // (even there, a player who hurts her gets what's coming)
                return super.canUse() && (!SandmanEntity.this.nearHoly || SandmanEntity.this.getLastHurtByMob() instanceof Player);
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> !this.nearHoly && player instanceof Player p && !p.isCreative() && !p.isSpectator() && (hasRecord(p) || carriesContraband(p))));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> !this.nearHoly && entity.getType().is(HUNTS)));
    }

    // ---------------------------------------------------------------- what she's after
    // Any contraband at all, anywhere on them: the inventory, armor and off hand, a loaded Toy Gun, a vest's pockets.
    public static boolean carriesContraband(Player pPlayer) {
        if (pPlayer.isCreative() || pPlayer.isSpectator()) {
            return false;
        }
        Inventory inventory = pPlayer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isContraband(inventory.getItem(i))) {
                return true;
            }
        }
        for (ItemStack pocket : TacticalVestItem.pockets(pPlayer.getItemBySlot(EquipmentSlot.CHEST))) {
            if (isContraband(pocket)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContraband(ItemStack pStack) {
        return pStack.is(CONTRABAND) || pStack.getItem() instanceof ToyGunItem && ToyGunItem.isLoaded(pStack);
    }

    // (A record from before it counted points was saved as true: it reads as 1.)
    public static int getRecord(Player pPlayer) {
        return pPlayer.getPersistentData().getInt(RECORD);
    }

    public static boolean hasRecord(Player pPlayer) {
        return getRecord(pPlayer) > 0;
    }

    // (Never below 0.)
    public static void addToRecord(Player pPlayer, int pPoints) {
        pPlayer.getPersistentData().putInt(RECORD, Math.max(0, getRecord(pPlayer) + pPoints));
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        LivingEntity after = this.getTarget();
        boolean hurt = super.hurt(pSource, pAmount);
        if (hurt && !this.level().isClientSide() && this.isAlive()) {
            if (pSource.getEntity() instanceof Player player && !player.isCreative() && after != player) {
                addToRecord(player, RECORD_HURT);            // attacking a Sandman (once per fight, not per hit)
            }
            if (this.aimTicks == 0) {
                this.playAction("hurt");
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            // a cold flame flickers off the top of her head now and then
            if (this.isAlive() && this.random.nextInt(8) == 0) {
                Vec3 top = this.position().add(0.0, this.getBbHeight() * 0.95, 0.0);
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, top.x + (this.random.nextDouble() - 0.5) * 0.2, top.y,
                        top.z + (this.random.nextDouble() - 0.5) * 0.2, 0.0, 0.02, 0.0);
            }
            return;
        }
        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel level) {
            this.nearHoly = HolyPlaces.near(level, this.blockPosition(), HOLY_PEACE);
            if (this.nearHoly && this.getTarget() != null && !(this.getTarget() instanceof Player)) {
                this.setTarget(null);                        // keeping the peace (a player she's chasing, she follows in)
            }
        }
        if (this.shotCooldown > 0) {
            this.shotCooldown--;
        }
        if (this.strikeCooldown > 0) {
            this.strikeCooldown--;
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        if (pTarget instanceof Player player && !this.level().isClientSide() && this.getTarget() != player && carriesContraband(player)) {
            addToRecord(player, RECORD_CAUGHT);              // caught with it: it goes on their record
        }
        if (pTarget != null && this.getTarget() != pTarget && this.shotCooldown < FIRST_SHOT_MIN) {
            this.shotCooldown = Mth.nextInt(this.random, FIRST_SHOT_MIN, FIRST_SHOT_MAX);
        }
        super.setTarget(pTarget);
    }

    // ---------------------------------------------------------------- shooting and striking
    private void shoot(LivingEntity pTarget) {
        SandmanBullet bullet = new SandmanBullet(this.level(), this);
        double dx = pTarget.getX() - this.getX(), dz = pTarget.getZ() - this.getZ();
        double dy = pTarget.getY(0.45) - bullet.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float inaccuracy = 14 - this.level().getDifficulty().getId() * 4;       // a skeleton's
        bullet.shoot(dx, dy + horizontal * 0.06, dz, BULLET_SPEED, inaccuracy);
        this.level().addFreshEntity(bullet);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE,
                1.2F, 1.35F + this.random.nextFloat() * 0.2F);
        if (this.level() instanceof ServerLevel level) {
            Vec3 look = new Vec3(dx, dy, dz).normalize();
            Vec3 muzzle = this.getEyePosition().add(look.scale(0.9)).subtract(0.0, 0.25, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 5, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 4, 0.04, 0.04, 0.04, 0.02);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);
        }
        this.playAction("shoot");
        this.shotCooldown = Mth.nextInt(this.random, SHOT_COOLDOWN_MIN, SHOT_COOLDOWN_MAX);
    }

    // A closed iron door between her and her target (on the line to them, or, when she can't see them, right beside
    // her), or null.
    @Nullable
    private BlockPos ironDoorInTheWay(LivingEntity pTarget, boolean pClear) {
        if (!pClear) {
            BlockHitResult hit = this.level().clip(new ClipContext(this.getEyePosition(), pTarget.getEyePosition(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK && isClosedIronDoor(hit.getBlockPos())
                    && hit.getLocation().distanceToSqr(this.getEyePosition()) < 16.0 * 16.0) {
                return lowerHalf(hit.getBlockPos());
            }
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        if (!pClear) {
            BlockPos here = this.blockPosition();
            int r = (int) Math.ceil(DOOR_SEARCH);
            for (BlockPos p : BlockPos.betweenClosed(here.offset(-r, -1, -r), here.offset(r, 2, r))) {
                if (isClosedIronDoor(p)) {
                    double d = p.distToCenterSqr(this.position());
                    // only one that's toward her target
                    if (d < bestDistance && p.distToCenterSqr(pTarget.position()) < this.distanceToSqr(pTarget)) {
                        best = lowerHalf(p.immutable());
                        bestDistance = d;
                    }
                }
            }
        }
        return best;
    }

    private boolean isClosedIronDoor(BlockPos pPos) {
        BlockState state = this.level().getBlockState(pPos);
        return state.getBlock() instanceof DoorBlock door && !door.type().canOpenByHand() && !state.getValue(DoorBlock.OPEN);
    }

    private BlockPos lowerHalf(BlockPos pPos) {
        return this.level().getBlockState(pPos).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pPos.below() : pPos;
    }

    // The shot that breaks an iron door: it's blown apart (and drops, like a broken door).
    private void shootDoor(BlockPos pDoor) {
        if (!isClosedIronDoor(pDoor)) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(pDoor.above());
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE,
                1.2F, 1.35F + this.random.nextFloat() * 0.2F);
        this.level().playSound(null, pDoor, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 0.7F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 10, 0.3, 0.5, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 4, 0.2, 0.4, 0.2, 0.01);
        }
        this.level().destroyBlock(pDoor, true, this);
        this.playAction("shoot");
        this.shotCooldown = Math.max(this.shotCooldown, 40);
    }

    private void strike(LivingEntity pTarget) {
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.doHurtTarget(pTarget);
        this.playAction("strike");
        this.strikeCooldown = STRIKE_COOLDOWN;
    }

    // Goes after her target; when the gun's ready and she has a clear shot, stops, takes aim and fires.
    private class AttackGoal extends Goal {
        private int repath;

        AttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = SandmanEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.repath = 0;
            SandmanEntity.this.setAggressive(true);         // (her eyes narrow: see animateEyes)
        }

        @Override
        public void stop() {
            SandmanEntity self = SandmanEntity.this;
            self.setAggressive(false);
            self.aimTicks = 0;
            self.doorTarget = null;
            self.entityData.set(AIMING, false);
            self.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SandmanEntity self = SandmanEntity.this;
            LivingEntity target = self.getTarget();
            if (target == null) {
                return;
            }
            double distance = self.distanceToSqr(target);
            boolean clear = self.getSensing().hasLineOfSight(target);
            if (self.doorTarget != null) {
                self.getLookControl().setLookAt(Vec3.atCenterOf(self.doorTarget.above()));
            } else {
                self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (self.aimTicks > 0) {                         // taking aim: standing still
                self.getNavigation().stop();
                if (--self.aimTicks == 0) {
                    if (self.doorTarget != null) {
                        self.shootDoor(self.doorTarget);
                        self.doorTarget = null;
                    } else if (clear) {
                        self.shoot(target);
                    }
                    self.entityData.set(AIMING, false);
                }
                return;
            }
            if (self.tickCount % 10 == 0) {                  // an iron door in the way: she shoots it apart
                BlockPos door = self.ironDoorInTheWay(target, clear);
                if (door != null) {
                    self.doorTarget = door;
                    self.aimTicks = AIM_TICKS;
                    self.entityData.set(AIMING, true);
                    self.getNavigation().stop();
                    return;
                }
            }
            if (self.shotCooldown <= 0 && clear && distance < SHOT_RANGE * SHOT_RANGE) {
                self.aimTicks = AIM_TICKS;
                self.entityData.set(AIMING, true);
                self.getNavigation().stop();
                return;
            }
            double reach = STRIKE_REACH + target.getBbWidth() / 2.0;
            if (distance < reach * reach && self.strikeCooldown <= 0) {
                self.strike(target);
            }
            if (--this.repath <= 0) {
                this.repath = 10;
                self.getNavigation().moveTo(target, CHASE_SPEED);
            }
        }
    }

    // ---------------------------------------------------------------- death: she collapses and fades away
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6),
                        0.0, 0.03, 0.0);
            }
        }
        if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // Client: how far she has faded (1 solid, 0 gone).
    public float getFade(float pPartialTick) {
        if (!this.isDeadOrDying()) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - Math.max(0.0F, this.deathTime + pPartialTick - 12.0F) / (DEATH_TICKS - 12.0F), 0.0F, 1.0F);
    }

    // ---------------------------------------------------------------- animation (art/tools/sandman_model.py makes them)
    private void playAction(String pName) {
        this.entityData.set(ACTION, ACTIONS.indexOf(pName));
        this.entityData.set(ACTION_COUNT, this.entityData.get(ACTION_COUNT) + 1);
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation AIM = RawAnimation.begin().thenPlayAndHold("aim");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation FLICKER = RawAnimation.begin().thenLoop("flicker");
    private static final RawAnimation EYES_OPEN = RawAnimation.begin().thenLoop("eyes_open");
    private static final RawAnimation BLINK = RawAnimation.begin().thenPlayAndHold("blink");
    private static final RawAnimation SQUINT = RawAnimation.begin().thenLoop("squint");
    private static final int BLINK_MIN = 15 * 20, BLINK_MAX = 25 * 20, BLINK_TICKS = 5;
    private static final Map<String, RawAnimation> ONE_OFF = new HashMap<>();
    private static final double WALK_ANIM_SPEED = 0.07, RUN_ANIM_SPEED = 0.17;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::animate));
        controllers.add(new AnimationController<>(this, "flame", 0, state -> state.setAndContinue(FLICKER)));
        controllers.add(new AnimationController<>(this, "eyes", 2, this::animateEyes));
    }

    // Her eyes: narrowed while she goes after someone, focused on them; otherwise a blink every 15-25 seconds.
    private PlayState animateEyes(AnimationState<SandmanEntity> state) {
        if (this.nextBlinkAt < 0) {
            this.nextBlinkAt = this.tickCount + Mth.nextInt(this.random, BLINK_MIN, BLINK_MAX);
        }
        if (this.isAggressive() && !this.isDeadOrDying()) {
            return state.setAndContinue(SQUINT);
        }
        if (this.tickCount >= this.nextBlinkAt) {
            this.blinkStartedAt = this.tickCount;
            this.nextBlinkAt = this.tickCount + Mth.nextInt(this.random, BLINK_MIN, BLINK_MAX);
        }
        return state.setAndContinue(this.tickCount - this.blinkStartedAt < BLINK_TICKS ? BLINK : EYES_OPEN);
    }

    private PlayState animate(AnimationState<SandmanEntity> state) {
        state.setControllerSpeed(1.0F);
        if (this.isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        int count = this.entityData.get(ACTION_COUNT);
        if (!this.actionCountSeen) {
            this.actionCountSeen = true;
            this.shownActionCount = count;
        }
        if (count != this.shownActionCount) {
            this.shownActionCount = count;
            int index = this.entityData.get(ACTION);
            this.shownAction = index >= 0 ? ACTIONS.get(index) : null;
            this.actionStartedAt = this.tickCount;
            state.getController().forceAnimationReset();
        }
        if (this.shownAction != null) {
            if (this.tickCount - this.actionStartedAt < Mth.ceil(ACTION_SECONDS[ACTIONS.indexOf(this.shownAction)] * 20)) {
                return state.setAndContinue(ONE_OFF.computeIfAbsent(this.shownAction, n -> RawAnimation.begin().thenPlayAndHold(n)));
            }
            this.shownAction = null;
        }
        if (this.entityData.get(AIMING)) {
            return state.setAndContinue(AIM);
        }
        double speed = Math.sqrt(Mth.square(this.getX() - this.xo) + Mth.square(this.getZ() - this.zo));
        if (speed > 0.015) {
            boolean running = speed > 0.12;
            state.setControllerSpeed((float) Mth.clamp(speed / (running ? RUN_ANIM_SPEED : WALK_ANIM_SPEED), 0.6, 1.8));
            return state.setAndContinue(running ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
