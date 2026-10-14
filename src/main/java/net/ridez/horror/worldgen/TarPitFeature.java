package net.ridez.horror.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;

// A tar pit: the plain pool (worldgen/configured_feature/tar_pit_pool.json, vanilla's lava pool filled
// with Tar) plus a dead, eerie rim: bare coarse dirt, dead bushes, bones, cobwebs, a skull and leafless
// trees. The rim is only decorated if the pool actually generated.
public class TarPitFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<ConfiguredFeature<?, ?>> POOL = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "tar_pit_pool"));
    // The pool fills the 16x16 area starting at the origin, so its middle is 8 blocks in.
    private static final int POOL_CENTER_OFFSET = 8;
    // Keeps every change within the neighboring chunks a feature is allowed to edit.
    private static final int RIM_RADIUS = 7;

    public TarPitFeature(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
        WorldGenLevel level = pContext.level();
        RandomSource random = pContext.random();
        BlockPos origin = pContext.origin();

        ConfiguredFeature<?, ?> pool = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(POOL);
        if (pool == null || !pool.place(level, pContext.chunkGenerator(), random, origin)) {
            return false;
        }

        BlockPos center = origin.offset(POOL_CENTER_OFFSET, 0, POOL_CENTER_OFFSET);
        decorateGround(level, random, center);
        scatterBones(level, random, center);
        scatterCobwebs(level, random, center);
        placeSkull(level, random, center);
        int trees = 1 + random.nextInt(2);
        for (int i = 0; i < trees; i++) {
            placeDeadTree(level, random, center);
        }
        return true;
    }

    // Patches of bare coarse dirt with dead bushes growing out of them.
    private static void decorateGround(WorldGenLevel level, RandomSource random, BlockPos center) {
        for (int i = 0; i < 24; i++) {
            BlockPos ground = randomGround(level, random, center);
            if (ground == null) {
                continue;
            }
            if (random.nextInt(3) != 0) {
                level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), 2);
            }
            BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
            if (random.nextInt(3) == 0 && bush.canSurvive(level, ground.above())) {
                level.setBlock(ground.above(), bush, 2);
            }
        }
    }

    // Bone blocks, some half-buried in the ground and some lying on top of it.
    private static void scatterBones(WorldGenLevel level, RandomSource random, BlockPos center) {
        int bones = 2 + random.nextInt(3);
        for (int i = 0; i < bones; i++) {
            BlockPos ground = randomGround(level, random, center);
            if (ground == null) {
                continue;
            }
            Direction.Axis axis = Direction.Axis.values()[random.nextInt(3)];
            BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
            level.setBlock(random.nextBoolean() ? ground : ground.above(), bone, 2);
        }
    }

    private static void scatterCobwebs(WorldGenLevel level, RandomSource random, BlockPos center) {
        int webs = random.nextInt(3);
        for (int i = 0; i < webs; i++) {
            BlockPos ground = randomGround(level, random, center);
            if (ground != null) {
                level.setBlock(ground.above(), Blocks.COBWEB.defaultBlockState(), 2);
            }
        }
    }

    // About half of pits get a skeleton skull facing a random way.
    private static void placeSkull(WorldGenLevel level, RandomSource random, BlockPos center) {
        if (random.nextBoolean()) {
            BlockPos ground = randomGround(level, random, center);
            if (ground != null) {
                BlockState skull = Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, random.nextInt(16));
                level.setBlock(ground.above(), skull, 2);
            }
        }
    }

    // A bare dark oak trunk with one or two stubby branches and no leaves.
    private static void placeDeadTree(WorldGenLevel level, RandomSource random, BlockPos center) {
        BlockPos ground = randomGround(level, random, center);
        if (ground == null) {
            return;
        }
        int height = 3 + random.nextInt(3);
        for (int y = 1; y <= height; y++) {
            BlockPos trunk = ground.above(y);
            if (!level.ensureCanWrite(trunk) || !level.isEmptyBlock(trunk)) {
                return;
            }
        }
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 2);
        for (int y = 1; y <= height; y++) {
            level.setBlock(ground.above(y), log, 2);
        }
        int branches = 1 + random.nextInt(2);
        for (int i = 0; i < branches; i++) {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos branch = ground.above(height - random.nextInt(2)).relative(direction);
            if (level.ensureCanWrite(branch) && level.isEmptyBlock(branch)) {
                level.setBlock(branch, log.setValue(RotatedPillarBlock.AXIS, direction.getAxis()), 2);
            }
        }
    }

    // A random dirt-like surface block near the pool with open air above it (never the Tar itself),
    // or null if the spot isn't usable.
    @Nullable
    private static BlockPos randomGround(WorldGenLevel level, RandomSource random, BlockPos center) {
        int dx = random.nextInt(RIM_RADIUS * 2 + 1) - RIM_RADIUS;
        int dz = random.nextInt(RIM_RADIUS * 2 + 1) - RIM_RADIUS;
        if (dx * dx + dz * dz > RIM_RADIUS * RIM_RADIUS) {
            return null;
        }
        BlockPos column = center.offset(dx, 0, dz);
        if (!level.ensureCanWrite(column)) {
            return null;
        }
        BlockPos above = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, column);
        BlockPos ground = above.below();
        if (!level.ensureCanWrite(ground) || !level.ensureCanWrite(above) || !level.isEmptyBlock(above)
                || !isDirt(level.getBlockState(ground))) {
            return null;
        }
        return ground;
    }
}
