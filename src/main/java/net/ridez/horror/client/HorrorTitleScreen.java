package net.ridez.horror.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
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
import net.minecraft.util.Mth;
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
// at the top left, the menu as smears of blood along the bottom left corner (the one under the pointer flows), the
// mod's name in the bottom middle, the support links at the bottom right, and Odie standing toward the right, as
// tall as the screen, tipping his hat. The art is drawn by art/tools/title_screen_art.py (textures/gui/title/).
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HorrorTitleScreen extends Screen {
    static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/gui/title/name.png");
    static final int NAME_W = 1839, NAME_H = 125;
    private static final ResourceLocation ODIE = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/gui/title/odie.png");
    private static final int ODIE_W = 1040, ODIE_H = 1456;
    // Odie stands this far across the screen (his middle)
    private static final float ODIE_CENTRE = 0.76F;
    // The menu buttons' blood smears, one per button (all drawn by art/tools/title_screen_art.py): each texture is a
    // strip of frames, first the hover flowing in (frame 0 at rest), then a wet sheen gliding along it, looped.
    private static final ResourceLocation[] BLOOD = new ResourceLocation[5];
    static {
        for (int i = 0; i < BLOOD.length; i++) {
            BLOOD[i] = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/gui/title/blood_" + i + ".png");
        }
    }
    private static final int BLOOD_W = 450, BLOOD_H = 60, BLOOD_FLOW_FRAMES = 8, BLOOD_SHEEN_FRAMES = 12;
    // how long the blood takes to flow in and to settle back (ms), and each frame of the sheen
    private static final float FLOW_IN_MS = 400.0F, FLOW_OUT_MS = 300.0F;
    private static final int SHEEN_FRAME_MS = 100;
    // Support links. Empty until they exist: the buttons show but say the link is coming soon.
    private static final String DISCORD_URL = "";
    private static final String KOFI_URL = "";

    static final int BACKGROUND_TOP = 0xFF0E0C0C, BACKGROUND_BOTTOM = 0xFF070505;
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

    // ---------------------------------------------------------------- the same look at every GUI scale
    // The screen is laid out and drawn as if the GUI scale were Auto for this window, whatever it's set to, so the
    // logo, menu and text keep their size against the art. layoutScale is how much bigger that is than
    // the GUI scale in use; drawing is scaled up by it and mouse positions scaled down to match.
    private float layoutScale = 1.0F;

    private void fitToAutoScale() {
        Window window = this.minecraft.getWindow();
        int auto = window.calculateScale(0, this.minecraft.isEnforceUnicode());
        this.layoutScale = (float) (auto / window.getGuiScale());
        this.width = Mth.ceil(window.getWidth() / (float) auto);
        this.height = Mth.ceil(window.getHeight() / (float) auto);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().scale(this.layoutScale, this.layoutScale, 1.0F);
        this.renderScaled(pGuiGraphics, (int) (pMouseX / this.layoutScale), (int) (pMouseY / this.layoutScale), pPartialTick);
        pGuiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return super.mouseClicked(pMouseX / this.layoutScale, pMouseY / this.layoutScale, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return super.mouseReleased(pMouseX / this.layoutScale, pMouseY / this.layoutScale, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return super.mouseDragged(pMouseX / this.layoutScale, pMouseY / this.layoutScale, pButton,
                pDragX / this.layoutScale, pDragY / this.layoutScale);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        return super.mouseScrolled(pMouseX / this.layoutScale, pMouseY / this.layoutScale, pScrollX, pScrollY);
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        super.mouseMoved(pMouseX / this.layoutScale, pMouseY / this.layoutScale);
    }

    // ---------------------------------------------------------------- layout and drawing (in Auto-scale units)
    @Override
    protected void init() {
        this.fitToAutoScale();
        if (this.splash == null) {
            String text = ModSplashes.pick();
            this.splash = text != null ? new SplashRenderer(text) : null;
        }
        // the menu: bars stacked up from the bottom left corner, the first one on top
        Component multiplayerDisabled = this.minecraft.allowsMultiplayer() ? null : Component.translatable("title.multiplayer.disabled");
        MenuButton[] menu = {
                new MenuButton(0, Component.translatable("menu.singleplayer"), b -> this.minecraft.setScreen(new SelectWorldScreen(this))),
                new MenuButton(1, Component.translatable("menu.multiplayer"), b -> this.minecraft.setScreen(
                        this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this))),
                new MenuButton(2, Component.translatable("fml.menu.mods"), b -> this.minecraft.setScreen(new ModListScreen(this))),
                new MenuButton(3, Component.translatable("menu.options"), b -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))),
                new MenuButton(4, Component.translatable("menu.quit"), b -> this.minecraft.stop())};
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

    // The title screen's art, also behind the other menus (HorrorScreens): the near-black, and Odie tipping his hat,
    // as tall as the screen, toward the right (at the given brightness).
    static void drawBackdrop(GuiGraphics pGuiGraphics, int pWidth, int pHeight, float pOdieBrightness) {
        pGuiGraphics.fillGradient(0, 0, pWidth, pHeight, BACKGROUND_TOP, BACKGROUND_BOTTOM);
        RenderSystem.enableBlend();
        int w = pHeight * ODIE_W / ODIE_H;
        pGuiGraphics.setColor(pOdieBrightness, pOdieBrightness, pOdieBrightness, 1.0F);
        pGuiGraphics.blit(ODIE, (int) (pWidth * ODIE_CENTRE) - w / 2, 0, w, pHeight, 0, 0, ODIE_W, ODIE_H, ODIE_W, ODIE_H);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        drawBackdrop(pGuiGraphics, this.width, this.height, 1.0F);
        RenderSystem.enableBlend();
        // the menu corner sinks into darkness
        pGuiGraphics.fillGradient(0, this.height / 2, this.width / 2, this.height, 0x00000000, 0x60000000);
        // the mod's name, faded into the bottom middle, between the menu and the links, above the version lines
        int nameW = Math.min(this.width - 2 * (MENU_WIDTH + MENU_MARGIN), this.width / 2);
        int nameH = nameW * NAME_H / NAME_W;
        pGuiGraphics.blit(NAME, (this.width - nameW) / 2, this.height - NAME_BOTTOM - nameH, nameW, nameH, 0, 0, NAME_W, NAME_H, NAME_W, NAME_H);
        RenderSystem.disableBlend();
    }

    private void renderScaled(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
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

    // One of the menu buttons: a smear of dried blood from the left edge of the screen (each button its own).
    // Hovered (or selected with the keyboard) the blood flows: it brightens to a wet red, creeps a little further
    // out and its drips run down, then a sheen keeps gliding along it until the pointer leaves, when it settles
    // back; the text slides in a little. Greyed out (Multiplayer when it's disabled), the smear is faint.
    private static class MenuButton extends Button {
        private final ResourceLocation blood;
        private float flow;
        private long lastMillis;
        private float sheenMillis;

        MenuButton(int pIndex, Component pMessage, OnPress pOnPress) {
            super(0, 0, MENU_WIDTH, MENU_HEIGHT, pMessage, pOnPress, DEFAULT_NARRATION);
            this.blood = BLOOD[pIndex % BLOOD.length];
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            boolean lit = this.active && this.isHoveredOrFocused();
            long now = Util.getMillis();
            float dt = this.lastMillis == 0 ? 0.0F : Math.min(100.0F, now - this.lastMillis);
            this.lastMillis = now;
            this.flow = Mth.clamp(this.flow + (lit ? dt / FLOW_IN_MS : -dt / FLOW_OUT_MS), 0.0F, 1.0F);
            int frame;
            if (this.flow >= 1.0F) {
                this.sheenMillis += dt;
                frame = BLOOD_FLOW_FRAMES + (int) (this.sheenMillis / SHEEN_FRAME_MS) % BLOOD_SHEEN_FRAMES;
            } else {
                this.sheenMillis = 0.0F;
                frame = Math.round(this.flow * (BLOOD_FLOW_FRAMES - 1));
            }
            int x0 = this.getX(), y0 = this.getY(), w = this.getWidth(), h = this.getHeight();
            RenderSystem.enableBlend();
            if (!this.active) {
                pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.4F);
            }
            pGuiGraphics.blit(this.blood, x0, y0, w, h, 0, frame * BLOOD_H, BLOOD_W, BLOOD_H, BLOOD_W,
                    BLOOD_H * (BLOOD_FLOW_FRAMES + BLOOD_SHEEN_FRAMES));
            pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            Font font = Minecraft.getInstance().font;
            int text = !this.active ? 0x6A5A5A : lit ? 0xFFF0E6 : 0xD8C4BC;
            pGuiGraphics.drawString(font, this.getMessage(), x0 + 12 + Math.round(4 * this.flow), y0 + (h - 8) / 2, text | 0xFF000000);
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
