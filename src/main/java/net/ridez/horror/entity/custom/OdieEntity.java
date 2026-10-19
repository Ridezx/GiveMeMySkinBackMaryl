package net.ridez.horror.entity.custom;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

// Odie, the Hat Man: a tall man in a long dark coat and a wide-brimmed hat, standing on a summit far off and watching
// one player go about their day (he's brought there by OdieSpawner). He never attacks and can't be hurt or touched.
// - Looked at (the player's crosshair on or near him for 2 seconds): he takes off his hat, holds it to his chest, waves slowly and puts the hat back on, then turns round
//   (a smooth half turn, his head first) and walks straight away from the player. Once they can't see him he's gone;
//   if they can still see him after 15 blocks, he dissolves into the air.
// - Chased while he walks away: he crumbles into black dust on the air, so he can never be reached.
// - Approached within 30 blocks: he's simply gone, and the player's screen goes black for a split second with
//   "Careful." in the middle (client/OdieFlash).
// He's never saved with the world: if it unloads, he's gone.
public class OdieEntity extends PathfinderMob implements GeoEntity {
    public static final int WATCHING = 0, WAVING = 1, LEAVING = 2, CRUMBLING = 3, TURNING = 4;
    // Entity event the clients get when he vanishes up close: the watched player's screen flashes.
    public static final byte FLASH_EVENT = 70;
    private static final double TOO_CLOSE = 30.0;
    // Seen: the player's crosshair stays on him or near him (within LOOK_ANGLE degrees, whatever the distance) for
    // 2 seconds in a row, with nothing in the way.
    private static final int SEEN_TICKS = 40;
    private static final double LOOK_ANGLE = 7.0;
    // The lengths of his wave (hat off, wave, hat back on) and turn animations (art/tools/odie_model.py).
    private static final int WAVE_TICKS = 174;
    private static final int TURN_TICKS = 28;
    private static final double LEAVE_DISTANCE = 15.0;
    // Leaving, he crumbles if the player closes in this many blocks on him (chasing).
    private static final double CHASE_GAIN = 6.0;
    public static final int CRUMBLE_TICKS = 80;
    // He doesn't stay forever: after 5 minutes of watching he leaves on his own.
    private static final int MAX_WATCH_TICKS = 6000;
    // About 2 blocks a second: an unhurried walk, but he does get away.
    private static final double LEAVE_SPEED = 0.8;
    // Ground speed (blocks per tick) his walk animation is made for; it plays faster or slower to match, so his feet
    // don't slide.
    private static final double WALK_ANIMATION_SPEED = 0.044;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(OdieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> WATCHED = SynchedEntityData.defineId(OdieEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WAVE = RawAnimation.begin().thenPlayAndHold("wave");
    private static final RawAnimation TURN = RawAnimation.begin().thenPlayAndHold("turn_around");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk_away");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    private int seenTicks;
    @Nullable
    private Vec3 leaveStart;
    // the way he walks off: straight away from the player, as he turned
    private Vec3 leaveDirection = Vec3.ZERO;
    private double leaveStartDistance;
    private int repathIn;
    // client: when he started crumbling, for the fade
    private int crumbleStartedAt = -1;
    // turning round (both sides, so the client turns him as smoothly as the server): when it started, and from where
    private int turnStartedAt;
    private float turnFromYaw;

    public OdieEntity(EntityType<? extends PathfinderMob> pType, Level pLevel) {
        super(pType, pLevel);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noCulling = true;                      // drawn even when the camera's frustum check would skip him
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(STATE, WATCHING);
        pBuilder.define(WATCHED, Optional.empty());
    }

    public void setWatched(UUID pPlayer) {
        this.entityData.set(WATCHED, Optional.of(pPlayer));
    }

    @Nullable
    public UUID getWatched() {
        return this.entityData.get(WATCHED).orElse(null);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int pState) {
        this.entityData.set(STATE, pState);
        this.stateTicks = 0;
    }

    private void startTurn() {
        this.turnStartedAt = this.tickCount;
        this.turnFromYaw = this.getYRot();
    }

    // Turning round to walk away: half a circle over the turn animation, easing in and out, the body following the
    // head (which leads in the animation, toward increasing yaw). Done the same way on both sides.
    private void tickTurn() {
        float u = Mth.clamp((this.tickCount - this.turnStartedAt) / (float) TURN_TICKS, 0.0F, 1.0F);
        float yaw = this.turnFromYaw + 180.0F * u * u * (3.0F - 2.0F * u);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getNavigation().stop();
    }

    // ---------------------------------------------------------------- behaviour (server)
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getState() == TURNING) {
            this.tickTurn();
        }
        if (this.level().isClientSide()) {
            return;
        }
        UUID watched = this.getWatched();
        if (watched == null) {
            // brought in some other way (a spawn egg, /summon): he watches the nearest player
            Player nearest = this.level().getNearestPlayer(this, 128.0);
            if (nearest != null) {
                this.setWatched(nearest.getUUID());
                watched = nearest.getUUID();
            }
        }
        Player player = watched == null ? null : this.level().getPlayerByUUID(watched);
        if (player == null || !player.isAlive() || player.distanceTo(this) > 200.0) {
            this.discard();
            return;
        }
        this.stateTicks++;
        double distance = player.distanceTo(this);
        switch (this.getState()) {
            case WATCHING -> {
                this.face(player);
                if (distance < TOO_CLOSE) {
                    this.flash();
                    return;
                }
                this.seenTicks = this.isLookedAtBy(player) ? this.seenTicks + 1 : 0;
                if (this.seenTicks >= SEEN_TICKS) {
                    this.setState(WAVING);
                } else if (this.stateTicks > MAX_WATCH_TICKS || OdieSpawner.isMidday(this.level())) {
                    this.startLeaving(player);
                }
            }
            case WAVING -> {
                this.face(player);
                if (distance < TOO_CLOSE) {
                    this.flash();
                } else if (this.stateTicks >= WAVE_TICKS) {
                    this.setState(TURNING);
                }
            }
            case TURNING -> {
                if (distance < TOO_CLOSE) {
                    this.flash();
                } else if (this.stateTicks >= TURN_TICKS) {
                    this.startLeaving(player);
                }
            }
            case LEAVING -> this.tickLeaving(player, distance);
            case CRUMBLING -> this.tickCrumbling();
            default -> { }
        }
    }

