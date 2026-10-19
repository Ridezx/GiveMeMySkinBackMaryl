package net.ridez.horror.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.ridez.horror.HorrorMod;
import org.joml.Vector3f;

import java.util.function.Consumer;

// Nutrinon: the blood that fills the Aspiraton veins. Flows and swims like water (you can drown in it),
// but it's thicker: swimming is slower than in water, though not as slow as lava. Two sources never make
// a new one, so it can't be farmed endlessly.
public class NutrinonFluidType extends FluidType {
    // Its own animated textures (art/tools/fluid_textures.py), already bright red, so they're drawn untinted.
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "block/nutrinon_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "block/nutrinon_flowing");
    private static final int TINT = 0xFFFFFFFF;
    private static final Vector3f FOG_COLOR = new Vector3f(0.45F, 0.02F, 0.04F);
    // How much speed is kept each tick while swimming: water keeps 0.8, lava 0.5.
    private static final float DRAG = 0.65F;
    private static final float SPRINT_DRAG = 0.75F;

    public NutrinonFluidType() {
        super(Properties.create()
                .descriptionId("block.horrormod.nutrinon")
                .canSwim(true)
                .canDrown(true)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(false)
                .canHydrate(false)
                .fallDistanceModifier(0.0F)
                .motionScale(0.014)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .density(1100)
                .viscosity(2000));
    }

    // Vanilla's water swimming from LivingEntity.travel, with heavier drag.
    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        boolean falling = entity.getDeltaMovement().y <= 0.0;
        double startY = entity.getY();
        float drag = entity.isSprinting() ? SPRINT_DRAG : DRAG;
        float acceleration = 0.02F;
        float efficiency = (float) entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!entity.onGround()) {
            efficiency *= 0.5F;
        }
        if (efficiency > 0.0F) {
            drag += (0.54600006F - drag) * efficiency;
            acceleration += (entity.getSpeed() - acceleration) * efficiency;
        }
        if (entity.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            drag = 0.96F;
        }
        acceleration *= (float) entity.getAttributeValue(ForgeMod.SWIM_SPEED.getHolder().get());
        entity.moveRelative(acceleration, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 motion = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.onClimbable()) {
            motion = new Vec3(motion.x, 0.2, motion.z);
        }
        entity.setDeltaMovement(motion.multiply(drag, 0.8F, drag));
        motion = entity.getFluidFallingAdjustedMovement(gravity, falling, entity.getDeltaMovement());
        entity.setDeltaMovement(motion);
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

            // Thick red murk: a few blocks of visibility, more than lava but far less than water.
            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(0.0F);
                RenderSystem.setShaderFogEnd(10.0F);
            }
        });
    }
}
