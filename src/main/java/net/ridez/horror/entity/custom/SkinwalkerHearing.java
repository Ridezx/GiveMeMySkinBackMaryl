package net.ridez.horror.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;

// Skinwalkers hear blocks being placed or broken within their hearing range (it wakes them up, or puts them on
// alert), even when whoever did it is crouching. Footsteps are heard in SkinwalkerEntity.listen.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class SkinwalkerHearing {
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        soundAt(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        soundAt(event.getLevel(), event.getPos());
    }

    private static void soundAt(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        double range = SkinwalkerEntity.HEAR_RANGE;
        for (SkinwalkerEntity skinwalker : serverLevel.getEntitiesOfClass(SkinwalkerEntity.class, new AABB(pos).inflate(range))) {
            if (skinwalker.distanceToSqr(at) < range * range) {
                skinwalker.hearSound(at);
            }
        }
    }
}
