package net.ridez.horror.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

// The sweet berry bush itself, dropped when a bush is broken with shears. Smelts into dust.
// Not a BlockItem: that would replace Sweet Berries as the bush's item (pick block, other mods' lookups).
// Instead it plants through the Sweet Berries item's own placement logic, then grows the bush to full size:
// it's the whole plant, not a seedling.
public class BerryBushItem extends Item {
    public BerryBushItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockPlaceContext place = new BlockPlaceContext(pContext);
        InteractionResult result = ((BlockItem) Items.SWEET_BERRIES).place(place);
        if (result.consumesAction()) {
            Level level = pContext.getLevel();
            BlockPos pos = place.getClickedPos();
            BlockState placed = level.getBlockState(pos);
            if (placed.is(Blocks.SWEET_BERRY_BUSH)) {
                level.setBlock(pos, placed.setValue(SweetBerryBushBlock.AGE, SweetBerryBushBlock.MAX_AGE), 3);
            }
        }
        return result;
    }
}
