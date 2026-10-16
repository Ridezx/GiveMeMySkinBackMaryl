package net.ridez.horror.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Vanilla swaps in holiday splashes (Christmas, New Year, Halloween) and a rare "<name> IS YOU".
// Picking the title screen's splash before vanilla does means only lines from splashes.txt ever show.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModSplashes {
    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.Init.Pre event) {
        if (!(event.getScreen() instanceof TitleScreen titleScreen)) {
            return;
        }
        SplashRenderer current = ObfuscationReflectionHelper.getPrivateValue(TitleScreen.class, titleScreen, "splash");
        if (current != null) {
            return;
        }
        String splash = pick();
        if (splash != null) {
            ObfuscationReflectionHelper.setPrivateValue(TitleScreen.class, titleScreen, new SplashRenderer(splash), "splash");
        }
    }

    // A random line from splashes.txt (also used by HorrorTitleScreen), or null if there are none.
    @Nullable
    public static String pick() {
        List<String> splashes = ObfuscationReflectionHelper.getPrivateValue(
                SplashManager.class, Minecraft.getInstance().getSplashManager(), "splashes");
        if (splashes == null || splashes.isEmpty()) {
            return null;
        }
        return splashes.get(RANDOM.nextInt(splashes.size()));
    }
}
