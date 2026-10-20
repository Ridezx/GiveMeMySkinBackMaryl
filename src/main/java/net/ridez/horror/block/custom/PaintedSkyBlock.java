package net.ridez.horror.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// A painted sky (or cloud): looks solid, but daylight goes straight through it, as through glass. The Child's
// Playroom's ceiling is made of it, so the whole yard under it is lit evenly by the sky, with no shadows, while
// anything with a roof of its own (the dollhouse) stays dark inside.
public class PaintedSkyBlock extends Block {
    public PaintedSkyBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 0;
    }
}
