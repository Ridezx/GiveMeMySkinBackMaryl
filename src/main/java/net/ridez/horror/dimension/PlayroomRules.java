package net.ridez.horror.dimension;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;

// In the Child's Playroom, players in survival (or adventure) can't break anything: they can't even start to.
// Creative players can, to build it. And nobody falls to their death in the void under the room (slipping through
// the way out between ticks, say): dropping below VOID_Y sends them back where they came from instead.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class PlayroomRules {
    private static final int VOID_Y = ChildsPlayroom.FLOOR - 24;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof net.minecraft.server.level.ServerLevel level) || !ChildsPlayroom.isIn(level) || level.getGameTime() % 4 != 0) {
            return;
        }
        for (net.minecraft.server.level.ServerPlayer player : java.util.List.copyOf(level.players())) {
            if (player.getY() < VOID_Y) {
                player.fallDistance = 0.0F;
                ChildsPlayroom.sendBack(player);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (ChildsPlayroom.isIn(event.getEntity().level()) && !event.getEntity().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && ChildsPlayroom.isIn(level) && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }
}
