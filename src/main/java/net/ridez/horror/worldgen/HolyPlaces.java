package net.ridez.horror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.ridez.horror.HorrorMod;

import java.util.Map;
import java.util.Optional;

import it.unimi.dsi.fastutil.longs.LongSet;

// Holy places: the structures in the horrormod:holy_places tag (the Mecca Black Box and the like, once they're built;
// the tag is empty until then). Their grounds count, not just their buildings. Sandmen keep the peace around them;
// Skinwalkers won't go into them; Hollows haunt them.
public class HolyPlaces {
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "holy_places"));

    // Whether pos is in a holy place's grounds, or within `margin` blocks of them.
    public static boolean near(ServerLevel pLevel, BlockPos pPos, int pMargin) {
        Registry<Structure> registry = pLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> holy = registry.getTag(TAG);
        if (holy.isEmpty() || holy.get().size() == 0) {
            return false;
        }
        StructureManager structures = pLevel.structureManager();
        int reach = pMargin + 16;                       // (the chunks around, for grounds that reach toward it)
        for (int dx = -reach; dx <= reach; dx += 16) {
            for (int dz = -reach; dz <= reach; dz += 16) {
                BlockPos at = pPos.offset(dx, 0, dz);
                if (!pLevel.hasChunkAt(at)) {
                    continue;
                }
                for (Map.Entry<Structure, LongSet> entry : structures.getAllStructuresAt(at).entrySet()) {
                    if (!holy.get().contains(registry.wrapAsHolder(entry.getKey()))) {
                        continue;
                    }
                    boolean[] found = {false};
                    structures.fillStartsForStructure(entry.getKey(), entry.getValue(),
                            start -> found[0] |= start.getBoundingBox().inflatedBy(pMargin).isInside(pPos));
                    if (found[0]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean inside(ServerLevel pLevel, BlockPos pPos) {
        return near(pLevel, pPos, 0);
    }
}
