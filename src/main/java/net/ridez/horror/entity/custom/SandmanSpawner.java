package net.ridez.horror.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.worldgen.HolyPlaces;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// Where Sandmen turn up (Overworld only, any time of day): around players, now and then a Sandman appears on the
// streets of a village they're near (at most 2 in a village), or, more rarely, in the grounds of a pillager outpost or
// woodland mansion (the horrormod:sandman_watches structure tag), outside the buildings (at most 1). They wander off
// and despawn like other mobs once no one is near.
// A player whose record (SandmanEntity.getRecord) has reached RECORD_SOUGHT is sought anywhere: Sandmen are sent after
// them out in the open world, coming straight for them, more often the bigger the record (at 10 points about every
// 7 minutes, from 50 points about every minute), one at a time (two at once from 50 points).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class SandmanSpawner {
    public static final TagKey<Structure> WATCHES = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "sandman_watches"));
    private static final int CHECK_INTERVAL = 200;            // every 10 seconds per player
    private static final int VILLAGE_CHANCE = 4, WATCH_CHANCE = 8;
    private static final int VILLAGE_MAX = 2, WATCH_MAX = 1;
    private static final double MIN_DISTANCE = 20.0, MAX_DISTANCE = 56.0, COUNT_RANGE = 72.0;
    private static final int SOUGHT_FULL = 50;                // the record at which they're sent most often
    private static final int SOUGHT_CHANCE_LOW = 40, SOUGHT_CHANCE_HIGH = 6;   // 1 in this many checks, at RECORD_SOUGHT / SOUGHT_FULL
    private static final double SENT_MIN_DISTANCE = 20.0, SENT_MAX_DISTANCE = 36.0;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                trySpawn(level, player);
                trySendFor(level, player);
            }
        }
    }

    // A spot on open ground at a random distance from the player, or null.
    @Nullable
    private static BlockPos randomSpot(ServerLevel pLevel, ServerPlayer pPlayer, double pMin, double pMax) {
        RandomSource random = pLevel.random;
        double angle = random.nextDouble() * Math.PI * 2, distance = Mth.lerp(random.nextDouble(), pMin, pMax);
        int x = Mth.floor(pPlayer.getX() + Math.cos(angle) * distance), z = Mth.floor(pPlayer.getZ() + Math.sin(angle) * distance);
        if (!pLevel.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        BlockPos feet = new BlockPos(x, pLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return standable(pLevel, feet) && !HolyPlaces.inside(pLevel, feet) ? feet : null;      // (never in a holy place)
    }

    private static void trySpawn(ServerLevel pLevel, ServerPlayer pPlayer) {
        RandomSource random = pLevel.random;
        BlockPos feet = randomSpot(pLevel, pPlayer, MIN_DISTANCE, MAX_DISTANCE);
        if (feet == null) {
            return;
        }
        StructureManager structures = pLevel.structureManager();
        if (pLevel.isVillage(feet)) {                          // (among the villagers' beds and workplaces)
            if (random.nextInt(VILLAGE_CHANCE) == 0 && sandmenNear(pLevel, feet) < VILLAGE_MAX) {
                spawn(pLevel, feet);
            }
            return;
        }
        Optional<HolderSet.Named<Structure>> watched = pLevel.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(WATCHES);
        if (watched.isEmpty() || random.nextInt(WATCH_CHANCE) != 0) {
            return;
        }
        for (Holder<Structure> structure : watched.get()) {
            // in the structure's grounds but not inside one of its buildings
            if (structures.getStructureAt(feet, structure.value()).isValid()
                    && !structures.getStructureWithPieceAt(feet, structure.value()).isValid()) {
                if (sandmenNear(pLevel, feet) < WATCH_MAX) {
                    spawn(pLevel, feet);
                }
                return;
            }
        }
    }

    // A Sandman sent after a sought player, wherever they are.
    private static void trySendFor(ServerLevel pLevel, ServerPlayer pPlayer) {
        int record = SandmanEntity.getRecord(pPlayer);
        if (record < SandmanEntity.RECORD_SOUGHT || pPlayer.isCreative()) {
            return;
        }
        float u = Mth.clamp((record - SandmanEntity.RECORD_SOUGHT) / (float) (SOUGHT_FULL - SandmanEntity.RECORD_SOUGHT), 0.0F, 1.0F);
        if (pLevel.random.nextInt(Math.round(Mth.lerp(u, SOUGHT_CHANCE_LOW, SOUGHT_CHANCE_HIGH))) != 0) {
            return;
        }
        int after = pLevel.getEntities(ModEntities.sandman.get(), sandman -> sandman.getTarget() == pPlayer).size();
        if (after >= (record >= SOUGHT_FULL ? 2 : 1)) {
            return;
        }
        BlockPos feet = randomSpot(pLevel, pPlayer, SENT_MIN_DISTANCE, SENT_MAX_DISTANCE);
        if (feet != null) {
            SandmanEntity sandman = spawn(pLevel, feet);
            if (sandman != null) {
                sandman.setTarget(pPlayer);
            }
        }
    }

    private static int sandmenNear(ServerLevel pLevel, BlockPos pPos) {
        return pLevel.getEntities(ModEntities.sandman.get(), sandman -> sandman.distanceToSqr(pPos.getCenter()) < COUNT_RANGE * COUNT_RANGE).size();
    }

    private static boolean standable(ServerLevel pLevel, BlockPos pFeet) {
        BlockState below = pLevel.getBlockState(pFeet.below());
        // on open ground (grass, dirt, paths, gravel, sand), not on a roof or in a tree
        boolean ground = below.is(BlockTags.DIRT) || below.is(Blocks.DIRT_PATH) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL);
        if (!ground || !below.isFaceSturdy(pLevel, pFeet.below(), Direction.UP) || !below.getFluidState().isEmpty()) {
            return false;
        }
        for (int dy = 0; dy < 2; dy++) {
            BlockPos p = pFeet.above(dy);
            if (!pLevel.getBlockState(p).getCollisionShape(pLevel, p).isEmpty() || !pLevel.getFluidState(p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static SandmanEntity spawn(ServerLevel pLevel, BlockPos pFeet) {
        SandmanEntity sandman = ModEntities.sandman.get().create(pLevel);
        if (sandman == null) {
            return null;
        }
        sandman.moveTo(pFeet.getX() + 0.5, pFeet.getY(), pFeet.getZ() + 0.5, pLevel.random.nextFloat() * 360.0F, 0.0F);
        sandman.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(pFeet), MobSpawnType.EVENT, null);
        pLevel.addFreshEntity(sandman);
        return sandman;
    }
}
