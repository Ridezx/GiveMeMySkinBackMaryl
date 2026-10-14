package net.ridez.horror.item.custom;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

// The sweet berry bush itself, dropped when a bush is broken with shears. Smelts into dust.
// Not a BlockItem: that would replace Sweet Berries as the bush's item (pick block, other mods' lookups).
// Instead it plants a young bush through the Sweet Berries item's own placement logic.
public class BerryBushItem extends Item {
    public BerryBushItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        return ((BlockItem) Items.SWEET_BERRIES).place(new BlockPlaceContext(pContext));
    }
}
