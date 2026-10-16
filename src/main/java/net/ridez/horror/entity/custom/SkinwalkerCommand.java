package net.ridez.horror.entity.custom;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.ModEntities;

// For testing: /skinwalker sleep (operators only) makes every loaded Skinwalker sleepy: each keeps doing
// whatever it was doing for 5 seconds, then lies down and goes to sleep.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class SkinwalkerCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skinwalker")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("sleep").executes(context -> sleep(context.getSource()))));
    }

    private static int sleep(CommandSourceStack pSource) {
        int count = 0;
        for (ServerLevel level : pSource.getServer().getAllLevels()) {
            for (SkinwalkerEntity skinwalker : level.getEntities(ModEntities.skinwalker.get(), skinwalker -> true)) {
                skinwalker.makeSleepy();
                count++;
            }
        }
        int found = count;
        pSource.sendSuccess(() -> Component.literal(found + " Skinwalker(s) will fall asleep in 5 seconds."), true);
        return found;
    }
}
