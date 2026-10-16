package net.ridez.horror.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.internal.BrandingControl;
import net.ridez.horror.HorrorMod;
import org.jetbrains.annotations.Nullable;

// The mod's title screen, shown instead of the vanilla one: a dark screen with the Minecraft logo and splash text
// at the top left, the menu as dark red bars along the bottom left corner, the mod's name faded into the bottom
// middle, the support links at the bottom right, and the Skinwalker leaning out from behind the right edge of the
// screen, watching. The art is drawn by art/tools/title_screen_art.py (textures/gui/title/).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HorrorTitleScreen extends Screen {
    private static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/gui/title/name.png");
    private static final int NAME_W = 1839, NAME_H = 125;
    private static final ResourceLocation SKINWALKER = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/gui/title/skinwalker.png");
    private static final int SKINWALKER_W = 1040, SKINWALKER_H = 1456;
    // Support links. Empty until they exist: the buttons show but say the link is coming soon.
    private static final String DISCORD_URL = "";
    private static final String KOFI_URL = "";

    private static final int BACKGROUND_TOP = 0xFF0E0C0C, BACKGROUND_BOTTOM = 0xFF070505;
    private static final int MENU_WIDTH = 150, MENU_HEIGHT = 20, MENU_GAP = 5, MENU_MARGIN = 12;
    private static final int LINK_WIDTH = 52, LINK_HEIGHT = 16;
    private static final Component COPYRIGHT = Component.translatable("title.credits");
    // the faded name sits this far up from the bottom, clear of the version lines and credits link
    private static final int NAME_BOTTOM = 62;

    private final LogoRenderer logo = new LogoRenderer(false);
    @Nullable
    private SplashRenderer splash;

    public HorrorTitleScreen() {
        super(Component.translatable("menu.horrormod.title"));
    }

    // Shows this screen wherever the game opens its plain title screen (not other mods' versions of it).
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (isVanillaTitle(event.getNewScreen())) {
            event.setNewScreen(new HorrorTitleScreen());
        }
    }

    // The very first title screen opens just before Forge starts passing events on, so the event above misses it:
    // it's swapped out on the first tick instead.
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isVanillaTitle(minecraft.screen)) {
            minecraft.setScreen(new HorrorTitleScreen());
        }
    }

    private static boolean isVanillaTitle(@Nullable Screen pScreen) {
        return pScreen != null && pScreen.getClass() == TitleScreen.class;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (this.splash == null) {
            String text = ModSplashes.pick();
            this.splash = text != null ? new SplashRenderer(text) : null;
        }
        // the menu: bars stacked up from the bottom left corner, the first one on top
        Component multiplayerDisabled = this.minecraft.allowsMultiplayer() ? null : Component.translatable("title.multiplayer.disabled");
        MenuButton[] menu = {
                new MenuButton(Component.translatable("menu.singleplayer"), b -> this.minecraft.setScreen(new SelectWorldScreen(this))),
                new MenuButton(Component.translatable("menu.multiplayer"), b -> this.minecraft.setScreen(
                        this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this))),
                new MenuButton(Component.translatable("fml.menu.mods"), b -> this.minecraft.setScreen(new ModListScreen(this))),
                new MenuButton(Component.translatable("menu.options"), b -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))),
                new MenuButton(Component.translatable("menu.quit"), b -> this.minecraft.stop())};
        int y = this.height - MENU_MARGIN - menu.length * (MENU_HEIGHT + MENU_GAP) + MENU_GAP;
        for (MenuButton button : menu) {
            button.setPosition(0, y);
            this.addRenderableWidget(button);
            y += MENU_HEIGHT + MENU_GAP;
        }
        if (multiplayerDisabled != null) {
            menu[1].active = false;
            menu[1].setTooltip(Tooltip.create(multiplayerDisabled));
        }
        // support links, bottom right
        int linkY = this.height - MENU_MARGIN - LINK_HEIGHT;
        this.addRenderableWidget(new LinkButton(this.width - MENU_MARGIN - LINK_WIDTH * 2 - 6, linkY,
                Component.translatable("menu.horrormod.discord"), DISCORD_URL));
        this.addRenderableWidget(new LinkButton(this.width - MENU_MARGIN - LINK_WIDTH, linkY,
                Component.translatable("menu.horrormod.kofi"), KOFI_URL));
        // Mojang's credits link, as on the vanilla title screen
        int copyrightWidth = this.font.width(COPYRIGHT);
        this.addRenderableWidget(new PlainTextButton((this.width - copyrightWidth) / 2, this.height - 10, copyrightWidth, 10, COPYRIGHT,
                b -> this.minecraft.setScreen(new CreditsAndAttributionScreen(this)), this.font));
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.fillGradient(0, 0, this.width, this.height, BACKGROUND_TOP, BACKGROUND_BOTTOM);
        RenderSystem.enableBlend();
        // the Skinwalker, as tall as the screen, its right side on the screen's right edge
        int h = this.height;
        int w = h * SKINWALKER_W / SKINWALKER_H;
        pGuiGraphics.blit(SKINWALKER, this.width - w, 0, w, h, 0, 0, SKINWALKER_W, SKINWALKER_H, SKINWALKER_W, SKINWALKER_H);
        // the menu corner sinks into darkness
        pGuiGraphics.fillGradient(0, this.height / 2, this.width / 2, this.height, 0x00000000, 0x60000000);
        // the mod's name, faded into the bottom middle, between the menu and the links, above the version lines
        int nameW = Math.min(this.width - 2 * (MENU_WIDTH + MENU_MARGIN), this.width / 2);
        int nameH = nameW * NAME_H / NAME_W;
        pGuiGraphics.blit(NAME, (this.width - nameW) / 2, this.height - NAME_BOTTOM - nameH, nameW, nameH, 0, 0, NAME_W, NAME_H, NAME_W, NAME_H);
        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        // the Minecraft logo with its splash text, at the top left (both are drawn centred on half the width given)
        int logoCentre = LogoRenderer.LOGO_WIDTH / 2 + Math.max(12, this.width / 20);
        this.logo.renderLogo(pGuiGraphics, logoCentre * 2, 1.0F);
        if (this.splash != null && !this.minecraft.options.hideSplashTexts().get()) {
            this.splash.render(pGuiGraphics, logoCentre * 2, this.font, 0xFF000000);
        }
        // version and mod loader lines, small, just above the credits link
        BrandingControl.forEachLine(true, true, (line, text) -> pGuiGraphics.drawCenteredString(this.font, text,
                this.width / 2, this.height - 22 - line * (this.font.lineHeight + 1), 0x8A7A7A));
    }

    // One of the menu bars: dark red, flush against the left edge and fading out toward its right end. Hovered
    // (or selected with the keyboard) it lights up and the text slides in a little.
    private static class MenuButton extends Button {
        MenuButton(Component pMessage, OnPress pOnPress) {
            super(0, 0, MENU_WIDTH, MENU_HEIGHT, pMessage, pOnPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            boolean lit = this.active && this.isHoveredOrFocused();
            int rgb = !this.active ? 0x2A1414 : lit ? 0x8E1018 : 0x5C0A0E;
            int x0 = this.getX(), y0 = this.getY(), w = this.getWidth(), h = this.getHeight();
            // a horizontal fade: solid for the first 60%, then thinning out to nothing
            for (int x = 0; x < w; x += 2) {
                float u = (float) x / w;
                int alpha = (int) (235 * (u < 0.6F ? 1.0F : 1.0F - (u - 0.6F) / 0.4F));
                pGuiGraphics.fill(x0 + x, y0, x0 + Math.min(w, x + 2), y0 + h, alpha << 24 | rgb);
            }
            if (lit) {
                pGuiGraphics.fill(x0, y0, x0 + 2, y0 + h, 0xFFC0202A);       // a bright sliver on the screen's edge
            }
            Font font = Minecraft.getInstance().font;
            int text = !this.active ? 0x6A5A5A : lit ? 0xFFF0E6 : 0xD8C4BC;
            pGuiGraphics.drawString(font, this.getMessage(), x0 + (lit ? 16 : 12), y0 + (h - 8) / 2, text | 0xFF000000);
        }
    }

    // A small pale support-link box. Without a URL yet it stays greyed out and says the link is coming soon.
    private class LinkButton extends Button {
        LinkButton(int pX, int pY, Component pMessage, String pUrl) {
            super(pX, pY, LINK_WIDTH, LINK_HEIGHT, pMessage, b -> ConfirmLinkScreen.confirmLinkNow(HorrorTitleScreen.this, pUrl), DEFAULT_NARRATION);
            if (pUrl.isEmpty()) {
                this.active = false;
                this.setTooltip(Tooltip.create(Component.translatable("menu.horrormod.link_soon")));
            }
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            int x0 = this.getX(), y0 = this.getY();
            int fill = !this.active ? 0x99D8C8C8 : this.isHoveredOrFocused() ? 0xFFFFF4F4 : 0xE6EEDADA;
            pGuiGraphics.fill(x0, y0, x0 + this.getWidth(), y0 + this.getHeight(), fill);
            pGuiGraphics.drawCenteredString(HorrorTitleScreen.this.font, this.getMessage(), x0 + this.getWidth() / 2,
                    y0 + (this.getHeight() - 8) / 2, 0xFF2A0A0C);
        }
    }
}
