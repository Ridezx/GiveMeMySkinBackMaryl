package net.ridez.horror.dimension;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;

// /playroom enter and /playroom leave (operators): straight into the Child's Playroom and back out again, the same
// way the Angel's portal and the way out do it (remembering where you were), without fetching an Angel each time.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class PlayroomCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("playroom")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enter").executes(context -> enter(context.getSource())))
                .then(Commands.literal("leave").executes(context -> leave(context.getSource()))));
    }

    private static int enter(CommandSourceStack pSource) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = pSource.getPlayerOrException();
        if (ChildsPlayroom.isIn(player.level())) {
            pSource.sendFailure(Component.literal("You're already in the Child's Playroom."));
            return 0;
        }
        ChildsPlayroom.sendIn(player);
        return 1;
    }

    private static int leave(CommandSourceStack pSource) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = pSource.getPlayerOrException();
        if (!ChildsPlayroom.isIn(player.level())) {
            pSource.sendFailure(Component.literal("You're not in the Child's Playroom."));
            return 0;
        }
        ChildsPlayroom.sendBack(player);
        return 1;
    }
}
