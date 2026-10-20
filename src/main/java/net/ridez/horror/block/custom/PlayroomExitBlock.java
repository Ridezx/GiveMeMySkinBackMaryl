package net.ridez.horror.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ridez.horror.dimension.ChildsPlayroom;

// The way out of the Child's Playroom: a dark, spreading rot of a portal (the one in the house's basement, and the
// hole the slide in the outside wall goes into). Walking or falling into it, a player is sent back to where they
// came into the playroom. It does nothing outside the playroom. Nothing stops you passing into it; it can't be
// broken.
public class PlayroomExitBlock extends Block {
    public PlayroomExitBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide() && pEntity instanceof ServerPlayer player && ChildsPlayroom.isIn(pLevel) && !player.isOnPortalCooldown()) {
            ChildsPlayroom.sendBack(player);
        }
    }
}
