package net.ridez.horror.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

// Tar behaves like lava (flow, movement, sounds, fog) but never burns or damages anything. It isn't in the
// lava fluid tag on purpose: vanilla uses that tag to set entities on fire.
public class TarFluidType extends FluidType {
    // Placeholder look: vanilla's water textures (made to be tinted) tinted near-black. Swap in
    // horrormod:block/tar_still and tar_flowing once those textures exist (see TEXTURES.md).
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flowing");
    private static final int TINT = 0xFF1A1512;
    private static final Vector3f FOG_COLOR = new Vector3f(0.10F, 0.08F, 0.07F);

    public TarFluidType() {
        // Lava's settings from Forge's LAVA_TYPE, minus its light and heat.
        super(Properties.create()
                .descriptionId("block.horrormod.tar")
                .canSwim(false)
                .canDrown(false)
                .canExtinguish(false)
                .canConvertToSource(false)
                .supportsBoating(false)
                .canHydrate(false)
                .pathType(PathType.LAVA)
                .adjacentPathType(null)
                .motionScale(0.0023333333333333335)
                .fallDistanceModifier(0.5F)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .density(3000)
                .viscosity(6000));
    }

    // Vanilla's lava movement from LivingEntity.travel. Without this, Forge moves entities through custom
    // fluids like water.
    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        boolean falling = entity.getDeltaMovement().y <= 0.0;
        double startY = entity.getY();
        entity.moveRelative(0.02F, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        if (entity.getFluidTypeHeight(this) <= entity.getFluidJumpThreshold()) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
            entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, falling, entity.getDeltaMovement()));
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
        }
        if (gravity != 0.0) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -gravity / 4.0, 0.0));
        }
        Vec3 motion = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.isFree(motion.x, motion.y + 0.6F - entity.getY() + startY, motion.z)) {
            entity.setDeltaMovement(motion.x, 0.3F, motion.z);
        }
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return TINT;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return FOG_COLOR;
            }

            // Lava's fog distances: you can barely see while submerged.
            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(0.25F);
                RenderSystem.setShaderFogEnd(1.0F);
            }
        });
    }
}
