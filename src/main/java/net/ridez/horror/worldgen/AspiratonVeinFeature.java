package net.ridez.horror.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraftforge.common.Tags;
import net.ridez.horror.block.ModBlocks;

import java.util.Arrays;

// The world's blood veins: endless winding tubes running underground like veins under skin. From the
// middle out: a channel of Nutrinon (still source blocks, wide enough to swim through) whose level rises
// and falls along the vein, from brim-full to a shallow stream with air above; a wall of Aspiraton Block;
// and a ragged outer layer of Veinseep (Deepslate Veinseep in deepslate) that has gaps and loose bits,
// like pieces of the vein flaking off.
//
// How: two smooth 3D noise fields, seeded from the world seed. Where both are zero at once is a thin
// winding curve that never ends and lines up across chunks (the same trick as vanilla's "spaghetti"
// caves). Each block's distance from that curve, in blocks, is worked out from the noise values and
// their slopes, so the tubes keep a steady round cross-section however the two surfaces cross. A third,
// finer noise varies the Veinseep's thickness and a fourth the Nutrinon level. Runs once per chunk and
// only edits that chunk.
// The channel and wall are built through open air and water too, so a vein crossing a cave stays sealed
// (a bare Aspiraton tube, since Veinseep only forms in rock) and never spills. Structures are left alone.
public class AspiratonVeinFeature extends Feature<NoneFeatureConfiguration> {
    // How far apart the veins are: one noise "cell" spans this many blocks. Bigger = fewer, longer veins.
    // 256 x 96 gives roughly 3-7 sizeable veins in a 384x384-block area (what a spectator sees).
    private static final double HORIZONTAL_SCALE = 256.0;
    // Smaller than horizontal so veins wind mostly sideways, like veins under skin, rather than up and down.
    private static final double VERTICAL_SCALE = 96.0;
    // Layer sizes in blocks from the vein's centre line: the Nutrinon channel, then the Aspiraton wall.
    private static final double CHANNEL_RADIUS = 3.0;
    private static final double WALL_RADIUS = 4.2;
    // The Veinseep layer outside the wall is 0 to this many blocks thick, varying in patches about
    // FLAKE_SIZE blocks across. FLAKE_COVERAGE (0-1) is roughly how much of the wall is covered.
    private static final double FLAKE_MAX_THICKNESS = 2.6;
    private static final double FLAKE_SIZE = 5.0;
    private static final double FLAKE_COVERAGE = 0.65;
    private static final double OUTER_RADIUS = WALL_RADIUS + FLAKE_MAX_THICKNESS;
    // The Nutrinon level changes over stretches about FILL_SIZE blocks long. Below FILL_EMPTIEST it's a
    // shallow stream along the bottom; above FILL_FULLEST it's full. Values are fractions of the channel
    // radius measured from the vein's centre line (-1 = bottom, 0 = half full, 1 = top).
    private static final double FILL_SIZE = 40.0;
    private static final double FILL_EMPTIEST = -0.85;
    private static final double FILL_FULLEST = 1.1;
    // Shape how often each level shows up: with these, about 40% of the channel is Nutrinon overall, with
    // both full and shallow stretches (raise FILL_BIAS for more blood, lower it for less).
    private static final double FILL_BIAS = 0.35;
    private static final double FILL_SPREAD = 1.4;
    // Height band the veins run through; they taper off over FADE blocks at the top and bottom.
    private static final int MIN_Y = -56;
    private static final int MAX_Y = 48;
    private static final double FADE = 12.0;
    // Keeps these noises different from any other feature seeded from the world seed.
    private static final long SEED_SALT = 0x6A5F3C0E15A11B7DL;
    private record Noises(long seed, ImprovedNoise first, ImprovedNoise second, ImprovedNoise flakes, ImprovedNoise fill) {}

    // Generation runs on several threads; the noises are read-only once built, so sharing one is safe.
    private static volatile Noises cachedNoises;

