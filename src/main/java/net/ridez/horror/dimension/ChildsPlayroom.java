package net.ridez.horror.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.block.ModBlocks;

// The Child's Playroom, the Angel's dimension (data: dimension/childs_playroom.json, an empty world, and
// dimension_type/childs_playroom.json: always midday). In it, built the first time anyone goes in: one great boxed-in
// room made to look like outdoors, its floor a grassy yard rolling in low hills (a dirt path winding through it to
// the house, grass and flowers here and there), its walls painted with trees against the sky, its ceiling
// a painted sky with blocky clouds. The ceiling lets daylight through (PaintedSkyBlock), so the yard is lit evenly
// by the sky with no shadows, while anything roofed over (the dollhouse) is dark inside. (Still to come in it, from the design: the dollhouse against the back (north) wall, broken and
// haunted inside, the playground filling the yard, and the two ways out, the infected portal in the house's
// basement and the slide into the outside wall, both made of the playroom exit block.)
// Going in (through the Angel's open portal) remembers where the player was; going out puts them back there.
public final class ChildsPlayroom {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "childs_playroom"));
    public static final int HALF = 48;                      // the room runs -HALF..HALF across, x and z
    public static final int FLOOR = 63;                     // the grass is at this y; the room stands on it
    public static final int HEIGHT = 64;                    // from the floor to the ceiling (room for the dollhouse)
    private static final String RETURN = "horrormod_playroom_return";
    // where players arrive: near the front (south) wall, facing the house at the back
    private static final Vec3 ARRIVE = new Vec3(0.5, FLOOR + 1, HALF - 6 + 0.5);

    private ChildsPlayroom() {
    }

    public static boolean isIn(Level pLevel) {
        return pLevel.dimension() == LEVEL;
    }

    // Through the Angel's portal: into the playroom (built if it isn't yet), remembering where they came from.
    public static void sendIn(ServerPlayer pPlayer) {
        ServerLevel room = pPlayer.server.getLevel(LEVEL);
        if (room == null || isIn(pPlayer.level())) {
            return;
        }
        CompoundTag back = new CompoundTag();
        back.putString("Dimension", pPlayer.level().dimension().location().toString());
        back.putDouble("X", pPlayer.getX());
        back.putDouble("Y", pPlayer.getY());
        back.putDouble("Z", pPlayer.getZ());
        back.putFloat("Yaw", pPlayer.getYRot());
        pPlayer.getPersistentData().put(RETURN, back);
        build(room);
        pPlayer.teleportTo(room, ARRIVE.x, ARRIVE.y, ARRIVE.z, 180.0F, 0.0F);
        pPlayer.setPortalCooldown();
    }

    // Out of the playroom: back where they went in (or to the world's spawn, if that's lost).
    public static void sendBack(ServerPlayer pPlayer) {
        if (!isIn(pPlayer.level())) {
            return;
        }
        CompoundTag back = pPlayer.getPersistentData().getCompound(RETURN);
        ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
        ServerLevel to = id != null ? pPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) : null;
        if (to == null || !back.contains("X")) {
            to = pPlayer.server.overworld();
            BlockPos spawn = to.getSharedSpawnPos();
            pPlayer.teleportTo(to, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, pPlayer.getYRot(), 0.0F);
        } else {
            pPlayer.teleportTo(to, back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"), back.getFloat("Yaw"), 0.0F);
        }
        pPlayer.getPersistentData().remove(RETURN);
        pPlayer.setPortalCooldown();
    }

    // ---------------------------------------------------------------- building it
    // Which version of the room is built (kept with the dimension's saved data); a room built by an older version is
    // cleared and built again. Raise ROOM_VERSION when the room's layout changes.
    private static final int ROOM_VERSION = 9;
    private static final int CLEAR_BELOW = 14;

    private static class Built extends SavedData {
        int version;

        static Built load(CompoundTag pTag, HolderLookup.Provider pRegistries) {
            Built built = new Built();
            built.version = pTag.getInt("Version");
            return built;
        }

        @Override
        public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
            pTag.putInt("Version", this.version);
            return pTag;
        }
    }

    private static void build(ServerLevel pRoom) {
        Built built = pRoom.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Built::new, Built::load, null), "horrormod_playroom");
        if (built.version == ROOM_VERSION) {
            return;
        }
        int edge = HALF + 1, top = FLOOR + HEIGHT + 1;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        if (built.version != 0 || !pRoom.getBlockState(at.set(0, FLOOR, 0)).isAir()) {      // an older room: cleared out
            for (int x = -HALF; x <= HALF; x++) {
                for (int z = -HALF; z <= HALF; z++) {
                    for (int y = FLOOR - CLEAR_BELOW; y < top; y++) {          // (down past the house's basement too)
                        if (!pRoom.getBlockState(at.set(x, y, z)).isAir()) {
                            pRoom.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState();
        for (int x = -edge; x <= edge; x++) {
            for (int z = -edge; z <= edge; z++) {
                boolean side = Math.abs(x) == edge || Math.abs(z) == edge;
                if (side) {
                    for (int y = FLOOR - 3; y <= top; y++) {
                        pRoom.setBlock(at.set(x, y, z), wallAt(x, y, z), 2);
                    }
                    continue;
                }
                pRoom.setBlock(at.set(x, top, z), skyAt(x, z), 2);
                int ground = groundAt(x, z);
                for (int y = FLOOR - 3; y < ground; y++) {
                    pRoom.setBlock(at.set(x, y, z), dirt, 2);
                }
                pRoom.setBlock(at.set(x, ground, z), onPath(x, z) ? Blocks.DIRT_PATH.defaultBlockState() : grass, 2);
                BlockState plant = plantAt(x, z);
                if (plant != null && !onPath(x, z)) {
                    pRoom.setBlock(at.set(x, ground + 1, z), plant, 2);
                }
            }
        }
        PlayroomHouse.build(pRoom);
        built.version = ROOM_VERSION;
        built.setDirty();
    }

    // ---------------------------------------------------------------- the yard: rolling ground, a path, plants
    // The house's ground along the back wall (HOUSE_DEPTH deep), the arrival spot and the edges by the walls are kept
    // flat; between them the ground rolls in low hills, up to HILLS blocks.
    public static final int HOUSE_DEPTH = 41;
    private static final double HILLS = 4.0;

    public static int groundAt(int pX, int pZ) {
        double hills = noise(pX / 20.0, pZ / 20.0, 31) * 0.55 + noise(pX / 9.0, pZ / 9.0, 33) * 0.3 + noise(pX / 4.0, pZ / 4.0, 34) * 0.15;
        double flat = 1.0;
        flat = Math.min(flat, smoothstep((pZ - (-HALF + HOUSE_DEPTH)) / 8.0));                            // the house's ground
        flat = Math.min(flat, smoothstep((Math.hypot(pX - ARRIVE.x, pZ - ARRIVE.z) - 5.0) / 6.0));       // where you arrive
        flat = Math.min(flat, smoothstep((HALF - Math.max(Math.abs(pX), Math.abs(pZ)) - 3.0) / 6.0));    // the edges
        return FLOOR + (int) Math.round(Math.max(0.0, hills - 0.3) / 0.45 * HILLS * flat);
    }

    private static double smoothstep(double pU) {
        double u = Math.max(0.0, Math.min(1.0, pU));
        return u * u * (3 - 2 * u);
    }

    // A worn dirt path winding from where you arrive to the house's front garden (PlayroomHouse takes it on from
    // pathEndZ to the porch).
    private static boolean onPath(int pX, int pZ) {
        if (pZ > ARRIVE.z || pZ < pathEndZ()) {
            return false;
        }
        return Math.abs(pX + 0.5 - pathMiddle(pZ)) < 1.6 + 0.4 * noise(pX * 0.5, pZ * 0.5, 35);
    }

    static int pathEndZ() {
        return -HALF + HOUSE_DEPTH - 2;
    }

    static double pathMiddle(int pZ) {
        return 4.0 * Math.sin(pZ / 11.0) + 2.0 * Math.sin(pZ / 5.0 + 1.0);
    }

    // Patches of grass and flowers here and there.
    private static BlockState plantAt(int pX, int pZ) {
        double patch = noise(pX / 6.0, pZ / 6.0, 37), r = hash(pX, pZ, 39);
        if (patch > 0.55 && r < 0.45) {
            return Blocks.SHORT_GRASS.defaultBlockState();
        }
        if (noise(pX / 8.0, pZ / 8.0, 41) > 0.7 && r < 0.18) {
            Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY};
            return flowers[(int) (hash(pX, pZ, 43) * flowers.length)].defaultBlockState();
        }
        return null;
    }

    // ---------------------------------------------------------------- the painted outdoors
    // The ceiling: Minecraft's sky, with its blocky clouds (whole 4x4 patches, as the game draws them).
    private static BlockState skyAt(int pX, int pZ) {
        int cx = Math.floorDiv(pX, 4), cz = Math.floorDiv(pZ, 4);
        boolean cloud = noise(cx * 0.35, cz * 0.35, 11) > 0.6;
        return (cloud ? ModBlocks.painted_cloud : ModBlocks.painted_sky).get().defaultBlockState();
    }

    // The walls, painted as the world outside: a band of grass along the bottom, trees against the sky (a front row
    // of all sizes, smaller ones behind, sky between them all), a few clouds high up, and the sun high on the back
    // wall, which lights every painted tree: each crown, taken as a ball standing out of its wall, is light where it
    // faces the sun and dark where it faces away. `u` runs round the room's four walls.
    public static final int SUN_X = 34, SUN_H = 54;                 // the sun, on the back (north) wall
    private static final double SUN_R = 4.5;

    private static BlockState wallAt(int pX, int pY, int pZ) {
        int edge = HALF + 1;
        int u;
        double[] along, inward;
        if (pZ == -edge) {
            u = pX + edge; along = new double[]{1, 0, 0}; inward = new double[]{0, 0, 1};
        } else if (pX == edge) {
            u = 2 * edge + pZ + edge; along = new double[]{0, 0, 1}; inward = new double[]{-1, 0, 0};
        } else if (pZ == edge) {
            u = 4 * edge + (edge - pX); along = new double[]{-1, 0, 0}; inward = new double[]{0, 0, -1};
        } else {
            u = 6 * edge + (edge - pZ); along = new double[]{0, 0, -1}; inward = new double[]{1, 0, 0};
        }
        int h = pY - FLOOR;                                        // height above the floor
        if (h <= 2) {
            return ModBlocks.painted_grass.get().defaultBlockState();
        }
        if (pZ == -edge && Math.hypot(pX - SUN_X, h - SUN_H) < SUN_R) {
            return ModBlocks.painted_sun.get().defaultBlockState();
        }
        // the light on a crown: its surface's normal there against the way to the sun
        double lx = SUN_X - pX, ly = FLOOR + SUN_H - pY, lz = -edge - pZ;
        double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        Lit lit = (du, dh, rx, ry) -> {
            double a = du / rx, b = dh / ry, c = Math.sqrt(Math.max(0.0, 1.0 - a * a - b * b));
            double nx = a * along[0] + c * inward[0], ny = b, nz = a * along[2] + c * inward[2];
            double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
            return (nx * lx + ny * ly + nz * lz) / (nl * ll) > 0.12;
        };
        BlockState front = tree(u, h, FRONT_GAP, 1, false, lit);
        if (front != null) {
            return front;
        }
        BlockState back = tree(u + FRONT_GAP / 2, h, BACK_GAP, 101, true, lit);
        if (back != null) {
            return back;
        }
        if (h > HEIGHT - 22 && noise(u * 0.18, h * 0.45, 23) > 0.66) {
            return ModBlocks.painted_cloud.get().defaultBlockState();
        }
        return ModBlocks.painted_sky.get().defaultBlockState();
    }

    private interface Lit {
        boolean at(double pDu, double pDh, double pRx, double pRy);
    }

    private static final int FRONT_GAP = 15, BACK_GAP = 9;

    // One of a row of painted trees, if (u, h) is on one: a trunk and a round, ragged crown, each tree its own
    // height and size (some left out, for gaps), light or dark by the sun (lit). The row behind is smaller.
    private static BlockState tree(int pU, int pH, int pGap, int pSalt, boolean pBehind, Lit pLit) {
        int slot = Math.floorDiv(pU, pGap);
        for (int k = slot - 1; k <= slot + 1; k++) {
            if (rand(k, pSalt) < (pBehind ? 0.15 : 0.1)) {
                continue;                                          // (a gap in the row)
            }
            double trunkAt = k * pGap + rand(k, pSalt + 1) * pGap * 0.6;
            double size = pBehind ? 0.6 + rand(k, pSalt + 2) * 0.5 : 0.7 + Math.pow(rand(k, pSalt + 2), 1.5) * 1.6;
            double rx = (3.5 + rand(k, pSalt + 3) * 3.0) * size + 1.5, ry = (3.5 + rand(k, pSalt + 4) * 3.5) * size + 1.0;
            double trunkTop = 3 + (4 + rand(k, pSalt + 5) * 10) * size;
            double cy = trunkTop + ry * 0.55;
            double du = pU - trunkAt, dh = pH - cy;
            double ragged = 0.82 + 0.36 * noise(pU * 0.45, pH * 0.45, pSalt + 13 + k);
            if ((du * du) / (rx * rx) + (dh * dh) / (ry * ry) < ragged) {
                boolean light = pLit.at(du, dh, rx * Math.sqrt(ragged), ry * Math.sqrt(ragged));
                return (light ? ModBlocks.painted_leaves : ModBlocks.painted_leaves_dark).get().defaultBlockState();
            }
            if (Math.abs(du) < (size > 1.1 ? 1.0 : 0.6) && pH < cy) {
                return ModBlocks.painted_bark.get().defaultBlockState();
            }
        }
        return null;
    }

    private static double rand(int pK, int pSalt) {
        long h = pK * 0x9E3779B97F4A7C15L + pSalt * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return (h >>> 11) / (double) (1L << 53);
    }

    private static double hash(int pX, int pY, int pSalt) {
        return rand(pX * 73856093 ^ pY * 19349663, pSalt);
    }

    // Smooth value noise, 0..1.
    private static double noise(double pX, double pY, int pSalt) {
        int ix = (int) Math.floor(pX), iy = (int) Math.floor(pY);
        double fx = pX - ix, fy = pY - iy;
        double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        double a = hash(ix, iy, pSalt), b = hash(ix + 1, iy, pSalt), c = hash(ix, iy + 1, pSalt), d = hash(ix + 1, iy + 1, pSalt);
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy;
    }
}