    // Faces the player he's watching, body and head.
    private void face(Player pPlayer) {
        float yaw = (float) (Mth.atan2(pPlayer.getZ() - this.getZ(), pPlayer.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getNavigation().stop();
    }

    // Whether the player's crosshair is on him or near him (within LOOK_ANGLE of his chest), with nothing in the way.
    private boolean isLookedAtBy(Player pPlayer) {
        Vec3 chest = new Vec3(this.getX(), this.getY() + 1.6, this.getZ());
        Vec3 toHim = chest.subtract(pPlayer.getEyePosition()).normalize();
        if (toHim.dot(pPlayer.getViewVector(1.0F)) < Math.cos(Math.toRadians(LOOK_ANGLE))) {
            return false;
        }
        return this.canBeSeenFrom(pPlayer, chest);
    }

    private boolean canBeSeenFrom(Player pPlayer, Vec3 pPoint) {
        return this.level().clip(new ClipContext(pPlayer.getEyePosition(), pPoint, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, pPlayer)).getType() == HitResult.Type.MISS;
    }

    // Too close: gone in an instant, and the player's screen flashes black with "Careful.".
    private void flash() {
        this.level().broadcastEntityEvent(this, FLASH_EVENT);
        this.discard();
    }

    private void startLeaving(Player pPlayer) {
        this.setState(LEAVING);
        this.leaveStart = this.position();
        Vec3 away = new Vec3(this.getX() - pPlayer.getX(), 0, this.getZ() - pPlayer.getZ());
        this.leaveDirection = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, this.getYRot()) : away.normalize();
        this.leaveStartDistance = pPlayer.distanceTo(this);
        this.repathIn = 0;
    }

    // Walks away from the player at a slow, even pace (turning his back on them), and is gone once they can't see
    // him or he's walked 15 blocks. If they come after him, he crumbles away.
    private void tickLeaving(Player pPlayer, double pDistance) {
        if (pDistance < this.leaveStartDistance - CHASE_GAIN || pDistance < TOO_CLOSE + 5.0) {
            this.setState(CRUMBLING);
            this.getNavigation().stop();
            return;
        }
        boolean walkedFar = this.leaveStart != null && this.position().distanceTo(this.leaveStart) >= LEAVE_DISTANCE;
        boolean outOfSight = this.stateTicks > 20 && !this.canBeSeenFrom(pPlayer, new Vec3(this.getX(), this.getY() + 2.4, this.getZ()));
        if (outOfSight) {
            this.discard();                         // nobody's watching: he's simply gone
            return;
        }
        if (walkedFar) {
            this.setState(CRUMBLING);               // still in view: he dissolves into the air
            this.getNavigation().stop();
            return;
        }
        if (--this.repathIn <= 0 || this.getNavigation().isDone()) {
            this.repathIn = 40;
            this.walkOn();
        }
        if (this.stateTicks > 200 && this.leaveStart != null && this.position().distanceTo(this.leaveStart) < 2.0) {
            this.setState(CRUMBLING);                // can't find a way off: he doesn't stay
        }
    }

    // Heads for a point a dozen blocks on along his way out, straight away from the player; if the ground there
    // can't be reached, the nearest way round it (turning a little to either side).
    private void walkOn() {
        for (double turn : new double[] {0, 25, -25, 50, -50}) {
            Vec3 dir = this.leaveDirection.yRot((float) Math.toRadians(turn));
            int x = Mth.floor(this.getX() + dir.x * 12), z = Mth.floor(this.getZ() + dir.z * 12);
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (this.getNavigation().moveTo(x + 0.5, y, z + 0.5, LEAVE_SPEED)) {
                return;
            }
        }
    }

    // Crumbles into black dust that drifts up into the air (he fades out on the clients meanwhile), then he's gone.
    private void tickCrumbling() {
        if (this.level() instanceof ServerLevel level) {
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.04F, 0.03F, 0.04F), 1.4F);
            double top = this.getBbHeight() * (1.0 - this.stateTicks / (double) CRUMBLE_TICKS);
            for (int i = 0; i < 6; i++) {
                double y = this.getY() + this.random.nextDouble() * Math.max(0.3, top);
                level.sendParticles(dust, this.getX() + (this.random.nextDouble() - 0.5) * 0.7, y,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.7, 1, 0.05, 0.15, 0.05, 0.02);
            }
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + top * 0.6, this.getZ(), 2, 0.25, 0.4, 0.25, 0.01);
        }
        if (this.stateTicks >= CRUMBLE_TICKS) {
            this.discard();
        }
    }

    // ---------------------------------------------------------------- client
    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == FLASH_EVENT) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                net.ridez.horror.client.OdieFlash.onOdieVanished(this.getWatched());
            }
            return;
        }
        super.handleEntityEvent(pId);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (STATE.equals(pKey) && this.getState() == CRUMBLING) {
            this.crumbleStartedAt = this.tickCount;
        }
        if (STATE.equals(pKey) && this.getState() == TURNING) {
            this.startTurn();
        }
    }

    // Client: how far through crumbling he is (0 = not crumbling, 1 = gone).
    public float getCrumble(float pPartialTick) {
        if (this.getState() != CRUMBLING || this.crumbleStartedAt < 0) {
            return 0.0F;
        }
        return Mth.clamp((this.tickCount - this.crumbleStartedAt + pPartialTick) / CRUMBLE_TICKS, 0.0F, 1.0F);
    }

    // Seen from far off: drawn up to 256 blocks away, much further than a mob of his size normally is.
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return pDistance < 256.0 * 256.0;
    }

    // ---------------------------------------------------------------- untouchable
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(pSource, pAmount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity pEntity) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    // ---------------------------------------------------------------- animation (GeckoLib)
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animate));
    }

    private PlayState animate(AnimationState<OdieEntity> pState) {
        pState.setControllerSpeed(1.0F);
        return switch (this.getState()) {
            case WAVING -> pState.setAndContinue(WAVE);
            case TURNING -> pState.setAndContinue(TURN);
            case LEAVING -> {
                double speed = Math.sqrt(Mth.square(this.getX() - this.xo) + Mth.square(this.getZ() - this.zo));
                pState.setControllerSpeed((float) Mth.clamp(speed / WALK_ANIMATION_SPEED, 0.6, 2.5));
                yield pState.setAndContinue(WALK);
            }
            case CRUMBLING -> PlayState.CONTINUE;          // freezes in whatever he was doing as he crumbles
            default -> pState.setAndContinue(IDLE);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
