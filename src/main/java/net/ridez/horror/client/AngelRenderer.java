package net.ridez.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.AngelEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Draws the Angel (geo, texture and animations from art/tools/angel_model.py) in its own light, as bright by night as
// by day, its ring tipped up or down to look at the player it faces (not while it stands open on the ground).
// Cutout: the feather tips.
public class AngelRenderer extends GeoEntityRenderer<AngelEntity> {
    private static final float RING_MIDDLE = 18.0F / 16.0F;                // (the model's ring centre, above its feet)

    public AngelRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "angel")));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AngelEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(AngelEntity pAnimatable, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTick, float pNativeScale) {
        super.applyRotations(pAnimatable, pPoseStack, pAgeInTicks, pRotationYaw, pPartialTick, pNativeScale);
        if (!pAnimatable.isStanding()) {
            float pitch = Mth.lerp(pPartialTick, pAnimatable.xRotO, pAnimatable.getXRot());
            pPoseStack.translate(0.0F, RING_MIDDLE, 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            pPoseStack.translate(0.0F, -RING_MIDDLE, 0.0F);
        }
    }
}
