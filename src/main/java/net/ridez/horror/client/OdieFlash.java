package net.ridez.horror.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// When a player gets too close to Odie he's gone, and their screen goes black for a split second with "Careful." in
// plain white in the middle (only for the player he was watching). Drawn over the HUD by mixin/GuiMixin: Forge 1.21
// has no event for drawing over the HUD.
public class OdieFlash {
    private static final long FLASH_MILLIS = 500;
    private static final Component TEXT = Component.translatable("message.horrormod.odie_careful");
    private static long flashUntil;

    public static void onOdieVanished(@Nullable UUID pWatched) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(pWatched)) {
            flashUntil = Util.getMillis() + FLASH_MILLIS;
        }
    }

    public static void render(GuiGraphics graphics) {
        if (Util.getMillis() >= flashUntil) {
            return;
        }
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);              // over everything else on the screen
        graphics.fill(0, 0, width, height, 0xFF000000);
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, TEXT, (width - minecraft.font.width(TEXT)) / 2, (height - minecraft.font.lineHeight) / 2,
                0xFFFFFFFF, false);
        graphics.pose().popPose();
    }
}