    public AspiratonVeinFeature(Codec<NoneFeatureConfiguration> pCodec) {
        super(pCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
        WorldGenLevel level = pContext.level();
        ChunkPos chunk = new ChunkPos(pContext.origin());
        Noises noises = noisesFor(level.getSeed());
        int minY = Math.max(MIN_Y, level.getMinBuildHeight());
        int maxY = Math.min(MAX_Y, level.getMaxBuildHeight() - 1);

        BlockState nutrinon = ModBlocks.nutrinon.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wall = ModBlocks.aspiraton_block.get().defaultBlockState();
        BlockState flake = ModBlocks.veinseep.get().defaultBlockState();
        BlockState deepFlake = ModBlocks.veinseep_deepslate.get().defaultBlockState();
        double[] slopeFirst = new double[3];
        double[] slopeSecond = new double[3];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = minY; y <= maxY; y++) {
                    double fade = Math.min(1.0, Math.min(y - minY, maxY - y) / FADE);
                    double outerRadius = OUTER_RADIUS * fade;
                    if (outerRadius <= 0.0) {
                        continue;
                    }
                    double nx = x / HORIZONTAL_SCALE;
                    double ny = y / VERTICAL_SCALE;
                    double nz = z / HORIZONTAL_SCALE;
                    // noiseWithDerivative adds the slope into the array rather than overwriting it, so clear it first.
                    Arrays.fill(slopeFirst, 0.0);
                    double first = noises.first().noiseWithDerivative(nx, ny, nz, slopeFirst);
                    // Cheap early out: the curve can't be closer than the first noise's zero surface.
                    if (blocksToZero(first, slopeFirst) > outerRadius) {
                        continue;
                    }
                    Arrays.fill(slopeSecond, 0.0);
                    double second = noises.second().noiseWithDerivative(nx, ny, nz, slopeSecond);
                    double distance = blocksToCurve(first, slopeFirst, second, slopeSecond);
                    if (distance > outerRadius) {
                        continue;
                    }

                    pos.set(x, y, z);
                    BlockState current = level.getBlockState(pos);
                    if (distance <= CHANNEL_RADIUS * fade) {
                        if (isNaturalOrOpen(current)) {
                            double height = heightAboveCentre(first, slopeFirst, second, slopeSecond);
                            boolean submerged = height <= fillLevel(noises.fill(), x, y, z) * CHANNEL_RADIUS * fade;
                            level.setBlock(pos, submerged ? nutrinon : air, 2);
                            placed = true;
                        }
                    } else if (distance <= WALL_RADIUS * fade) {
                        if (isNaturalOrOpen(current)) {
                            level.setBlock(pos, wall, 2);
                            placed = true;
                        }
                    } else if (distance <= (WALL_RADIUS + flakeThickness(noises.flakes(), x, y, z)) * fade) {
                        boolean stone = current.is(BlockTags.STONE_ORE_REPLACEABLES);
                        boolean deepslate = current.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
                        if (stone || deepslate) {
                            level.setBlock(pos, deepslate ? deepFlake : flake, 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    // What a vein may cut through: stone, deepslate and the like, any ores in them (including other mods'),
    // and open cave air, water and lava, so the tube stays sealed. Leaves structures and surface blocks alone.
    private static boolean isNaturalOrOpen(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES)
                || state.is(ModBlocks.veinseep.get()) || state.is(ModBlocks.veinseep_deepslate.get())
                || state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    // How high the Nutrinon reaches here, as a fraction of the channel radius from the centre line.
    private static double fillLevel(ImprovedNoise fill, int x, int y, int z) {
        double value = fill.noise(x / FILL_SIZE, y / FILL_SIZE, z / FILL_SIZE);
        // Noise rarely strays far from 0, so stretch it to reach both the shallow and the full extremes.
        double t = Math.max(0.0, Math.min(1.0, FILL_BIAS + value * FILL_SPREAD));
        return FILL_EMPTIEST + (FILL_FULLEST - FILL_EMPTIEST) * t;
    }

    // How far above the vein's centre line this point is, in blocks: the vertical part of the offset from the
    // nearest point on the curve, solved from the same flat-slope approximation as blocksToCurve.
    private static double heightAboveCentre(double first, double[] slopeFirst, double second, double[] slopeSecond) {
        double ff = dot(slopeFirst, slopeFirst);
        double ss = dot(slopeSecond, slopeSecond);
        double fs = dot(slopeFirst, slopeSecond);
        double determinant = ff * ss - fs * fs;
        if (determinant < 1.0E-12) {
            return 0.0;
        }
        double alongFirst = (first * ss - second * fs) / determinant;
        double alongSecond = (second * ff - first * fs) / determinant;
        return alongFirst * slopeFirst[1] / VERTICAL_SCALE + alongSecond * slopeSecond[1] / VERTICAL_SCALE;
    }

    // How thick the Veinseep is here: 0 (bare wall) in some patches, up to FLAKE_MAX_THICKNESS in others.
    private static double flakeThickness(ImprovedNoise flakes, int x, int y, int z) {
        double value = flakes.noise(x / FLAKE_SIZE, y / FLAKE_SIZE, z / FLAKE_SIZE);
        double covered = (value + 1.0) * 0.5 - (1.0 - FLAKE_COVERAGE);
        return Math.max(0.0, Math.min(1.0, covered / FLAKE_COVERAGE * 1.6)) * FLAKE_MAX_THICKNESS;
    }

    // Approximate distance in blocks from this point to where one noise is zero: the value divided by
    // how fast it changes per block. Infinite where the noise is flat, so no vein forms there.
    private static double blocksToZero(double value, double[] slope) {
        double steepness = Math.sqrt(dot(slope, slope));
        return steepness < 1.0E-9 ? Double.POSITIVE_INFINITY : Math.abs(value) / steepness;
    }

    // Approximate distance in blocks from this point to the curve where both noises are zero, treating each
    // noise as a flat slope nearby. Unlike adding the two distances, this stays right when the surfaces
    // cross at a shallow angle (which would otherwise flatten the tube into a wide ribbon).
    private static double blocksToCurve(double first, double[] slopeFirst, double second, double[] slopeSecond) {
        double ff = dot(slopeFirst, slopeFirst);
        double ss = dot(slopeSecond, slopeSecond);
        double fs = dot(slopeFirst, slopeSecond);
        double determinant = ff * ss - fs * fs;
        if (determinant < 1.0E-12) {
            return Double.POSITIVE_INFINITY;
        }
        double squared = (first * first * ss - 2.0 * first * second * fs + second * second * ff) / determinant;
        return Math.sqrt(Math.max(0.0, squared));
    }

    // Dot product of two noise slopes, converted from noise units to per-block.
    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] / (HORIZONTAL_SCALE * HORIZONTAL_SCALE)
                + a[1] * b[1] / (VERTICAL_SCALE * VERTICAL_SCALE)
                + a[2] * b[2] / (HORIZONTAL_SCALE * HORIZONTAL_SCALE);
    }

    private static Noises noisesFor(long seed) {
        Noises noises = cachedNoises;
        if (noises == null || noises.seed() != seed) {
            RandomSource random = new XoroshiroRandomSource(seed ^ SEED_SALT);
            noises = new Noises(seed, new ImprovedNoise(random), new ImprovedNoise(random), new ImprovedNoise(random), new ImprovedNoise(random));
            cachedNoises = noises;
        }
        return noises;
    }
}
