package net.ridez.horror.dimension;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.block.ModBlocks;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Optional;

// The dollhouse in the Child's Playroom: the user's build, saved with Litematica (data/horrormod/playroom/
// dollhouse.litematic, read here as it is: re-export it from Litematica and drop the file in to change the house).
// It's placed with its bottom, back, west corner at CORNER, against the room's back wall, facing the yard. Its
// blocks and their contents (chests, beds and so on) go in as they were saved; its entities too, except bats. The
// one thing a Litematica world can't hold is this mod's own way out, so it's put back here: the Playroom Exit in the
// basement's far wall, with solid wall behind it (nothing to slip through into the void below the room).
final class PlayroomHouse {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "playroom/dollhouse.litematic");
    static final BlockPos CORNER = new BlockPos(-43, ChildsPlayroom.FLOOR - 9, -48);
    // the way out, in the house's own coordinates (from CORNER): the basement's far wall
    private static final int EXIT_X0 = 62, EXIT_X1 = 66, EXIT_Y0 = 1, EXIT_Y1 = 5, EXIT_Z = 0;
    // the basement's span along that wall (backed with solid stone, outside the house)
    private static final int BASEMENT_X0 = 56, BASEMENT_X1 = 73, BASEMENT_Y1 = 9;

    private PlayroomHouse() {
    }

    static void build(ServerLevel pLevel) {
        Optional<Resource> resource = pLevel.getServer().getResourceManager().getResource(FILE);
        if (resource.isEmpty()) {
            LOGGER.error("The dollhouse ({}) is missing: the playroom is built without it", FILE);
            return;
        }
        try (InputStream in = resource.get().open()) {
            place(pLevel, NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
        } catch (Exception e) {
            LOGGER.error("Couldn't place the dollhouse from {}", FILE, e);
            return;
        }
        // the way out, and a solid back behind it and the whole basement wall
        for (int x = BASEMENT_X0; x <= BASEMENT_X1; x++) {
            for (int y = 0; y <= BASEMENT_Y1; y++) {
                pLevel.setBlock(CORNER.offset(x, y, EXIT_Z - 1), Blocks.COBBLESTONE.defaultBlockState(), 2);
            }
        }
        for (int x = EXIT_X0; x <= EXIT_X1; x++) {
            for (int y = EXIT_Y0; y <= EXIT_Y1; y++) {
                pLevel.setBlock(CORNER.offset(x, y, EXIT_Z), ModBlocks.playroom_exit.get().defaultBlockState(), 2);
            }
        }
    }

    // Litematica's format: regions, each with a palette of block states and the blocks packed into longs (each
    // index taking just enough bits for the palette, running on across the longs' edges), x fastest, then z, then y,
    // from the region's lowest corner; block entities by their place in the region; entities by theirs from the
    // region's origin.
    private static void place(ServerLevel pLevel, CompoundTag pRoot) {
        HolderGetter<Block> blocks = pLevel.holderLookup(Registries.BLOCK);
        CompoundTag regions = pRoot.getCompound("Regions");
        for (String name : regions.getAllKeys()) {
            CompoundTag region = regions.getCompound(name);
            CompoundTag pos = region.getCompound("Position"), size = region.getCompound("Size");
            int px = pos.getInt("x"), py = pos.getInt("y"), pz = pos.getInt("z");
            int sx = size.getInt("x"), sy = size.getInt("y"), sz = size.getInt("z");
            BlockPos min = CORNER.offset(Math.min(px, px + sx + 1), Math.min(py, py + sy + 1), Math.min(pz, pz + sz + 1));
            BlockPos origin = CORNER.offset(px, py, pz);
            int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);
            ListTag paletteTag = region.getList("BlockStatePalette", Tag.TAG_COMPOUND);
            BlockState[] palette = new BlockState[paletteTag.size()];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = NbtUtils.readBlockState(blocks, paletteTag.getCompound(i));
            }
            long[] packed = region.getLongArray("BlockStates");
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.length - 1));
            long mask = (1L << bits) - 1;
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int y = 0; y < ay; y++) {
                for (int z = 0; z < az; z++) {
                    for (int x = 0; x < ax; x++) {
                        long bit = (long) ((y * az + z) * ax + x) * bits;
                        int li = (int) (bit >> 6), off = (int) (bit & 63);
                        long v = packed[li] >>> off;
                        if (off + bits > 64) {
                            v |= packed[li + 1] << (64 - off);
                        }
                        pLevel.setBlock(at.setWithOffset(min, x, y, z), palette[(int) (v & mask)], 2 | 16);
                    }
                }
            }
            for (Tag t : region.getList("TileEntities", Tag.TAG_COMPOUND)) {
                CompoundTag tag = ((CompoundTag) t).copy();
                BlockPos where = min.offset(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                BlockEntity entity = pLevel.getBlockEntity(where);
                if (entity != null) {
                    tag.putInt("x", where.getX());
                    tag.putInt("y", where.getY());
                    tag.putInt("z", where.getZ());
                    entity.loadWithComponents(tag, pLevel.registryAccess());
                    entity.setChanged();
                }
            }
            for (Tag t : region.getList("Entities", Tag.TAG_COMPOUND)) {
                CompoundTag tag = ((CompoundTag) t).copy();
                if ("minecraft:bat".equals(tag.getString("id"))) {
                    continue;                                       // (bats came in with the save: none here)
                }
                ListTag p = tag.getList("Pos", Tag.TAG_DOUBLE);
                double ex = origin.getX() + p.getDouble(0), ey = origin.getY() + p.getDouble(1), ez = origin.getZ() + p.getDouble(2);
                tag.remove("UUID");
                Entity entity = EntityType.loadEntityRecursive(tag, pLevel, e -> {
                    e.moveTo(ex, ey, ez, e.getYRot(), e.getXRot());
                    return e;
                });
                if (entity != null) {
                    pLevel.addFreshEntity(entity);
                }
            }
        }
    }
}
