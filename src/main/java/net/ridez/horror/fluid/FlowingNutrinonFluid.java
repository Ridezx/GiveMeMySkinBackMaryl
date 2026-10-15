package net.ridez.horror.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.joml.Vector3f;

// Flowing Nutrinon sheds dark red droplets; still Nutrinon (the source blocks) shows no particles.
// Client-side only, like lava's popping.
public class FlowingNutrinonFluid extends ForgeFlowingFluid.Flowing {
    private static final DustParticleOptions DROPLET = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.05F), 0.8F);

    public FlowingNutrinonFluid(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void animateTick(Level pLevel, BlockPos pPos, FluidState pState, RandomSource pRandom) {
        if (pRandom.nextInt(3) == 0) {
            double x = pPos.getX() + pRandom.nextDouble();
            double y = pPos.getY() + pState.getOwnHeight() + 0.05;
            double z = pPos.getZ() + pRandom.nextDouble();
            pLevel.addParticle(DROPLET, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
