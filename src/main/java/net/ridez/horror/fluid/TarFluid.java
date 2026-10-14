package net.ridez.horror.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

// Still Tar gives off an eerie haze: ash drifting over the open surface and the odd wisp of smoke.
// Client-side only (vanilla calls animateTick for blocks near the player, like lava's popping).
public class TarFluid extends ForgeFlowingFluid.Source {
    public TarFluid(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void animateTick(Level pLevel, BlockPos pPos, FluidState pState, RandomSource pRandom) {
        if (!pLevel.getBlockState(pPos.above()).isAir()) {
            return;
        }
        double x = pPos.getX() + pRandom.nextDouble();
        double y = pPos.getY() + 1.0;
        double z = pPos.getZ() + pRandom.nextDouble();
        if (pRandom.nextInt(4) == 0) {
            pLevel.addParticle(ParticleTypes.ASH, x, y + 0.2, z, 0.0, 0.0, 0.0);
        }
        if (pRandom.nextInt(12) == 0) {
            pLevel.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
