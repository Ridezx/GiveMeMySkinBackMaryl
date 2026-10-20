package net.ridez.horror.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.custom.TacticalVestItem;
import net.ridez.horror.menu.ModMenus;
import net.ridez.horror.network.ModNetwork;
import org.lwjgl.glfw.GLFW;

// The "Open Tactical Vest" key (B by default): opens the worn vest's pockets (the server does it, see ModNetwork).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TacticalVestClient {
    public static final KeyMapping OPEN_VEST = new KeyMapping("key.horrormod.open_vest", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.horrormod");

    @Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Setup {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_VEST);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(ModMenus.tactical_vest.get(), TacticalVestScreen::new));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_VEST.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null && TacticalVestItem.isWorn(minecraft.player)) {
                ModNetwork.sendToServer(ModNetwork.VestAction.OPEN);
            }
        }
    }
}
