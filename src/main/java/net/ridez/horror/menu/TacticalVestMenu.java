package net.ridez.horror.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ridez.horror.item.custom.TacticalVestItem;

// The Tactical Vest's four pockets above the player's inventory. On the server the pockets are the worn vest's own
// storage (TacticalVestItem.open); it closes if the vest is taken off.
public class TacticalVestMenu extends AbstractContainerMenu {
    public static final int POCKETS_X = 53, POCKETS_Y = 20, INVENTORY_Y = 51;
    private final Container pockets;
    private final ItemStack vest;

    // the player's game: an empty stand-in the server fills
    public TacticalVestMenu(int pId, Inventory pInventory) {
        this(pId, pInventory, new SimpleContainer(TacticalVestItem.POCKETS), ItemStack.EMPTY);
    }

    public TacticalVestMenu(int pId, Inventory pInventory, Container pPockets, ItemStack pVest) {
        super(ModMenus.tactical_vest.get(), pId);
        this.pockets = pPockets;
        this.vest = pVest;
        for (int i = 0; i < TacticalVestItem.POCKETS; i++) {
            this.addSlot(new Slot(pPockets, i, POCKETS_X + i * 18, POCKETS_Y) {
                @Override
                public boolean mayPlace(ItemStack pStack) {
                    return TacticalVestItem.fitsInPocket(pStack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(pInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(pInventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.vest.isEmpty() || pPlayer.isAlive() && pPlayer.getItemBySlot(EquipmentSlot.CHEST) == this.vest;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot slot = this.slots.get(pIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int pockets = TacticalVestItem.POCKETS;
        if (pIndex < pockets) {
            if (!this.moveItemStackTo(stack, pockets, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!TacticalVestItem.fitsInPocket(stack) || !this.moveItemStackTo(stack, 0, pockets, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
