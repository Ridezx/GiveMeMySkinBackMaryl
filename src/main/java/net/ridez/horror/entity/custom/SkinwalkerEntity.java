package net.ridez.horror.entity.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.worldgen.HolyPlaces;
import net.ridez.horror.HorrorMod;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A towering, hunched creature wearing skin that isn't its own. That skin doesn't work
// for it, so it wants to take yours and see if it fits.
// - It spawns either standing or on all fours (50/50) and keeps that form: it hunts, fights and runs in it.
// - Calm, it wanders; after 2 minutes it changes to its other form, and 5 minutes after that it lies down and
//   sleeps (then the 7 minutes start over once it's woken).
// - It hears players who walk or run within 30 blocks (crouching players only within 10) and blocks being
//   placed or broken within 30 blocks (see SkinwalkerHearing), asleep or awake. It wakes up and goes on
//   alert, looking around for the sound. It has to see a player to get angry: 70% of
//   Skinwalkers hunt players they see, the rest ignore them until hurt. If it can't find what it heard
//   within 5 seconds, it runs off away from the sound for a few seconds, then calms down.
// - If it's still around when the sun comes up, it runs off away from the nearest player and disappears once it's
//   far enough away that nobody can see it.
// - It only counts a player as seen after keeping them in sight for a full 1.5 seconds.
// - It lives in dense forests (see tags/worldgen/biome/skinwalker_spawns_in.json).
// - 18 health, but normal attacks only do a quarter of their damage (six critical hits of a netherite sword), so players
//   are meant to run.
// - Its hits always do 8 damage (4 hearts), whatever armor the target wears, and knock down a raised shield
//   like an axe does. Iron golems attack it, and it kills one in three hits.
// - It screams now and then while it chases a player.
// - The first time a player hurts it, it screams, then either flees or fights (50/50); after that it just
//   flinches and fights back, so repeated hits don't keep it screaming.
// - If it gets stuck (on a tree, or can't find a way through), it gets lower: standing it drops to all fours
//   (about half as tall), and on all fours down onto its belly to crawl through 1-block gaps and 1x1 holes. It
//   gets back up onto all fours once there's room (straight away if whoever it's after has room too).
// - It climbs up walls to get at something above it, like a spider: out of a pit, up a pillar, up a 1x1 shaft.
// - Salt (Straw): no damage, but the third hit makes it scream and flee.
// - It never goes into a holy place (HolyPlaces): it won't wander or follow a sound in, and a player who runs into
//   one is safe: it stops chasing them and leaves them be for 30 seconds (and while they stay inside).
// - Hurt by a Sandman: it screams, then flees from her or fights her (50/50), in its form (two of her shots kill it,
//   as two Dust Balls do). Awake and not fighting one, it runs from any Sandman it sees within 24 blocks too,
//   keeping an eye on where she is, and keeps running while she's still that close.
// - Dust Ball (Toy Gun): full damage, and the second hit kills it outright. A first hit has a 10% chance to
//   enrage it instead: it freezes and screams at the shooter for 1.5 seconds, then charges them at high
//   speed for a few seconds.
//
// Animated with GeckoLib (see animate): every action has a standing and an on-all-fours animation.
public class SkinwalkerEntity extends Monster implements GeoEntity {
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
    // A Sandman it sees this close sends it running (checked every half second), for at least FLEE_FROM_SANDMAN_TICKS
    // after it last saw her that close.
    private static final double SANDMAN_FEAR_RANGE = 24.0;
    private static final int FLEE_FROM_SANDMAN_TICKS = 100;
    // Enraged by the Toy Gun: chance per surviving Dust Ball hit, how long the charge lasts, and how much
    // faster it runs meanwhile (+60% is ~16 blocks/s, well beyond any player).
    private static final float ENRAGE_CHANCE = 0.1F;
    // The frozen, screaming wind-up before the charge (1.5 seconds, then half a second settling into its run),
    // with a second scream halfway through.
    private static final int WINDUP_TICKS = 40;
    private static final int RAGE_TICKS = 100;
    private static final AttributeModifier RAGE_SPEED = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "skinwalker_rage"), 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    // It holds still while it screams (2 seconds), staggers for a moment when it flinches, and stops while it
    // changes form, so it never slides along while one of those animations plays.
    private static final int SCREAM_HOLD_TICKS = 40;
    private static final int FLINCH_HOLD_TICKS = 9;
    // Calm wandering: it changes form after 2 minutes, and lies down to sleep 5 minutes after that.
    private static final int FORM_CHANGE_TICKS = 2 * 60 * 20;
    private static final int SLEEP_TICKS = 7 * 60 * 20;
    // Hearing: footsteps of players walking or running, and blocks placed or broken, within 30 blocks;
    // crouching players' footsteps only within 10.
    public static final double HEAR_RANGE = 30.0;
    private static final double CROUCH_HEAR_RANGE = 10.0;
    private static final int LISTEN_INTERVAL = 5;
    // On alert it searches for 5 seconds; if it finds nothing, it runs off from the sound for 4 seconds.
    private static final int ALERT_TICKS = 100;
    private static final int RETREAT_TICKS = 80;
    private static final int RETREAT_RANGE = 16;
    // Lying down on all fours, it first walks a small circle like a dog (2.4 seconds of the animation).
    private static final int CIRCLE_TICKS = 48;
    private static final double CIRCLE_RADIUS = 0.7;
    // Leaving at sunrise: it disappears once no player can see it and it's at least 24 blocks from all of them
    // (or more than 64 blocks away from everyone, too far to make out).
    private static final double DAWN_HIDDEN_DISTANCE = 24.0;
    private static final double DAWN_GONE_DISTANCE = 64.0;
    // Its hitbox: tall standing, half as tall on all fours, low lying asleep.
    private static final EntityDimensions STANDING_SIZE = EntityDimensions.scalable(0.9F, 4.5F);
    // (hitboxes are square from above, so on all fours and asleep, where his body is long, they're wider to cover
    // more of it; the model is drawn centred on the box, see getBodyOffset)
    private static final EntityDimensions ON_FOURS_SIZE = EntityDimensions.scalable(1.8F, 2.4F);
    private static final EntityDimensions ASLEEP_SIZE = EntityDimensions.scalable(1.8F, 1.0F);
    // Its low forms, for getting through tunnels too small for it otherwise (none of them wider than a block):
    // stooping (on its hind legs but folded over, for 3-high gaps), creeping (low on all fours, for 2-high gaps)
    // and dragging itself on its belly (for 1x1 holes). Their hitboxes, and how fast each goes (a share of its
    // speed; ground speed goes roughly with its square): stooping 56% (about 2 blocks a second), creeping 50%
    // (about 1.6), on its belly 30% (about 0.6).
    public static final int NOT_LOW = 0, STOOPING = 1, CREEPING = 2, ON_BELLY = 3;
    private static final EntityDimensions[] LOW_SIZES = {null, EntityDimensions.scalable(0.9F, 2.9F),
            EntityDimensions.scalable(0.9F, 1.9F), EntityDimensions.scalable(0.9F, 0.9F)};
    private static final double[] LOW_SPEED = {0.0, -0.44, -0.5, -0.7};
    private static final ResourceLocation LOW_SPEED_ID = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "skinwalker_low");
    // Changing between low forms (or getting down into one, or back up) takes 0.8 seconds. It rises to a taller
    // low form once there has been room for it for half a second, and back to all fours after 3 seconds of room.
    private static final int LOW_CHANGE_TICKS = 16;
    private static final int RISE_TICKS = 10;
    private static final int PRONE_GET_UP_TICKS = 60;
    // Its hits: a flat 8 damage that armor, its enchantments and the difficulty don't change
    // (data/horrormod/damage_type/skinwalker.json and the damage_type tags it's in).
    public static final ResourceKey<DamageType> SKINWALKER_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "skinwalker"));
    private static final float HIT_DAMAGE = 8.0F;
    // A player counts as seen once it has kept them in sight for 1.5 seconds without a break.
    private static final int SIGHT_TICKS = 30;
    private static final double SIGHT_RANGE = 32.0;
    // Trying to get somewhere without getting half a block closer for 0.8 seconds: it gets lower.
    private static final int STUCK_TICKS = 16;
    // /skinwalker sleep (testing): 5 more seconds of whatever it's doing, then it lies down.
    private static final int SLEEPY_TICKS = 100;
    // Screams while chasing a player: the first soon after it starts, then every 3 to 7 seconds.
    private static final int CHASE_SCREAM_FIRST = 10, CHASE_SCREAM_MIN = 60, CHASE_SCREAM_MAX = 140;
    // Finding a way to whoever it's after, once a second: if there's none at its size but there is one on its
    // belly (a 1x1 hole into a shelter), it goes to where the way gets too small and gets down there. With no way
    // at all for 15 seconds it gives up on them and goes back to roaming, leaving them be for 30 seconds (unless
    // they hurt it).
    private static final int ROUTE_INTERVAL = 20;
    private static final int GIVE_UP_TICKS = 300;
    private static final int LEAVE_BE_TICKS = 600;
    // Once down on its belly to get through a gap, it stays down at least this long (or it would get up again
    // before it had crawled in, still having room around it).
    private static final int SQUEEZE_COMMIT_TICKS = 100;
    // Knocking: roaming, it goes to a closed door within 5 blocks, kneels and knocks (the raps land at these ticks
    // of the knock animation), then speaks through it while it listens, and afterwards waits by the door for 10 to
    // 20 seconds, staring at it. Footsteps it hears meanwhile don't send it searching (or running off): it's
    // waiting for whoever is in there. At most one knock a minute, and not the same door again for 5 minutes; if
    // it can't get to the door within 10 seconds it leaves it.
    private static final int KNOCK_RANGE = 5;
    private static final int[] KNOCK_RAP_TICKS = {22, 27, 32};
    private static final int KNOCK_VOICE_TICK = 46;
    private static final int LINGER_MIN_TICKS = 200, LINGER_MAX_TICKS = 400;
    // Calm, it ambles: slowly (about 2 blocks a second, a third of its chasing speed) and only now and then
    // (on average every 20 seconds).
    private static final double AMBLE_SPEED = 0.3;
    private static final int AMBLE_INTERVAL = 400;
    private static final int KNOCK_COOLDOWN = 60 * 20;
    private static final long SAME_DOOR_COOLDOWN = 5 * 60 * 20;
    private static final int KNOCK_APPROACH_TICKS = 200;

    // Synced to the client, which picks the animations.
    private static final EntityDataAccessor<Boolean> ON_FOURS = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ALERT = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DUST_DEATH = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LOW = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    // The one-off action it's doing (an index into ACTIONS, -1 for none) and a counter that goes up with every
    // new action, so the same action twice in a row plays twice.
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_COUNT = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.INT);

    // ---- animation
    private static final String CONTROLLER = "main";
    // Ticks to blend from one animation into the next, so switching never snaps.
    private static final int BLEND_TICKS = 4;
    // One-off actions the server starts, and how long each lasts.
    private static final List<String> ACTIONS = List.of("punch", "slap_on_fours", "bite_on_fours",
            "scream_standing", "scream_on_fours", "hurt_standing", "hurt_on_fours",
            "enraged_windup_standing", "enraged_windup_on_fours", "lie_down_standing", "lie_down_on_fours",
            "wake_standing_to_standing", "wake_standing_to_on_fours", "wake_on_fours_to_standing", "wake_on_fours_to_on_fours",
            "knock_standing", "knock_on_fours");
    private static final float[] ACTION_SECONDS = {0.85F, 0.6F, 0.6F, 2.1F, 2.1F, 0.45F, 0.45F, 2.0F, 2.0F, 2.4F, 3.6F,
            1.7F, 2.2F, 1.9F, 1.0F, 5.4F, 5.4F};
    // Seconds each death animation lasts; the body stays a second longer before it's removed.
    private static final float DEATH_SECONDS = 2.0F, DEATH_ON_FOURS_SECONDS = 1.8F,
            DUST_DEATH_SECONDS = 5.6F, DUST_DEATH_ON_FOURS_SECONDS = 5.5F;
    // Changing form takes 0.9 seconds.
    private static final int STANCE_CHANGE_TICKS = 18;
    // Ground speed (blocks per tick) each gait's animation is made for; the animation speeds up or slows
    // down to match how fast it's actually moving, so its feet don't slide.
    private static final double WALK_SPEED = 0.12, RUN_SPEED = 0.32, FLEE_SPEED_ANIM = 0.9, CRAWL_SPEED = 0.068, CLIMB_SPEED = 0.15,
            CREEP_SPEED = 0.045, STOOP_SPEED = 0.089;
    // While idle, now and then it sniffs, tilts its head or tugs at its skin (on average every 20 seconds).
    private static final int IDLE_FLAVOR_CHANCE = 400;
    private static final Map<String, RawAnimation> LOOPS = new HashMap<>(), ONCE = new HashMap<>(), HOLD = new HashMap<>(),
            ALERTS = new HashMap<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    // Client-side animation state
    // the form it was last shown in (stand, crawl, stoop, creep or prone), and the change it's animating
    private String shownForm = "stand";
    private String formFrom = "stand";
    private int formChangedAt = -LOW_CHANGE_TICKS - 2;
    private boolean formSeen;
    @Nullable
    private String idleFlavor;
    private int idleFlavorUntil;
    private boolean actionCountSeen;
    private int shownActionCount;
    @Nullable
    private String shownAction;
    private int actionStartedAt;

    // Server state
    private boolean huntsOnSight;
    private int saltHits;
    private int dustBallHits;
    private int fleeTicks;
    private int windupTicks;
    private int rageTicks;
    // Who scared it; not saved, so after a reload it runs from the nearest player instead.
    @Nullable
    private LivingEntity scaredBy;
    private int lastScreamTick = -1;
    // ticks left holding still (screaming, flinching, changing form, waking)
    private int holdTicks;
    // the action playing now, when it ends, and one waiting for it (a scream after waking up)
    @Nullable
    private String currentAction;
    private int actionEndsAt;
    @Nullable
    private String pendingAction;
    // calm wandering time, and whether it has changed form yet this time round
    private int calmTicks;
    private boolean changedForm;
    private boolean sleeping;
    // searching for a sound, and running off from one it couldn't find
    private int alertTicks;
    @Nullable
    private Vec3 soundPos;
    @Nullable
    private Vec3 alertAfterHold;
    private int retreatTicks;
    // walking the little circle before lying down
    private int circleTicks;
    @Nullable
    private Vec3 circleCenter;
    private double circleAngle;
    // where each player was at the last listen, to tell who's moving
    private final Map<UUID, Vec3> lastPlayerPos = new HashMap<>();
    // whether it has been night since it appeared, and whether it's leaving because the sun came up
    private boolean sawNight;
    private boolean leavingAtDawn;
    // whether a player has hurt it before (only the first time makes it scream)
    private boolean reactedToHit;
    // how long it has had each player in sight, without a break
    private final Map<UUID, Integer> sightTicks = new HashMap<>();
    private int stuckTicks;
    @Nullable
    private Vec3 stuckFrom;
    private int roomTicks;
    private int risingTo;
    private int sleepyTicks;
    private int chaseScreamIn = CHASE_SCREAM_FIRST;
    // Navigators that plan paths as if it were in each low form (see LowNavigation; indexed by form), where to get
    // down to squeeze through and into which form, how long it has found no way at all, and who it has given up
    // on (until when).
    private final PathNavigation[] lowNavigations = new PathNavigation[ON_BELLY + 1];
    @Nullable
    private BlockPos squeezeAt;
    private int squeezeForm;
    private int squeezeCommitTicks;
    private int noWayTicks;
    private final Map<UUID, Integer> leftBe = new HashMap<>();
    // Its hits on each iron golem so far (GOLEM_HITS kill one, however much it has healed in between).
    private final Map<UUID, Integer> golemHits = new HashMap<>();
    private static final int GOLEM_HITS = 3;
    // knocking: the door it's knocking on and how far into the knock it is, the wait before the next knock, and
    // when it last knocked on each door
    @Nullable
    private BlockPos knockDoor;
    private int knockTicks;
    private int knockCooldown;
    private final Map<BlockPos, Long> knockedDoors = new HashMap<>();
    // after knocking: how much longer it waits by the door
    @Nullable
    private BlockPos lingerDoor;
    private int lingerTicks;

    public SkinwalkerEntity(EntityType<? extends Monster> pType, Level pLevel) {
        super(pType, pLevel);
        // Rolled once when created; a saved Skinwalker keeps both (see readAdditionalSaveData).
        this.huntsOnSight = this.random.nextFloat() < HUNTS_ON_SIGHT_CHANCE;
        // The form is picked on the server only: a game showing it would otherwise pick its own at random, and the
        // server never sends "standing" (the default) when it first shows the Skinwalker to a player, so the two
        // could disagree (on all fours on screen, standing on the server: the wrong scream, the wrong hitbox).
        if (!pLevel.isClientSide()) {
            this.entityData.set(ON_FOURS, this.random.nextBoolean());
        }
        for (int form = STOOPING; form <= ON_BELLY; form++) {
            this.lowNavigations[form] = new LowNavigation(this, pLevel, 4 - form);       // 3, 2 and 1 blocks high
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)                         // (normal hits do a quarter: six critical hits of a netherite sword)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                // ~6.4 blocks/s when chasing: faster than a sprinting player on flat ground (~5.6) and a baby
                // zombie (~5.2), just under a sprint-jumping player (~7), so only sprint-jumping escapes it.
                // (Mob ground speed scales with this value squared.)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(ON_FOURS, false);
        pBuilder.define(FLEEING, false);
        pBuilder.define(RUNNING, false);
        pBuilder.define(SLEEPING, false);
        pBuilder.define(ALERT, false);
        pBuilder.define(DUST_DEATH, false);
        pBuilder.define(LOW, NOT_LOW);
        pBuilder.define(CLIMBING, false);
        pBuilder.define(ACTION, -1);
        pBuilder.define(ACTION_COUNT, 0);
    }

    // Pathfinds like any mob, but when it can't find a way to what it's after, heads straight for it and climbs
    // (see tickClimb).
    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new WallClimberNavigation(this, pLevel);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new HoldStillGoal());
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(1, new LeaveAtDawnGoal());
        this.goalSelector.addGoal(1, new AlertGoal());
        this.goalSelector.addGoal(1, new SqueezeGoal());
        this.goalSelector.addGoal(2, new RetreatGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(4, new KnockGoal());
        this.goalSelector.addGoal(3, new LingerGoal());
        WaterAvoidingRandomStrollGoal amble = new WaterAvoidingRandomStrollGoal(this, AMBLE_SPEED) {
            @Nullable
            @Override
            protected Vec3 getPosition() {                  // (never into a holy place)
                Vec3 to = super.getPosition();
                return to != null && SkinwalkerEntity.this.isHoly(BlockPos.containing(to)) ? null : to;
            }
        };
        amble.setInterval(AMBLE_INTERVAL);
        this.goalSelector.addGoal(5, amble);
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // it has to have seen a player (1.5 seconds in sight) to go after them, and can't while asleep
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> this.huntsOnSight && !this.isFleeing() && !this.sleeping && !this.leavingAtDawn
                        && player instanceof Player seen && this.hasSeen(seen) && !this.leavingBe(seen) && !this.isHoly(seen.blockPosition())));
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.level().isClientSide() || this.isInvulnerableTo(pSource)) {
            return super.hurt(pSource, pAmount);
        }
        if (this.sleeping) {
            this.wakeUp(null, true);              // then reacts below once it's up (see playAction)
        }
        this.alertTicks = 0;
        this.retreatTicks = 0;
        boolean salt = pSource.getDirectEntity() instanceof SaltProjectile;
        boolean dustBall = pSource.getDirectEntity() instanceof DustBallProjectile;
        if (dustBall) {
            if (++this.dustBallHits >= DUST_BALL_HITS_TO_KILL) {
                pAmount = this.getHealth() + this.getMaxHealth();
                this.entityData.set(DUST_DEATH, true);
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
        if (this.leavingAtDawn) {
            return hurt;                            // it just keeps going
        }
        // Hurt by a Sandman: it screams, then flees from her or turns on her (50/50), in whatever form it's in; once
        // it has chosen, more hits from her don't change its mind.
        if (hurt && this.isAlive() && pSource.getEntity() instanceof SandmanEntity sandman) {
            if (this.getTarget() == sandman || this.isFleeing()) {
                return true;
            }
            this.scream();
            if (this.random.nextBoolean()) {
                this.startFleeing(sandman);
            } else {
                this.setTarget(sandman);
            }
            return true;
        }
        if (hurt && dustBall && this.isAlive() && pSource.getEntity() instanceof Player shooter
                && this.random.nextFloat() < ENRAGE_CHANCE) {
            this.scream();
            this.enrage(shooter);
            return true;
        }
        // Salt only counts toward its own three-hit flee, it doesn't provoke the scream-and-decide reaction.
        // Only the first player to hurt it gets the scream; after that it just turns on whoever hits it.
        if (hurt && !salt && this.isAlive() && !this.isFleeing() && pSource.getEntity() instanceof Player player && this.getTarget() != player) {
            if (!this.reactedToHit) {
                this.reactedToHit = true;
                this.scream();
                if (this.random.nextBoolean()) {
                    this.startFleeing(player);
                } else {
                    this.setTarget(player);
                }
            } else {
                this.setTarget(player);
            }
        }
        if (hurt && this.isAlive() && !this.isFleeing() && pSource.getEntity() instanceof IronGolem golem) {
            this.setTarget(golem);
        }
        // A hit it doesn't scream at still makes it flinch (unless it's still getting up).
        if (hurt && this.isAlive() && this.lastScreamTick != this.tickCount && this.windupTicks == 0 && !this.isWaking()) {
            this.playAction(this.isOnFours() ? "hurt_on_fours" : "hurt_standing");
            this.holdTicks = Math.max(this.holdTicks, FLINCH_HOLD_TICKS);
        }
        return hurt;
    }

    // Fear wins over rage: scaring it off (e.g. a third Salt hit) cancels a wind-up or charge.
    private void startFleeing(@Nullable LivingEntity from) {
        this.windupTicks = 0;
        this.rageTicks = 0;
        this.fleeTicks = FLEE_TICKS;
        this.scaredBy = from;
        this.setTarget(null);
    }

    public boolean isFleeing() {
        return this.fleeTicks > 0;
    }

    private boolean isHoly(BlockPos pPos) {
        return this.level() instanceof ServerLevel level && HolyPlaces.inside(level, pPos);
    }

    // A player it's chasing who gets into a holy place is safe from it: it gives up on them.
    private void tickHoly() {
        if (this.tickCount % 10 == 0 && this.getTarget() instanceof Player player && this.isHoly(player.blockPosition())) {
            this.leftBe.put(player.getUUID(), this.tickCount + LEAVE_BE_TICKS);
            this.setTarget(null);
        }
    }

    // Awake and seeing a Sandman close by, it runs from her, and keeps running while she's still that close.
    private void tickSandmanFear() {
        if (this.tickCount % 10 != 0 || this.sleeping || this.leavingAtDawn || this.getTarget() instanceof SandmanEntity) {
            return;
        }
        SandmanEntity nearest = null;
        double best = SANDMAN_FEAR_RANGE * SANDMAN_FEAR_RANGE;
        for (SandmanEntity sandman : this.level().getEntitiesOfClass(SandmanEntity.class, this.getBoundingBox().inflate(SANDMAN_FEAR_RANGE))) {
            double d = this.distanceToSqr(sandman);
            if (sandman.isAlive() && d < best && this.hasLineOfSight(sandman)) {
                nearest = sandman;
                best = d;
            }
        }
        if (nearest == null) {
            return;
        }
        if (!this.isFleeing() || this.scaredBy != nearest) {
            this.startFleeing(nearest);
        }
        this.fleeTicks = Math.max(this.fleeTicks, FLEE_FROM_SANDMAN_TICKS);
    }

    private void enrage(Player target) {
        this.fleeTicks = 0;
        this.scaredBy = null;
        this.rageTicks = 0;
        this.windupTicks = WINDUP_TICKS + this.wakingTicksLeft();
        this.setTarget(target);
        this.playAction(this.isOnFours() ? "enraged_windup_on_fours" : "enraged_windup_standing");
    }

    // Placeholder: a deep, slowed enderman scream until the mod has its own sounds.
    private void scream() {
        this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.5F);
        // (the wind-up animation already screams)
        if (!this.level().isClientSide() && this.windupTicks == 0 && this.isAlive()) {
            this.lastScreamTick = this.tickCount;
            this.holdTicks = Math.max(this.holdTicks, this.wakingTicksLeft() + SCREAM_HOLD_TICKS);
            this.playAction(this.isOnFours() ? "scream_on_fours" : "scream_standing");
        }
    }

    // Like a mob's normal attack, but with its own damage type (a flat 8 through any armor), an iron golem dies
    // in three hits, and a shield that blocks it is knocked down for 5 seconds like an axe does.
    @Override
    public boolean doHurtTarget(Entity pTarget) {
        DamageSource source = new SkinwalkerDamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SKINWALKER_DAMAGE), this);
        float damage = HIT_DAMAGE;
        if (pTarget instanceof IronGolem golem) {           // three hits kill it: a third of it each, the third finishes it
            int hits = this.golemHits.merge(golem.getUUID(), 1, Integer::sum);
            damage = hits >= GOLEM_HITS ? golem.getHealth() + golem.getMaxHealth() : golem.getMaxHealth() / GOLEM_HITS;
        }
        boolean shielded = pTarget instanceof Player player && player.isDamageSourceBlocked(source);
        boolean hit = pTarget.hurt(source, damage);
        if (shielded && pTarget instanceof Player player) {
            player.disableShield();
        }
        if (hit) {
            float knockback = this.getKnockback(pTarget, source);
            if (knockback > 0.0F && pTarget instanceof LivingEntity living) {
                living.knockback(knockback * 0.5F, Mth.sin(this.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(this.getYRot() * Mth.DEG_TO_RAD));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
            if (this.level() instanceof ServerLevel level) {
                EnchantmentHelper.doPostAttackEffects(level, pTarget, source);
            }
            this.setLastHurtMob(pTarget);
        }
        if (hit || shielded) {
            this.playAction(!this.isOnFours() ? "punch" : this.random.nextBoolean() ? "slap_on_fours" : "bite_on_fours");
        }
        return hit;
    }

    // ---------------------------------------------------------------- actions (server side)
    // Starts a one-off animation (the client plays it, see animate). Anything that happens while it's still
    // getting up from sleep waits until it's up.
    private void playAction(String name) {
        if (this.isLow()) {
            return;                                 // its one-off animations are for standing or all fours
        }
        if (this.isWaking()) {
            this.pendingAction = name;
            return;
        }
        this.currentAction = name;
        this.actionEndsAt = this.tickCount + Mth.ceil(ACTION_SECONDS[ACTIONS.indexOf(name)] * 20);
        this.entityData.set(ACTION, ACTIONS.indexOf(name));
        this.entityData.set(ACTION_COUNT, this.entityData.get(ACTION_COUNT) + 1);
    }

    private boolean isWaking() {
        return this.currentAction != null && this.currentAction.startsWith("wake_") && this.tickCount < this.actionEndsAt;
    }

    private int wakingTicksLeft() {
        return this.isWaking() ? this.actionEndsAt - this.tickCount : 0;
    }

    // ---------------------------------------------------------------- calm, sleep, hearing (server side)
    private boolean isCalm() {
        return !this.leavingAtDawn && !this.sleeping && !this.isLow() && this.getTarget() == null && !this.isFleeing() && this.retreatTicks == 0 && this.alertTicks == 0
                && this.windupTicks == 0 && this.rageTicks == 0 && this.holdTicks == 0 && this.circleTicks == 0;
    }

    private void tickCalm() {
        if (!this.isCalm()) {
            return;
        }
        this.calmTicks++;
        if (!this.changedForm && this.calmTicks >= FORM_CHANGE_TICKS && (!this.isOnFours() || this.roomToStand())) {
            this.changedForm = true;
            this.entityData.set(ON_FOURS, !this.isOnFours());
            this.holdTicks += STANCE_CHANGE_TICKS;
        } else if (this.calmTicks >= SLEEP_TICKS && this.canLieDownHere()) {
            this.lieDown();
        }
    }

    // On the ground and out of the water (a puddle too: lying down, even shallow water would float it).
    private boolean canLieDownHere() {
        return this.onGround() && !this.isInWater();
    }

    private void lieDown() {
        this.sleeping = true;
        this.getNavigation().stop();
        this.playAction(this.isOnFours() ? "lie_down_on_fours" : "lie_down_standing");
        if (this.isOnFours()) {
            // walks a small circle around the spot first, like a dog
            this.circleTicks = CIRCLE_TICKS;
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            this.circleCenter = this.position().add(right.scale(CIRCLE_RADIUS));
            this.circleAngle = Math.atan2(this.getZ() - this.circleCenter.z, this.getX() - this.circleCenter.x);
        }
    }

    private void tickCircle() {
        if (this.circleTicks <= 0 || this.circleCenter == null) {
            return;
        }
        this.circleTicks--;
        this.circleAngle += 2 * Math.PI / CIRCLE_TICKS;
        Vec3 next = this.circleCenter.add(Math.cos(this.circleAngle) * CIRCLE_RADIUS, 0, Math.sin(this.circleAngle) * CIRCLE_RADIUS);
        Vec3 step = new Vec3(next.x - this.getX(), 0, next.z - this.getZ());
        this.move(MoverType.SELF, step);
        if (step.lengthSqr() > 1.0E-6) {
            float yaw = (float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
    }

    // Wakes up, then searches for the sound. Woken by a sound it gets up into either form (picked at random);
    // hit in its sleep it gets up in the form it slept in, ready to react. It only stands if there's room.
    private void wakeUp(@Nullable Vec3 heard, boolean keepForm) {
        boolean sleptOnFours = this.isOnFours();
        boolean endOnFours = keepForm ? sleptOnFours : this.random.nextBoolean();
        if (!endOnFours && !this.roomToStand()) {
            endOnFours = true;
        }
        this.sleeping = false;
        this.circleTicks = 0;
        this.calmTicks = 0;
        this.changedForm = false;
        String name = "wake_" + (sleptOnFours ? "on_fours" : "standing") + "_to_" + (endOnFours ? "on_fours" : "standing");
        this.playAction(name);
        this.entityData.set(ON_FOURS, endOnFours);
        this.holdTicks = Math.max(this.holdTicks, Mth.ceil(ACTION_SECONDS[ACTIONS.indexOf(name)] * 20));
        this.alertAfterHold = heard;
    }

    // Every few ticks: did a player make a sound it could hear?
    private void listen() {
        for (Player player : this.level().players()) {
            Vec3 now = player.position();
            Vec3 before = this.lastPlayerPos.put(player.getUUID(), now);
            if (before == null || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            boolean moving = Mth.square(now.x - before.x) + Mth.square(now.z - before.z) > 0.0025;
            double range = player.isCrouching() ? CROUCH_HEAR_RANGE : HEAR_RANGE;
            if (moving && this.distanceToSqr(player) < range * range) {
                this.hearSound(now);
                return;
            }
        }
    }

    // A sound it can hear at pos: footsteps (see listen) or a block placed or broken (see SkinwalkerHearing).
    public void hearSound(Vec3 pos) {
        if (this.isHoly(BlockPos.containing(pos))) {
            return;                                 // (whatever it is, it's in a holy place: it won't go in)
        }
        if (this.leavingAtDawn || !this.isAlive() || !this.sleeping && (this.knockDoor != null || this.lingerTicks > 0)) {
            return;                                 // (knocking or waiting at a door, it pays footsteps no mind)
        }
        if (this.sleeping) {
            this.wakeUp(pos, false);
        } else if (this.getTarget() == null && !this.isFleeing() && this.windupTicks == 0 && this.rageTicks == 0
                && this.retreatTicks == 0) {
            if (this.holdTicks > 0) {
                this.alertAfterHold = pos;
            } else if (this.alertTicks == 0) {
                this.startAlert(pos);
            } else {
                this.soundPos = pos;               // keeps searching toward the latest sound
            }
        }
    }

    private void startAlert(Vec3 pos) {
        this.alertTicks = ALERT_TICKS;
        this.soundPos = pos;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
        }
        if (this.level().isClientSide()) {
            this.tickIdleFlavor();
            this.tickBodyOffset();
            this.tickEyeGlow();
            return;
        }
        if (this.windupTicks > 0) {
            this.windupTicks--;
            if (this.windupTicks == WINDUP_TICKS / 2) {
                this.scream();
            }
            if (this.windupTicks == 0) {
                this.rageTicks = RAGE_TICKS;
            }
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (this.isLow()) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(LOW_SPEED_ID, LOW_SPEED[this.getLow()],
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(LOW_SPEED_ID);
        }
        if (this.rageTicks > 0) {
            this.rageTicks--;
            speed.addOrUpdateTransientModifier(RAGE_SPEED);
        } else {
            speed.removeModifier(RAGE_SPEED.id());
        }
        if (this.holdTicks > 0) {
            this.holdTicks--;
        }
        if (this.retreatTicks > 0) {
            this.retreatTicks--;
        }
        if (this.knockCooldown > 0) {
            this.knockCooldown--;
        }
        if (this.lingerTicks > 0 && (--this.lingerTicks == 0 || this.getTarget() != null || this.isFleeing() || this.leavingAtDawn)) {
            this.lingerTicks = 0;
            this.lingerDoor = null;
        }
        if (this.squeezeCommitTicks > 0) {
            this.squeezeCommitTicks--;
        }
        if (this.pendingAction != null && !this.isWaking()) {
            String next = this.pendingAction;
            this.pendingAction = null;
            this.playAction(next);
        }
        if (this.alertAfterHold != null && this.holdTicks == 0 && this.isAlive()) {
            Vec3 pos = this.alertAfterHold;
            this.alertAfterHold = null;
            this.hearSound(pos);
        }
        if (this.getTarget() != null) {
            this.alertTicks = 0;
        }
        this.tickDawn();
        if (this.isAlive()) {
            this.tickSight();
            this.tickCircle();
            if (this.tickCount % LISTEN_INTERVAL == 0) {
                this.listen();
            }
            this.tickCalm();
            this.tickClimb();
            this.tickStuck();
            this.tickLow();
            this.tickRoute();
            this.tickKnock();
            this.tickChaseScream();
            this.tickSandmanFear();
            this.tickHoly();
            if (this.sleepyTicks > 0 && --this.sleepyTicks == 0) {
                this.fallAsleep();
            }
            if (this.sleeping && this.isInWater()) {
                this.wakeUp(null, true);            // water came over it: it gets up
            }
        }
        this.entityData.set(FLEEING, this.isFleeing());
        this.entityData.set(RUNNING, this.rageTicks > 0 || this.windupTicks > 0 || this.retreatTicks > 0 || this.leavingAtDawn);
        this.entityData.set(SLEEPING, this.sleeping);
        this.entityData.set(ALERT, this.alertTicks > 0);
    }

    public boolean isOnFours() {
        return this.entityData.get(ON_FOURS);
    }

    // Client: how far (blocks) the model is drawn back from the entity's position so his body sits centred on the
    // hitbox. His body reaches much further in front of his hips than behind (most of all on all fours), so without
    // this the hitbox sat over his back half. Eases over a few ticks as he changes form. Lying down to sleep keeps
    // the offset he had, so he doesn't slide along the ground as he goes down (the wide sleeping hitbox still
    // covers him).
    private float bodyOffset, bodyOffsetO;
    private static final float[] LOW_BODY_OFFSET = {0.0F, 0.45F, 0.42F, 0.27F};

    private void tickBodyOffset() {
        float target;
        if (this.isLow()) {
            target = LOW_BODY_OFFSET[this.getLow()];
        } else {
            target = this.isOnFours() ? 0.38F : 0.15F;
        }
        this.bodyOffsetO = this.bodyOffset;
        this.bodyOffset += (target - this.bodyOffset) * 0.15F;
    }

    public float getBodyOffset(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.bodyOffsetO, this.bodyOffset);
    }

    // Client: how brightly its eyes glow (1 awake); asleep they dim down, easing over a second or two.
    private static final float SLEEPING_EYE_GLOW = 0.3F;
    private float eyeGlow = 1.0F, eyeGlowO = 1.0F;

    private void tickEyeGlow() {
        float target = this.entityData.get(SLEEPING) ? SLEEPING_EYE_GLOW : 1.0F;
        this.eyeGlowO = this.eyeGlow;
        this.eyeGlow += (target - this.eyeGlow) * 0.08F;
    }

    public float getEyeGlow(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.eyeGlowO, this.eyeGlow);
    }

    // ---------------------------------------------------------------- sight, getting stuck, chase screams (server side)
    // Counts how long each player has been in its sight without a break (nothing while it's asleep).
    private void tickSight() {
        for (Player player : this.level().players()) {
            boolean inSight = !this.sleeping && !player.isSpectator() && player.isAlive()
                    && this.distanceToSqr(player) < SIGHT_RANGE * SIGHT_RANGE && this.getSensing().hasLineOfSight(player);
            if (inSight) {
                this.sightTicks.merge(player.getUUID(), 1, Integer::sum);
            } else {
                this.sightTicks.remove(player.getUUID());
            }
        }
    }

    private boolean hasSeen(Player player) {
        return this.sightTicks.getOrDefault(player.getUUID(), 0) >= SIGHT_TICKS;
    }

    // Trying to get somewhere (a path, or a target out of reach) and not getting half a block closer for 0.8
    // seconds: it gets lower to get under or through whatever is in the way, from standing to all fours, from all
    // fours (or stooping) to creeping, and from creeping onto its belly. Not while it's climbing. (Most of the
    // time it already knows it has to get low before it gets stuck: see tickRoute.)
    private void tickStuck() {
        LivingEntity target = this.getTarget();
        boolean wantsToMove = this.getNavigation().isInProgress() || target != null && !this.isWithinMeleeAttackRange(target);
        // (not while it has just got down into the form it planned to squeeze through with: give it time to line up)
        if (!wantsToMove || this.getLow() == ON_BELLY || this.squeezeCommitTicks > 0 || this.sleeping || this.holdTicks > 0 || this.windupTicks > 0 || this.circleTicks > 0
                || this.isClimbing() || this.isInWater()) {
            this.stuckTicks = 0;
            this.stuckFrom = null;
            return;
        }
        if (this.stuckFrom == null || this.position().distanceToSqr(this.stuckFrom) > 0.25) {
            this.stuckFrom = this.position();
            this.stuckTicks = 0;
            return;
        }
        if (++this.stuckTicks >= STUCK_TICKS) {
            this.stuckTicks = 0;
            this.stuckFrom = null;
            if (!this.isOnFours() && !this.isLow()) {
                this.entityData.set(ON_FOURS, true);
                this.holdTicks += STANCE_CHANGE_TICKS;
            } else {
                this.getDownTo(this.getLow() == CREEPING ? ON_BELLY : CREEPING);
            }
        }
    }

    // In a low form: rises to the tallest form there's room for where it is. Back up onto all fours once there has
    // been room for 3 seconds, or straight away when whoever it's after is close and has room around them too (if
    // they're in a tunnel, it stays down); from its belly or creeping up to a taller low form after half a second
    // (on its belly in a 2-high tunnel it creeps, in a 3-high one it stoops).
    private void tickLow() {
        int low = this.getLow();
        if (low == NOT_LOW || this.holdTicks > 0 || this.isClimbing() || this.squeezeCommitTicks > 0) {
            this.roomTicks = 0;
            return;
        }
        LivingEntity target = this.getTarget();
        int best = low;
        if (this.roomFor(ON_FOURS_SIZE, this.position()) && (target == null || this.roomFor(ON_FOURS_SIZE, target.position()))) {
            best = NOT_LOW;
        } else {
            for (int form = STOOPING; form < low; form++) {
                if (this.roomFor(LOW_SIZES[form], this.position())) {
                    best = form;
                    break;
                }
            }
        }
        if (best != this.risingTo) {
            this.risingTo = best;
            this.roomTicks = 0;
        }
        if (best == low) {
            return;
        }
        boolean close = target != null && this.distanceToSqr(target) < 16.0;
        int needed = best != NOT_LOW ? RISE_TICKS : close ? 2 : PRONE_GET_UP_TICKS;
        if (++this.roomTicks >= needed) {
            this.roomTicks = 0;
            this.entityData.set(LOW, best);
            this.holdTicks += LOW_CHANGE_TICKS;
        }
    }

    // Down into a low form (from standing or all fours, or a taller low form). It stays down at least a few seconds.
    private void getDownTo(int pForm) {
        this.entityData.set(ON_FOURS, true);        // (getting back up from any of them, it's on all fours)
        this.entityData.set(LOW, pForm);
        this.holdTicks += LOW_CHANGE_TICKS;
        this.squeezeCommitTicks = SQUEEZE_COMMIT_TICKS;
    }

    public int getLow() {
        return this.entityData.get(LOW);
    }

    public boolean isLow() {
        return this.getLow() != NOT_LOW;
    }

    // Climbs (like a spider) while it's against a wall, or in a shaft, trying to get to something above it.
    private void tickClimb() {
        boolean climb = false;
        // (never on its belly: its climbing poses are for standing, and the low gaps it crawls through would have it
        // climbing into the ceiling; nor while it's heading for a gap to squeeze through instead of going over)
        if (!this.sleeping && this.holdTicks == 0 && this.windupTicks == 0 && !this.isLow() && this.squeezeAt == null) {
            LivingEntity target = this.getTarget();
            BlockPos goal = target != null ? target.blockPosition() : this.getNavigation().getTargetPos();
            boolean wantsUp = goal != null && goal.getY() > this.getY() + 1.0;
            climb = wantsUp && (this.horizontalCollision || this.inShaft());
        }
        this.entityData.set(CLIMBING, climb);
    }

    private boolean inShaft() {
        BlockPos pos = this.blockPosition();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (this.level().getBlockState(side).getCollisionShape(this.level(), side).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isClimbing() {
        return this.entityData.get(CLIMBING);
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing() || super.onClimbable();
    }

    private boolean roomFor(EntityDimensions pSize, Vec3 pPos) {
        return this.level().noCollision(this, pSize.makeBoundingBox(pPos).deflate(1.0E-7));
    }

    // Whether there's headroom for it to stand up where it is.
    private boolean roomToStand() {
        return this.roomFor(STANDING_SIZE, this.position());
    }

    // Placeholder: the same deep, slowed enderman scream, now and then while it chases a player.
    private void tickChaseScream() {
        if (!(this.getTarget() instanceof Player) || this.holdTicks > 0 || this.windupTicks > 0) {
            this.chaseScreamIn = Math.max(this.chaseScreamIn, CHASE_SCREAM_FIRST);
            return;
        }
        if (--this.chaseScreamIn <= 0) {
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.5F);
            this.chaseScreamIn = Mth.nextInt(this.random, CHASE_SCREAM_MIN, CHASE_SCREAM_MAX);
        }
    }

    // ---------------------------------------------------------------- finding a way (server side)
    // Once a second while it's after someone it can't reach yet: is there a way at its size? If not, is there one
    // stooping, creeping or on its belly (the tallest that gets there; then it heads for where it has to get down,
    // see SqueezeGoal)? If none, after a while it gives up on them.
    private void tickRoute() {
        LivingEntity target = this.getTarget();
        if (target == null || this.sleeping || this.isClimbing() || this.isWithinMeleeAttackRange(target)) {
            this.squeezeAt = null;
            this.noWayTicks = 0;
            return;
        }
        if (this.tickCount % ROUTE_INTERVAL != 0 || !this.onGround()) {
            return;
        }
        Path path = this.getNavigation().createPath(target, 1);
        if (path != null && path.canReach()) {
            this.squeezeAt = null;
            this.noWayTicks = 0;
            return;
        }
        for (int form = this.getLow() + 1; form <= ON_BELLY; form++) {   // the tallest low form that gets there
            Path low = this.lowNavigations[form].createPath(target, 1);
            if (low != null && low.canReach()) {
                this.squeezeAt = this.whereToGetDown(low);
                this.squeezeForm = form;
                this.noWayTicks = 0;
                return;
            }
        }
        this.squeezeAt = null;
        this.noWayTicks += ROUTE_INTERVAL;
        if (this.noWayTicks >= GIVE_UP_TICKS) {
            this.noWayTicks = 0;
            if (target instanceof Player player) {
                this.leftBe.put(player.getUUID(), this.tickCount + LEAVE_BE_TICKS);
            }
            this.setTarget(null);                   // no way to them: it goes back to roaming
        }
    }

    // The last spot along a low form's path where it still fits as it is (just before the gap).
    private BlockPos whereToGetDown(Path pLow) {
        EntityDimensions size = this.getDimensions(this.getPose());
        for (int i = 0; i < pLow.getNodeCount(); i++) {
            BlockPos node = pLow.getNode(i).asBlockPos();
            if (!this.roomFor(size, Vec3.atBottomCenterOf(node))) {
                return i > 0 ? pLow.getNode(i - 1).asBlockPos() : this.blockPosition();
            }
        }
        return this.blockPosition();
    }

    // At the gap: down into the low form that gets it through.
    private void getDownToSqueeze() {
        this.getNavigation().stop();
        this.getDownTo(this.squeezeForm);
        this.squeezeAt = null;
    }

    private boolean leavingBe(Player pPlayer) {
        Integer until = this.leftBe.get(pPlayer.getUUID());
        return until != null && this.tickCount < until;
    }

    // ---------------------------------------------------------------- knocking (server side)
    private boolean canKnock() {
        return this.isCalm() && this.knockDoor == null && this.lingerTicks == 0 && this.onGround() && !this.isInWater();
    }

    // The nearest closed door within reach that it hasn't knocked on lately (its lower half).
    @Nullable
    private BlockPos findDoor() {
        BlockPos here = this.blockPosition();
        long now = this.level().getGameTime();
        BlockPos best = null;
        double bestDistance = KNOCK_RANGE * KNOCK_RANGE + 1;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-KNOCK_RANGE, -2, -KNOCK_RANGE), here.offset(KNOCK_RANGE, 2, KNOCK_RANGE))) {
            BlockState state = this.level().getBlockState(p);
            if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
                    || state.getValue(DoorBlock.OPEN)) {
                continue;
            }
            Long last = this.knockedDoors.get(p);
            double distance = p.distToCenterSqr(this.position());
            if ((last == null || now - last >= SAME_DOOR_COOLDOWN) && distance < bestDistance) {
                best = p.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isClosedDoor(BlockPos pPos) {
        BlockState state = this.level().getBlockState(pPos);
        return state.getBlock() instanceof DoorBlock && !state.getValue(DoorBlock.OPEN);
    }

    // Where to stand to knock: in front of the door, on its own side of it.
    private Vec3 doorStep(BlockPos pDoor) {
        Direction facing = this.level().getBlockState(pDoor).getValue(DoorBlock.FACING);
        BlockPos a = pDoor.relative(facing), b = pDoor.relative(facing.getOpposite());
        BlockPos near = a.distToCenterSqr(this.position()) <= b.distToCenterSqr(this.position()) ? a : b;
        return Vec3.atBottomCenterOf(near);
    }

    // Close enough to knock: right up against the door (allowing for its width).
    private boolean atDoor(BlockPos pDoor) {
        double dx = pDoor.getX() + 0.5 - this.getX(), dz = pDoor.getZ() + 0.5 - this.getZ();
        double reach = 1.0 + this.getBbWidth() / 2.0 + 0.5;
        return dx * dx + dz * dz < reach * reach;
    }

    private void startKnock(BlockPos pDoor) {
        this.knockDoor = pDoor;
        this.knockTicks = 0;
        this.knockCooldown = KNOCK_COOLDOWN;
        this.knockedDoors.put(pDoor, this.level().getGameTime());
        this.getNavigation().stop();
        float yaw = (float) (Mth.atan2(pDoor.getZ() + 0.5 - this.getZ(), pDoor.getX() + 0.5 - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        String name = this.isOnFours() ? "knock_on_fours" : "knock_standing";
        this.playAction(name);
        this.holdTicks = Math.max(this.holdTicks, Mth.ceil(ACTION_SECONDS[ACTIONS.indexOf(name)] * 20));
    }

    // The knock's sounds, in step with its animation: three raps on the door, then (placeholder for now) its voice
    // through the door. Cut short if something sets it off.
    private void tickKnock() {
        if (this.knockDoor == null) {
            return;
        }
        if (this.getTarget() != null || this.isFleeing() || this.sleeping || this.leavingAtDawn || this.windupTicks > 0) {
            this.knockDoor = null;
            return;
        }
        this.knockTicks++;
        BlockPos door = this.knockDoor;
        for (int rap : KNOCK_RAP_TICKS) {
            if (this.knockTicks == rap) {
                this.level().playSound(null, door, ModSounds.skinwalker_knock.get(), SoundSource.HOSTILE, 1.0F,
                        0.95F + this.random.nextFloat() * 0.1F);
            }
        }
        if (this.knockTicks == KNOCK_VOICE_TICK) {
            this.level().playSound(null, door, ModSounds.skinwalker_door_voice.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            this.knockDoor = null;
            this.lingerDoor = door;
            this.lingerTicks = Mth.nextInt(this.random, LINGER_MIN_TICKS, LINGER_MAX_TICKS);
        }
    }

    // ---------------------------------------------------------------- /skinwalker sleep (testing)
    // Lies down after 5 more seconds of whatever it's doing (see SkinwalkerCommand). Not while it's leaving at
    // sunrise.
    public void makeSleepy() {
        if (this.isAlive() && !this.sleeping && !this.leavingAtDawn) {
            this.sleepyTicks = SLEEPY_TICKS;
        }
    }

    private void fallAsleep() {
        if (this.sleeping || this.leavingAtDawn) {
            return;
        }
        if (!this.canLieDownHere()) {
            this.sleepyTicks = 10;                  // still swimming or in the air: tries again once it's out
            return;
        }
        if (this.isLow()) {
            // gets up onto all fours first (once there's room), then lies down
            if (this.holdTicks == 0 && this.roomFor(ON_FOURS_SIZE, this.position())) {
                this.entityData.set(LOW, NOT_LOW);
                this.holdTicks = LOW_CHANGE_TICKS;
            }
            this.sleepyTicks = this.holdTicks + 1;
            return;
        }
        this.fleeTicks = 0;
        this.scaredBy = null;
        this.windupTicks = 0;
        this.rageTicks = 0;
        this.alertTicks = 0;
        this.retreatTicks = 0;
        this.alertAfterHold = null;
        this.holdTicks = 0;
        this.setTarget(null);
        this.lieDown();
    }

    // ---------------------------------------------------------------- sunrise (server side)
    private void tickDawn() {
        if (!this.isAlive()) {
            return;
        }
        boolean day = this.level().isDay();
        if (!day) {
            this.sawNight = true;
        } else if (this.sawNight && !this.leavingAtDawn) {
            // the sun's coming up: drop everything and go
            this.leavingAtDawn = true;
            if (this.sleeping) {
                this.wakeUp(null, false);
            }
            this.alertTicks = 0;
            this.retreatTicks = 0;
            this.windupTicks = 0;
            this.rageTicks = 0;
            this.fleeTicks = 0;
            this.circleTicks = 0;
            this.alertAfterHold = null;
            this.setTarget(null);
        }
        if (this.leavingAtDawn && this.tickCount % 10 == 0 && this.holdTicks == 0 && this.outOfSight()) {
            this.discard();
        }
    }

    private boolean outOfSight() {
        for (Player player : this.level().players()) {
            if (player.isSpectator()) {
                continue;
            }
            double distance = player.distanceTo(this);
            if (distance < DAWN_GONE_DISTANCE && (distance < DAWN_HIDDEN_DISTANCE || player.hasLineOfSight(this))) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- goals
    // Holds it in place while it sleeps, winds up to charge, screams, flinches, changes form or gets up, staring
    // at whoever it's reacting to. Its MOVE and LOOK flags keep the other goals from running meanwhile.
    private class HoldStillGoal extends Goal {
        HoldStillGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            return self.sleeping || self.windupTicks > 0 || self.holdTicks > 0 || self.circleTicks > 0;
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
            if (self.sleeping) {
                return;
            }
            LivingEntity target = self.getTarget() != null ? self.getTarget() : self.getLastHurtByMob();
            if (target != null) {
                self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    // Heading for the gap it has to squeeze through (see tickRoute), then getting down there.
    private class SqueezeGoal extends Goal {
        private int repath;

        SqueezeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            return self.squeezeAt != null && self.getLow() < self.squeezeForm && self.getTarget() != null;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            BlockPos at = self.squeezeAt;
            if (at == null) {
                return;
            }
            Vec3 spot = Vec3.atBottomCenterOf(at);
            double dx = spot.x - self.getX(), dz = spot.z - self.getZ();
            boolean there = dx * dx + dz * dz < 1.0 || self.getNavigation().isDone() && dx * dx + dz * dz < 4.0;
            if (there) {
                self.getNavigation().stop();
                self.getDownToSqueeze();
            } else if (--this.repath <= 0) {
                this.repath = 10;
                self.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
            }
        }
    }

    // After knocking (once it's back up): stays by the door, staring at it.
    private class LingerGoal extends Goal {
        LingerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            return self.lingerTicks > 0 && self.lingerDoor != null && self.holdTicks == 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            BlockPos door = self.lingerDoor;
            if (door != null) {
                self.getNavigation().stop();
                self.getLookControl().setLookAt(door.getX() + 0.5, door.getY() + 1.0, door.getZ() + 0.5);
            }
        }
    }

    // Roaming, it goes up to a closed door nearby and knocks (see startKnock and tickKnock).
    private class KnockGoal extends Goal {
        @Nullable
        private BlockPos door;
        private int ticks;

        KnockGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            if (self.knockCooldown > 0 || self.tickCount % 20 != 0 || !self.canKnock()) {
                return false;
            }
            this.door = self.findDoor();
            return this.door != null;
        }

        @Override
        public boolean canContinueToUse() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            return this.door != null && self.canKnock() && self.isClosedDoor(this.door) && this.ticks < KNOCK_APPROACH_TICKS;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public void stop() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            if (this.door != null && self.knockDoor == null) {
                self.knockedDoors.put(this.door, self.level().getGameTime());     // couldn't get to it: leaves it be
            }
            this.door = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            if (this.door == null) {
                return;
            }
            if (self.atDoor(this.door)) {
                self.startKnock(this.door);
                this.door = null;
            } else if (this.ticks++ % 20 == 0) {
                Vec3 step = self.doorStep(this.door);
                self.getNavigation().moveTo(step.x, step.y, step.z, 0.8);
            }
        }
    }

    // On alert: stands still searching for what it heard (looking toward the sound and around). If it sees a
    // player, a hunter goes after them and the rest calm down; if it finds nothing in 5 seconds, it runs off.
    private class AlertGoal extends Goal {
        AlertGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return SkinwalkerEntity.this.alertTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            self.getNavigation().stop();
            if (self.soundPos != null) {
                self.getLookControl().setLookAt(self.soundPos.x, self.soundPos.y + 1.5, self.soundPos.z);
            }
            for (Player seen : self.level().players()) {
                if (self.hasSeen(seen)) {
                    self.alertTicks = 0;            // found it
                    if (self.huntsOnSight && !seen.isCreative()) {
                        self.setTarget(seen);
                    }
                    return;
                }
            }
            if (--self.alertTicks == 0) {
                self.retreatTicks = RETREAT_TICKS;  // couldn't find it: get away from that sound
            }
        }
    }

    // At sunrise: runs off away from the nearest player at its normal run until it's out of sight (see tickDawn).
    private class LeaveAtDawnGoal extends Goal {
        private int repathCooldown;

        LeaveAtDawnGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return SkinwalkerEntity.this.leavingAtDawn;
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
            Player from = self.level().getNearestPlayer(self, DAWN_GONE_DISTANCE * 2);
            Vec3 away = from != null ? DefaultRandomPos.getPosAway(self, FLEE_RANGE, 10, from.position())
                    : DefaultRandomPos.getPos(self, FLEE_RANGE, 10);
            if (away != null) {
                self.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
            }
        }
    }

    // Runs off from a sound it couldn't find, at its normal run, for a few seconds.
    private class RetreatGoal extends Goal {
        RetreatGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return SkinwalkerEntity.this.retreatTicks > 0 && SkinwalkerEntity.this.soundPos != null;
        }

        @Override
        public void start() {
            SkinwalkerEntity self = SkinwalkerEntity.this;
            Vec3 away = DefaultRandomPos.getPosAway(self, RETREAT_RANGE, 7, self.soundPos);
            if (away != null) {
                self.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
            }
        }

        @Override
        public void stop() {
            SkinwalkerEntity.this.getNavigation().stop();
        }
    }

    // Runs flat out away from whoever scared it for the whole flee time, picking a fresh escape point far
    // from them whenever it arrives or every second (half a second from a Sandman), even when it can no longer see them.
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
            LivingEntity from = self.scaredBy;
            this.repathCooldown = from instanceof SandmanEntity ? 10 : 20;     // (from a Sandman it checks on her more often)
            if (from == null || !from.isAlive() || from.level() != self.level()) {
                from = self.level().getNearestPlayer(self, FLEE_RANGE * 2);
            }
            if (from == null) {
                return;
            }
            // (where there's nowhere that far, e.g. on a mountain, somewhere nearer)
            Vec3 away = DefaultRandomPos.getPosAway(self, FLEE_RANGE, 10, from.position());
            for (int range = FLEE_RANGE / 2; away == null && range >= 8; range /= 2) {
                away = DefaultRandomPos.getPosAway(self, range, 7, from.position());
            }
            if (away != null) {
                self.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }

    // ---------------------------------------------------------------- death
    // Its deaths take a few seconds to play out, so the body stays until its death animation is done.
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= this.deathTicks() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    private int deathTicks() {
        boolean fours = this.isOnFours();
        float seconds = this.entityData.get(DUST_DEATH)
                ? (fours ? DUST_DEATH_ON_FOURS_SECONDS : DUST_DEATH_SECONDS)
                : (fours ? DEATH_ON_FOURS_SECONDS : DEATH_SECONDS);
        return Mth.ceil(seconds * 20) + 20;
    }

    // ---------------------------------------------------------------- animation (GeckoLib)
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, BLEND_TICKS, this::animate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    // Client: the form it's in, as the animations name it.
    private String formName() {
        return switch (this.getLow()) {
            case STOOPING -> "stoop";
            case CREEPING -> "creep";
            case ON_BELLY -> "prone";
            default -> this.isOnFours() ? "crawl" : "stand";
        };
    }

    // Client: whether the head may turn toward what it's looking at (not while asleep, lying down or getting up).
    public boolean headTracks() {
        return !this.entityData.get(SLEEPING) && (this.shownAction == null
                || !(this.shownAction.startsWith("lie_down") || this.shownAction.startsWith("wake_")));
    }

    // Client: lying down on all fours, the game moves it around the circle, so the animation's own turn on the
    // spot is left out (see SkinwalkerModel).
    public boolean isCirclingToLieDown() {
        return "lie_down_on_fours".equals(this.shownAction);
    }

    // Picks what it's doing right now. Everything goes through this one controller, so every change blends
    // from wherever its body is into the next animation.
    private PlayState animate(AnimationState<SkinwalkerEntity> state) {
        boolean fours = this.isOnFours();
        String stance = fours ? "_on_fours" : "_standing";
        state.setControllerSpeed(1.0F);
        String form = this.formName();
        if (!this.formSeen) {
            this.formSeen = true;
            this.shownForm = form;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(hold((this.entityData.get(DUST_DEATH) ? "dust_death" : "death") + stance));
        }
        // a new one-off action from the server (ignoring whatever it last did before we first saw it)
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
            this.idleFlavor = null;
            state.getController().forceAnimationReset();
        }
        if (this.shownAction != null) {
            if (this.tickCount - this.actionStartedAt < Mth.ceil(ACTION_SECONDS[ACTIONS.indexOf(this.shownAction)] * 20)) {
                return state.setAndContinue(hold(this.shownAction));
            }
            this.shownAction = null;
            this.shownForm = form;                   // waking up already ends in its new form
        }
        if (this.entityData.get(SLEEPING)) {
            return state.setAndContinue(loop("sleep" + stance));
        }
        if (this.isInDeepWater()) {
            return state.setAndContinue(loop("swim"));
        }
        double climbSpeed = this.getY() - this.yo;
        if (this.isClimbing() && Math.abs(climbSpeed) > 0.01) {
            this.idleFlavor = null;
            state.setControllerSpeed((float) Mth.clamp(Math.abs(climbSpeed) / CLIMB_SPEED, 0.6, 2.0));
            return state.setAndContinue(loop(climbSpeed > 0 ? "climb_up" : "climb_down"));
        }
        // changing form: from standing, all fours or a low form to another (each pair has its own animation)
        if (!form.equals(this.shownForm)) {
            this.formFrom = this.shownForm;
            this.shownForm = form;
            this.formChangedAt = this.tickCount;
            this.idleFlavor = null;
        }
        boolean upOrDown = (this.formFrom + form).equals("standcrawl") || (this.formFrom + form).equals("crawlstand");
        if (this.tickCount - this.formChangedAt < (upOrDown ? STANCE_CHANGE_TICKS : LOW_CHANGE_TICKS)) {
            return state.setAndContinue(once(this.formFrom + "_to_" + form));
        }
        double speed = Math.sqrt(Mth.square(this.getX() - this.xo) + Mth.square(this.getZ() - this.zo));
        if (this.isLow()) {
            this.idleFlavor = null;
            if (this.getLow() == ON_BELLY) {
                // drags itself along on its belly; lying still, it holds the crawl wherever it stopped
                state.setControllerSpeed(speed > 0.005 ? (float) Mth.clamp(speed / CRAWL_SPEED, 0.3, 2.5) : 0.0F);
                return state.setAndContinue(loop("crawl_prone"));
            }
            String move = this.getLow() == CREEPING ? "creep" : "stoop";
            if (speed > 0.01) {
                double animSpeed = this.getLow() == CREEPING ? CREEP_SPEED : STOOP_SPEED;
                state.setControllerSpeed((float) Mth.clamp(speed / animSpeed, 0.5, 2.5));
                return state.setAndContinue(loop(move));
            }
            return state.setAndContinue(loop("rest_" + move));
        }
        if (speed > 0.02) {
            this.idleFlavor = null;
            boolean fleeing = this.entityData.get(FLEEING);
            boolean running = this.entityData.get(RUNNING) || this.isAggressive();
            String gait = fleeing ? "flee" : running ? "run" : "walk";
            double animSpeed = fleeing ? FLEE_SPEED_ANIM : running ? RUN_SPEED : WALK_SPEED;
            state.setControllerSpeed((float) Mth.clamp(speed / animSpeed, 0.4, 2.0));
            return state.setAndContinue(loop(gait + stance));
        }
        if (this.entityData.get(ALERT)) {
            // perks up, then looks around for the sound
            this.idleFlavor = null;
            return state.setAndContinue(ALERTS.computeIfAbsent(stance, s -> RawAnimation.begin()
                    .thenPlay(fours ? "alert_on_fours" : "alert").thenLoop(fours ? "look_around_on_fours" : "look_around")));
        }
        if (this.idleFlavor != null) {
            return state.setAndContinue(once(this.idleFlavor + stance));
        }
        return state.setAndContinue(loop("rest" + stance));
    }

    // Client side, once a tick: whether to sniff, tilt its head or tug at its skin while standing still.
    private void tickIdleFlavor() {
        if (this.idleFlavor != null && this.tickCount >= this.idleFlavorUntil) {
            this.idleFlavor = null;
        }
        boolean still = Math.abs(this.getX() - this.xo) + Math.abs(this.getZ() - this.zo) < 0.01;
        boolean busy = this.entityData.get(SLEEPING) || this.entityData.get(ALERT) || this.shownAction != null || this.isLow();
        if (this.idleFlavor == null && still && !busy && this.isAlive() && this.random.nextInt(IDLE_FLAVOR_CHANCE) == 0) {
            float seconds;
            switch (this.random.nextInt(3)) {
                case 0 -> { this.idleFlavor = "sniff"; seconds = this.isOnFours() ? 2.8F : 2.4F; }
                case 1 -> { this.idleFlavor = "head_tilt"; seconds = 2.6F; }
                default -> { this.idleFlavor = "adjust_skin"; seconds = this.isOnFours() ? 2.9F : 3.1F; }
            }
            this.idleFlavorUntil = this.tickCount + Mth.ceil(seconds * 20);
        }
    }

    private static RawAnimation loop(String name) {
        return LOOPS.computeIfAbsent(name, n -> RawAnimation.begin().thenLoop(n));
    }

    private static RawAnimation once(String name) {
        return ONCE.computeIfAbsent(name, n -> RawAnimation.begin().thenPlay(n));
    }

    private static RawAnimation hold(String name) {
        return HOLD.computeIfAbsent(name, n -> RawAnimation.begin().thenPlayAndHold(n));
    }

    // ---------------------------------------------------------------- water
    // How deep water has to be before it swims (FloatGoal) instead of wading: vanilla's 0.4 blocks is for mobs
    // the size of a player, and made it bob up and down on the spot in any puddle. About half its height, at most
    // 1.2 blocks (so standing, on all fours or on its belly it wades through shallow water and swims in deep).
    @Override
    public double getFluidJumpThreshold() {
        return Math.min(1.2, this.getBbHeight() * 0.45);
    }

    // Deep enough to swim in (see getFluidJumpThreshold).
    private boolean isInDeepWater() {
        return this.isInWater() && this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold();
    }

    // ---------------------------------------------------------------- hitbox
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pPose) {
        if (this.entityData.get(SLEEPING)) {
            return ASLEEP_SIZE;
        }
        if (this.isLow()) {
            return LOW_SIZES[this.getLow()];
        }
        return this.isOnFours() ? ON_FOURS_SIZE : STANDING_SIZE;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (ON_FOURS.equals(pKey) || SLEEPING.equals(pKey) || LOW.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        super.setTarget(this.isFleeing() || this.leavingAtDawn ? null : pTarget);
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
        pCompound.putBoolean("OnFours", this.isOnFours());
        pCompound.putBoolean("Sleeping", this.sleeping);
        pCompound.putInt("CalmTicks", this.calmTicks);
        pCompound.putBoolean("ChangedForm", this.changedForm);
        pCompound.putBoolean("SawNight", this.sawNight);
        pCompound.putBoolean("LeavingAtDawn", this.leavingAtDawn);
        pCompound.putInt("SaltHits", this.saltHits);
        pCompound.putInt("DustBallHits", this.dustBallHits);
        pCompound.putInt("FleeTicks", this.fleeTicks);
        pCompound.putInt("WindupTicks", this.windupTicks);
        pCompound.putInt("RageTicks", this.rageTicks);
        pCompound.putBoolean("ReactedToHit", this.reactedToHit);
        pCompound.putInt("Low", this.getLow());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.huntsOnSight = pCompound.getBoolean("HuntsOnSight");
        if (pCompound.contains("OnFours")) {
            this.entityData.set(ON_FOURS, pCompound.getBoolean("OnFours"));
        }
        this.sleeping = pCompound.getBoolean("Sleeping");
        this.calmTicks = pCompound.getInt("CalmTicks");
        this.changedForm = pCompound.getBoolean("ChangedForm");
        this.sawNight = pCompound.getBoolean("SawNight");
        this.leavingAtDawn = pCompound.getBoolean("LeavingAtDawn");
        this.saltHits = pCompound.getInt("SaltHits");
        this.dustBallHits = pCompound.getInt("DustBallHits");
        this.fleeTicks = pCompound.getInt("FleeTicks");
        this.windupTicks = pCompound.getInt("WindupTicks");
        this.rageTicks = pCompound.getInt("RageTicks");
        this.reactedToHit = pCompound.getBoolean("ReactedToHit");
        // (saved before the stooping and creeping forms: "Prone", on its belly or not)
        this.entityData.set(LOW, pCompound.contains("Low") ? Mth.clamp(pCompound.getInt("Low"), NOT_LOW, ON_BELLY)
                : pCompound.getBoolean("Prone") ? ON_BELLY : NOT_LOW);
    }

    // Plans paths as if it were 1 block wide and pHeight blocks high (a low form), to find out whether a gap too
    // small for it as it is would get it to where it's going. Only used to plan, never to move.
    private static class LowNavigation extends GroundPathNavigation {
        private final int height;

        LowNavigation(Mob pMob, Level pLevel, int pHeight) {
            super(pMob, pLevel);
            this.height = pHeight;
        }

        @Override
        protected PathFinder createPathFinder(int pMaxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator() {
                @Override
                public void prepare(PathNavigationRegion pRegion, Mob pMob) {
                    super.prepare(pRegion, pMob);
                    this.entityWidth = 1;
                    this.entityHeight = LowNavigation.this.height;      // (read here: set after the constructor)
                    this.entityDepth = 1;
                }
            };
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
        }
    }
}
