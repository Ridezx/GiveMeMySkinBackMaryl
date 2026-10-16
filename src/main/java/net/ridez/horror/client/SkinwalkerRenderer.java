package net.ridez.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SkinwalkerEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

// Draws the Skinwalker's animated model (see SkinwalkerModel), with its eyes glowing in the dark
// (textures/entity/skinwalker_glowmask.png), then the eyes again as added light, with a faint glow on the socket
// walls around them (skinwalker_eye_glow.png), the way Enderman and spider eyes are drawn.
public class SkinwalkerRenderer extends GeoEntityRenderer<SkinwalkerEntity> {
    // The rig stands 5.75 blocks tall in Blockbench; the Skinwalker is 4.5.
    private static final float SCALE = 4.5F / 5.75F;
    private static final ResourceLocation EYE_GLOW = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/entity/skinwalker_eye_glow.png");

    public SkinwalkerRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new SkinwalkerModel());
        this.withScale(SCALE);
        this.shadowRadius = 0.9F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.addRenderLayer(new EyeGlowLayer(this));
    }

    // Its death animations already put it on the ground: no tipping over sideways like other mobs.
    @Override
    protected float getDeathMaxRotation(SkinwalkerEntity pAnimatable) {
        return 0.0F;
    }

    // Its deaths last several seconds, so no red damage tint while it dies (it still flashes when hurt).
    @Override
    public int getPackedOverlay(SkinwalkerEntity pAnimatable, float pU, float pPartialTick) {
        return pAnimatable.isDeadOrDying() ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(pAnimatable, pU, pPartialTick);
    }

    // Adds the eye glow on top of everything as light (additive, full brightness), so the eyes burn brighter
    // than their texture and light up the sockets around them.
    private static class EyeGlowLayer extends GeoRenderLayer<SkinwalkerEntity> {
        EyeGlowLayer(GeoRenderer<SkinwalkerEntity> pRenderer) {
            super(pRenderer);
        }

        @Override
        public void render(PoseStack pPoseStack, SkinwalkerEntity pAnimatable, BakedGeoModel pModel, @Nullable RenderType pRenderType,
                           MultiBufferSource pBufferSource, @Nullable VertexConsumer pBuffer, float pPartialTick, int pPackedLight, int pPackedOverlay) {
            if (pAnimatable.isInvisible()) {
                return;
            }
            RenderType eyes = RenderType.eyes(EYE_GLOW);
            this.getRenderer().reRender(pModel, pPoseStack, pBufferSource, pAnimatable, eyes, pBufferSource.getBuffer(eyes),
                    pPartialTick, 15728640, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
