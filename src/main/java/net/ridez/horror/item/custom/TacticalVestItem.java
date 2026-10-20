package net.ridez.horror.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

// A Sandman's bulletproof vest (sometimes dropped by one): diamond chestplate protection, half the damage from
// Skinwalkers and from Sandmen's shots while worn (see ModEvents), and four pockets of storage, opened with the
// "Open Tactical Vest" key (B) while it's worn (client/TacticalVestClient, menu/TacticalVestMenu). What's in the
// pockets is kept on the vest itself, so it goes wherever the vest goes.
public class TacticalVestItem extends ArmorItem {
    public static final int POCKETS = 4;

    public TacticalVestItem(Holder<ArmorMaterial> pMaterial, Properties pProperties) {
        super(pMaterial, Type.CHESTPLATE, pProperties);
    }

    public static boolean isWorn(LivingEntity pEntity) {
        return pEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof TacticalVestItem;
    }

    // What's in the vest's pockets (empty if it isn't a vest).
    public static List<ItemStack> pockets(ItemStack pVest) {
        if (!(pVest.getItem() instanceof TacticalVestItem)) {
            return List.of();
        }
        NonNullList<ItemStack> items = NonNullList.withSize(POCKETS, ItemStack.EMPTY);
        pVest.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    // The pockets as a container to open; changes are written straight back onto the vest.
    public static SimpleContainer open(ItemStack pVest) {
        SimpleContainer container = new SimpleContainer(POCKETS);
        List<ItemStack> items = pockets(pVest);
        for (int i = 0; i < items.size(); i++) {
            container.setItem(i, items.get(i));
        }
        container.addListener(c -> pVest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(((SimpleContainer) c).getItems())));
        return container;
    }

    // No vests, bundles or shulker boxes in the pockets (nothing that holds items itself).
    public static boolean fitsInPocket(ItemStack pStack) {
        return !(pStack.getItem() instanceof TacticalVestItem) && !(pStack.getItem() instanceof BundleItem)
                && !pStack.has(DataComponents.CONTAINER) && pStack.getItem().canFitInsideContainerItems();
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltip, TooltipFlag pFlag) {
        if (Screen.hasShiftDown()) {                         // (the details only while Shift is held)
            pTooltip.add(Component.translatable("tooltip.horrormod.tactical_vest", Component.keybind("key.horrormod.open_vest")));
            long used = pockets(pStack).stream().filter(s -> !s.isEmpty()).count();
            pTooltip.add(Component.translatable("tooltip.horrormod.tactical_vest.pockets", used, POCKETS));
        } else {
            pTooltip.add(Component.translatable("tooltip.horrormod.hold_shift"));
        }
        super.appendHoverText(pStack, pContext, pTooltip, pFlag);
    }
}
