package net.ridez.horror.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.dimension.ChildsPlayroom;
import net.ridez.horror.item.ModItems;
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

// The Angel: a great ring with one eye and six wings (model and animations: art/tools/angel_model.py). It hangs high
// in the air, 30-50 blocks above the ground (its own height, picked once) and always clear of everything within 30
// blocks to its sides (above the highest ground around), so it can't be jumped onto from a mountain; always turned
// to look at the nearest player.
// - A player building up toward it (placing blocks near it, high enough to be climbing to it) makes it fly off away
//   from them; the third time, it rises into the sky and is gone.
// - Shot with the Toy Gun or the Straw (a Dust Ball or salt hitting it), it flees: it flies off away from whoever
//   shot it, rising into the sky, and is gone. Nothing hurts it.
// - A Holy Text thrown on the ground (lying there, within HOLY_TEXT_RANGE) brings it down, wherever it is: it comes
//   down onto the book and takes it (if someone picks the book up first, it goes back up), and there, where the book
//   was, it widens its ring onto a white portal and stands there like a standing mirror for 25 seconds, then
//   closes and flies away into the sky for good. A player who jumps into the portal goes through to its dimension,
//   the Child's Playroom (ChildsPlayroom).
// No natural spawning yet (its spawn egg only).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class AngelEntity extends PathfinderMob implements GeoEntity {
    // what it's doing (synced: the animations follow it)
    public static final int HOVER = 0, MOVING = 1, DESCENDING = 3, OPENING = 4, OPEN = 5, CLOSING = 6, LEAVING = 7;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AngelEntity.class, EntityDataSerializers.INT);

    private static final int HOVER_MIN = 30, HOVER_MAX = 50;              // how high it hangs above the ground
    private static final double CLEAR = 30.0;                              // nothing this close to its sides reaches its height
    private static final double CLEAR_ABOVE = 4.0;                         // (it stays this much above the highest ground around)
    private static final double LOOK_RANGE = 64.0;
    private static final double BUILD_RANGE = 12.0, BUILD_BELOW = 16.0;    // blocks placed this close (across), no lower than this under it
    private static final double MOVE_AWAY = 20.0;                          // how far it goes off from a player building to it
    private static final int MOVES_BEFORE_LEAVING = 3;
    private static final double HOLY_TEXT_RANGE = 48.0;
    private static final int OPENING_TICKS = 40, OPEN_TICKS = 25 * 20, CLOSING_TICKS = 30, LEAVING_TICKS = 160;
    private static final double MOVE_SPEED = 0.35, DESCEND_SPEED = 0.3, LEAVE_SPEED = 0.6;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int moves;                                                     // times it has moved off from someone building
    private int hoverAbove = -1;                                           // its height above the ground (picked once)
    @Nullable
    private java.util.UUID book;                                           // the Holy Text it's coming down for
    private int stateTicks;
    @Nullable
    private Vec3 goal;                                                     // where it's flying to
    // client: when it next blinks, and when the last blink started
    private int nextBlinkAt = -1, blinkStartedAt = -100;

    public AngelEntity(EntityType<? extends PathfinderMob> pType, Level pLevel) {
        super(pType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(STATE, HOVER);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int pState) {
        this.entityData.set(STATE, pState);
        this.stateTicks = 0;
    }

    // Standing on the ground as a portal (opening, open or closing): it doesn't tip its ring to look up or down.
    public boolean isStanding() {
        int state = this.getState();
        return state == OPENING || state == OPEN || state == CLOSING;
    }

    // ---------------------------------------------------------------- nothing hurts it; being shot at sends it up high
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(pSource, pAmount);                           // (/kill, the void)
        }
        if (!this.level().isClientSide() && (pSource.getDirectEntity() instanceof DustBallProjectile || pSource.getDirectEntity() instanceof SaltProjectile)) {
            this.flee(pSource.getEntity() != null ? pSource.getEntity() : pSource.getDirectEntity());
        }
        return false;
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    // Shot at: off away from the shooter and up into the sky, gone.
    private void flee(@Nullable Entity pFrom) {
        if (this.getState() == LEAVING || this.isStanding()) {
            return;
        }
        Vec3 away = pFrom != null ? this.position().subtract(pFrom.position()).multiply(1.0, 0.0, 1.0) : Vec3.ZERO;
        away = away.lengthSqr() < 1.0e-4 ? Vec3.ZERO : away.normalize();
        this.goal = this.position().add(away.scale(80.0)).add(0.0, 120.0, 0.0);
        this.setState(LEAVING);
    }

    private double groundAt(double pX, double pZ) {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(pX), Mth.floor(pZ));
    }

    // How high it hangs over (x, z): `above` blocks over the ground there, and over everything within CLEAR blocks
    // around it (so nothing at its sides reaches it), under the build limit.
    private double heightAt(double pX, double pZ, double pAbove) {
        double y = this.groundAt(pX, pZ) + pAbove;
        int r = (int) CLEAR;
        for (int dx = -r; dx <= r; dx += 3) {
            for (int dz = -r; dz <= r; dz += 3) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                BlockPos at = BlockPos.containing(pX + dx, 0, pZ + dz);
                if (this.level().hasChunkAt(at)) {
                    y = Math.max(y, this.groundAt(at.getX(), at.getZ()) + CLEAR_ABOVE);
                }
            }
        }
        return Math.min(y, this.level().getMaxBuildHeight() - 4);
    }

    private double hoverHeightAt(double pX, double pZ) {
        if (this.hoverAbove < 0) {
            this.hoverAbove = Mth.nextInt(this.random, HOVER_MIN, HOVER_MAX);
        }
        return this.heightAt(pX, pZ, this.hoverAbove);
    }

    // ---------------------------------------------------------------- a player building up toward it
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getLevel() instanceof ServerLevel level) || player.isSpectator()) {
            return;
        }
        BlockPos pos = event.getPos();
        for (AngelEntity angel : level.getEntitiesOfClass(AngelEntity.class, new AABB(pos).inflate(BUILD_RANGE, 64.0, BUILD_RANGE))) {
            double across = Math.sqrt(Mth.square(angel.getX() - pos.getX() - 0.5) + Mth.square(angel.getZ() - pos.getZ() - 0.5));
            if (across < BUILD_RANGE && pos.getY() > angel.getY() - BUILD_BELOW) {
                angel.builtToward(player);
            }
        }
    }

    private void builtToward(Player pPlayer) {
        int state = this.getState();
        if (state != HOVER) {
            return;                                                        // (already going, or standing open)
        }
        if (++this.moves >= MOVES_BEFORE_LEAVING) {
            this.leave();
            return;
        }
        Vec3 away = this.position().subtract(pPlayer.position()).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0e-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        Vec3 to = this.position().add(away.scale(MOVE_AWAY));
        this.goal = new Vec3(to.x, Math.max(this.hoverHeightAt(to.x, to.z), pPlayer.getY() + 6.0), to.z);
        this.setState(MOVING);
    }

    private void leave() {
        this.goal = this.position().add(0.0, 200.0, 0.0);
        this.setState(LEAVING);
    }

    // ---------------------------------------------------------------- a Holy Text on the ground brings it down
    @Nullable
    private ItemEntity holyTextNear() {
        AABB around = this.getBoundingBox().inflate(HOLY_TEXT_RANGE, 256.0, HOLY_TEXT_RANGE);
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, around,
                e -> e.isAlive() && e.onGround() && e.getItem().is(ModItems.holy_text.get()))) {
            return item;
        }
        return null;
    }

    // Down onto the book, where it lies.
    private void comeDownTo(ItemEntity pBook) {
        this.book = pBook.getUUID();
        this.goal = new Vec3(pBook.getX(), Math.floor(pBook.getY() + 0.05), pBook.getZ());
        this.setState(DESCENDING);
    }

    @Nullable
    private ItemEntity bookStillThere() {
        if (this.book != null && this.level() instanceof ServerLevel level && level.getEntity(this.book) instanceof ItemEntity item
                && item.isAlive()) {
            return item;
        }
        return null;
    }

    // ---------------------------------------------------------------- each tick
    @Override
    public void tick() {
        super.tick();
        this.lookAtPlayer();
        if (this.level().isClientSide()) {
            return;
        }
        this.stateTicks++;
        int state = this.getState();
        if (state != DESCENDING && !this.isStanding() && state != LEAVING && this.tickCount % 20 == 0) {
            ItemEntity book = this.holyTextNear();
            if (book != null) {
                this.comeDownTo(book);
                state = DESCENDING;
            }
        }
        switch (state) {
            case HOVER -> {
                if (this.goal == null || this.stateTicks % 40 == 1) {         // (its height kept up to date with the ground around)
                    double y = this.hoverHeightAt(this.getX(), this.getZ());
                    this.goal = new Vec3(this.getX(), this.goal != null ? Math.max(this.goal.y, y) : y, this.getZ());
                }
                this.flyToward(this.goal, Math.abs(this.goal.y - this.getY()) > 2.0 ? 0.3 : 0.08);
            }
            case MOVING -> {
                if (this.flyToward(this.goal, MOVE_SPEED)) {
                    this.setState(HOVER);
                }
            }
            case DESCENDING -> {
                ItemEntity item = this.bookStillThere();
                if (item == null) {                                        // the book's gone: back up
                    this.book = null;
                    this.goal = new Vec3(this.getX(), this.hoverHeightAt(this.getX(), this.getZ()), this.getZ());
                    this.setState(MOVING);
                } else if (this.flyToward(this.goal, DESCEND_SPEED)) {
                    item.discard();                                        // it takes the book
                    this.book = null;
                    this.setDeltaMovement(Vec3.ZERO);
                    this.setPos(this.goal);
                    this.setState(OPENING);
                    this.playSound(ModSounds.angel_open.get(), 2.0F, 1.0F);
                }
            }
            case OPENING -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (this.stateTicks >= OPENING_TICKS) {
                    this.setState(OPEN);
                }
            }
            case OPEN -> {
                this.setDeltaMovement(Vec3.ZERO);
                this.takeThroughPortal();
                if (this.stateTicks >= OPEN_TICKS) {
                    this.setState(CLOSING);
                    this.playSound(ModSounds.angel_close.get(), 2.0F, 1.0F);
                }
            }
            case CLOSING -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (this.stateTicks >= CLOSING_TICKS) {
                    this.leave();
                }
            }
            case LEAVING -> {
                this.flyToward(this.goal, LEAVE_SPEED);
                if (this.stateTicks >= LEAVING_TICKS) {
                    this.discard();                                        // gone into the sky
                }
            }
            default -> {
            }
        }
        if ((state == MOVING || state == LEAVING) && this.stateTicks % 12 == 0) {
            this.playSound(ModSounds.angel_wings.get(), 2.5F, 0.8F + this.random.nextFloat() * 0.2F);
        }
    }

    // A player who jumps into the open portal (the white disc standing in its ring) goes through to the Child's
    // Playroom.
    private static final double PORTAL_MIDDLE = 1.2, PORTAL_HALF = 0.8;    // (the white disc's middle above its feet, half its size)

    private void takeThroughPortal() {
        AABB portal = new AABB(this.getX() - PORTAL_HALF, this.getY() + PORTAL_MIDDLE - PORTAL_HALF, this.getZ() - PORTAL_HALF,
                this.getX() + PORTAL_HALF, this.getY() + PORTAL_MIDDLE + PORTAL_HALF, this.getZ() + PORTAL_HALF);
        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, portal,
                p -> p.isAlive() && !p.isSpectator() && !p.isOnPortalCooldown())) {
            ChildsPlayroom.sendIn(player);
        }
    }

    // Flies toward a point (easing in as it gets there); true once it's there.
    private boolean flyToward(@Nullable Vec3 pGoal, double pSpeed) {
        if (pGoal == null) {
            return true;
        }
        Vec3 to = pGoal.subtract(this.position());
        double dist = to.length();
        if (dist < 0.4) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            return true;
        }
        Vec3 want = to.scale(Math.min(pSpeed, dist * 0.25) / dist);
        this.setDeltaMovement(this.getDeltaMovement().add(want.subtract(this.getDeltaMovement()).scale(0.2)));
        this.move(MoverType.SELF, this.getDeltaMovement());
        return false;
    }

    // Turned to look at the nearest player; once it has come down and stands open as a portal, it stays still,
    // facing the way it was.
    private void lookAtPlayer() {
        if (this.isStanding()) {
            this.setXRot(0.0F);
            this.yBodyRot = this.yHeadRot = this.getYRot();
            return;
        }
        Player player = this.level().getNearestPlayer(this, LOOK_RANGE);
        if (player == null || player.isSpectator()) {
            return;
        }
        Vec3 eye = this.position().add(0.0, 1.125, 0.0);
        double dx = player.getX() - eye.x, dy = player.getEyeY() - eye.y, dz = player.getZ() - eye.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        this.setYRot(yaw);
        this.setXRot(Mth.clamp(pitch, -80.0F, 80.0F));
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    // (its own flying, not a mob's walking or the vanilla AI)
    @Override
    protected void registerGoals() {
    }

    @Override
    public void travel(Vec3 pTravelVector) {
    }

    // ---------------------------------------------------------------- saving: it comes back hanging in the air
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Moves", this.moves);
        pCompound.putInt("HoverAbove", this.hoverAbove);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.moves = pCompound.getInt("Moves");
        this.hoverAbove = pCompound.contains("HoverAbove") ? pCompound.getInt("HoverAbove") : -1;
    }

    // ---------------------------------------------------------------- animation (art/tools/angel_model.py makes them)
    private static final RawAnimation HOVERING = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation FLYING = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation RISING = RawAnimation.begin().thenLoop("ascend");
    private static final RawAnimation GLIDING_DOWN = RawAnimation.begin().thenLoop("descend");
    private static final RawAnimation OPENING_UP = RawAnimation.begin().thenPlay("open").thenLoop("opened");
    private static final RawAnimation CLOSING_UP = RawAnimation.begin().thenPlayAndHold("close");
    private static final RawAnimation EYE_OPEN = RawAnimation.begin().thenLoop("eye_open");
    private static final RawAnimation BLINK = RawAnimation.begin().thenPlayAndHold("blink");
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private static final int BLINK_MIN = 6 * 20, BLINK_MAX = 12 * 20, BLINK_TICKS = 10;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 8, this::animate));
        controllers.add(new AnimationController<>(this, "eye", 2, this::animateEye));
        controllers.add(new AnimationController<>(this, "spin", 0, state -> state.setAndContinue(SPIN)));
    }

    private PlayState animate(AnimationState<AngelEntity> state) {
        return state.setAndContinue(switch (this.getState()) {
            case MOVING -> FLYING;
            case LEAVING -> RISING;
            case DESCENDING -> GLIDING_DOWN;
            case OPENING, OPEN -> OPENING_UP;
            case CLOSING -> CLOSING_UP;
            default -> HOVERING;
        });
    }

    private PlayState animateEye(AnimationState<AngelEntity> state) {
        if (this.nextBlinkAt < 0) {
            this.nextBlinkAt = this.tickCount + Mth.nextInt(this.random, BLINK_MIN, BLINK_MAX);
        }
        if (this.tickCount >= this.nextBlinkAt) {
            this.blinkStartedAt = this.tickCount;
            this.nextBlinkAt = this.tickCount + Mth.nextInt(this.random, BLINK_MIN, BLINK_MAX);
        }
        return state.setAndContinue(this.tickCount - this.blinkStartedAt < BLINK_TICKS ? BLINK : EYE_OPEN);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
