package net.ridez.horror.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;

// The title screen's look carried through the rest of the game's screens:
// - Every menu outside a world (Singleplayer, the multiplayer warning and server list, Options, Mods, and the rest,
//   other mods' too) has the title screen's art behind it instead of the vanilla panorama: the near-black and Odie,
//   dimmed a little so the menu reads over him. (Menus opened inside a world keep the blurred world behind them.)
// - The screens while a world opens and closes (reading the world, preparing it, loading terrain, saving) and while
//   going through a Nether or End portal: the same near-black, the mod's name in dried-blood red in the middle, the
//   message under it in pale text, and while the world is being prepared a thin line of blood filling up under that
//   instead of the vanilla map of chunks. Only the vanilla screens themselves are drawn over (other mods' versions of
//   them are left alone).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HorrorScreens {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TEXT = 0xFFD8C4BC;
    private static final int LINE_TRACK = 0xFF1C1010, LINE_BLOOD = 0xFF6E1014, LINE_EDGE = 0xFFA0262A;
    private static final int NAME_MAX_WIDTH = 320, LINE_WIDTH = 160;
    private static final float MENU_ODIE_BRIGHTNESS = 0.6F;
    // (the screen keeps it private; if it can't be read, that screen stays vanilla)
    @Nullable
    private static final Field PROGRESS = field(LevelLoadingScreen.class, "progressListener");

    @Nullable
    private static Field field(Class<?> pClass, String pName) {
        try {
            return ObfuscationReflectionHelper.findField(pClass, pName);
        } catch (RuntimeException e) {
            LOGGER.warn("Can't theme {}: {}", pClass.getSimpleName(), e.toString());
            return null;
        }
    }

    // The message screens and the world-preparing screen: drawn whole, in place of the vanilla drawing.
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        try {
            if (screen.getClass() == GenericMessageScreen.class) {
                draw(event.getGuiGraphics(), screen, screen.getTitle(), -1);
            } else if (screen.getClass() == LevelLoadingScreen.class && PROGRESS != null) {
                int progress = Mth.clamp(((StoringChunkProgressListener) PROGRESS.get(screen)).getProgress(), 0, 100);
                draw(event.getGuiGraphics(), screen, Component.translatable("loading.progress", progress), progress);
            } else if (screen.getClass() == ReceivingLevelScreen.class) {
                draw(event.getGuiGraphics(), screen, Component.translatable("multiplayer.downloadingTerrain"), -1);
            } else {
                return;
            }
        } catch (IllegalAccessException e) {
            return;
        }
        event.setCanceled(true);
    }

    // Menus outside a world: the title screen's art in place of the panorama (drawn over it, before the menu itself).
    // The progress screen draws its own text (and closes itself when it's done), so only its background is replaced.
    @SubscribeEvent
    public static void onBackground(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        if (screen.getClass() == ProgressScreen.class) {
            backdrop(event.getGuiGraphics(), screen.width, screen.height);
        } else if (screen.getMinecraft().level == null) {
            HorrorTitleScreen.drawBackdrop(event.getGuiGraphics(), screen.width, screen.height, MENU_ODIE_BRIGHTNESS);
        }
    }

    private static void draw(GuiGraphics pGuiGraphics, Screen pScreen, Component pMessage, int pProgress) {
        int w = pScreen.width, h = pScreen.height;
        backdrop(pGuiGraphics, w, h);
        int y = h / 2 + 4;
        pGuiGraphics.drawCenteredString(pScreen.getMinecraft().font, pMessage, w / 2, y, TEXT);
        if (pProgress >= 0) {
            int x0 = (w - LINE_WIDTH) / 2, x1 = x0 + Math.round(LINE_WIDTH * pProgress / 100.0F), ly = y + 16;
            pGuiGraphics.fill(x0, ly, x0 + LINE_WIDTH, ly + 2, LINE_TRACK);
            if (x1 > x0) {
                pGuiGraphics.fill(x0, ly, x1, ly + 2, LINE_BLOOD);
                pGuiGraphics.fill(x1 - 1, ly, x1, ly + 3, LINE_EDGE);          // the wet front of it, a drop hanging
            }
        }
    }

    // The near-black of the title screen, darker at the edges, and the mod's name above the middle.
    private static void backdrop(GuiGraphics pGuiGraphics, int pWidth, int pHeight) {
        pGuiGraphics.fillGradient(0, 0, pWidth, pHeight, HorrorTitleScreen.BACKGROUND_TOP, HorrorTitleScreen.BACKGROUND_BOTTOM);
        pGuiGraphics.fillGradient(0, 0, pWidth, pHeight / 4, 0x60000000, 0x00000000);
        pGuiGraphics.fillGradient(0, pHeight * 3 / 4, pWidth, pHeight, 0x00000000, 0x60000000);
        int nameW = Math.min(pWidth - 40, NAME_MAX_WIDTH);
        int nameH = nameW * HorrorTitleScreen.NAME_H / HorrorTitleScreen.NAME_W;
        RenderSystem.enableBlend();
        pGuiGraphics.blit(HorrorTitleScreen.NAME, (pWidth - nameW) / 2, pHeight / 2 - 14 - nameH, nameW, nameH, 0, 0,
                HorrorTitleScreen.NAME_W, HorrorTitleScreen.NAME_H, HorrorTitleScreen.NAME_W, HorrorTitleScreen.NAME_H);
        RenderSystem.disableBlend();
    }
}
