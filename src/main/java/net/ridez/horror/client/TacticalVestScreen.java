package net.ridez.horror.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ridez.horror.menu.TacticalVestMenu;

// The Tactical Vest's pockets: four slots over the inventory, drawn in the game's usual grey panel style.
public class TacticalVestScreen extends AbstractContainerScreen<TacticalVestMenu> {
    private static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, EDGE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;

    public TacticalVestScreen(TacticalVestMenu pMenu, Inventory pInventory, Component pTitle) {
        super(pMenu, pInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x0 = this.leftPos, y0 = this.topPos, x1 = x0 + this.imageWidth, y1 = y0 + this.imageHeight;
        pGuiGraphics.fill(x0 + 1, y0, x1 - 1, y1, EDGE);
        pGuiGraphics.fill(x0, y0 + 1, x1, y1 - 1, EDGE);
        pGuiGraphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, LIGHT);
        pGuiGraphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
        pGuiGraphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, PANEL);
        for (Slot slot : this.menu.slots) {
            int sx = x0 + slot.x - 1, sy = y0 + slot.y - 1;
            pGuiGraphics.fill(sx, sy, sx + 18, sy + 18, SLOT_DARK);
            pGuiGraphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, LIGHT);
            pGuiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
        }
    }
}
