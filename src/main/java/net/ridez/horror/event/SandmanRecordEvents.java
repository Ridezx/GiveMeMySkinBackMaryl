package net.ridez.horror.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SandmanEntity;
import net.ridez.horror.entity.custom.SkinwalkerEntity;

// A Sandman's record stays with a player through death (and coming back from the End): punishment doesn't end.
// Killing a Sandman adds to it; killing a Skinwalker (doing the Sandmen's work) takes some off.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class SandmanRecordEvents {
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        int record = SandmanEntity.getRecord(event.getOriginal());
        if (record > 0) {
            event.getEntity().getPersistentData().putInt(SandmanEntity.RECORD, record);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getEntity() instanceof Player player) || player.isCreative()) {
            return;
        }
        if (event.getEntity() instanceof SandmanEntity) {
            SandmanEntity.addToRecord(player, SandmanEntity.RECORD_KILL_SANDMAN);
        } else if (event.getEntity() instanceof SkinwalkerEntity) {
            SandmanEntity.addToRecord(player, SandmanEntity.RECORD_KILL_SKINWALKER);
        }
    }
}
