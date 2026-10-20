package net.ridez.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SandmanEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

// Draws the Sandman (geo, texture and animations from art/tools/sandman_model.py): solid, with her fire, eyes and the
// cold rim of her outline glowing in the dark (sandman_glow.png, added on top as light), turning her head to look, and
// fading away as she dies. Her rifle is part of the model (the "gun" bone); the Toy Gun she holds is only for her drop.
// (Not GeckoLib's AutoGlowingGeoLayer: it erases the texture under every pixel of its glowmask.)
public class SandmanRenderer extends GeoEntityRenderer<SandmanEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/entity/sandman_glow.png");

    public SandmanRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "sandman"), true));
        this.shadowRadius = 0.4F;
        this.addRenderLayer(new GlowLayer(this));
    }

    @Override
    public RenderType getRenderType(SandmanEntity pAnimatable, ResourceLocation pTexture, @Nullable MultiBufferSource pBufferSource,
                                    float pPartialTick) {
        return RenderType.entityTranslucent(pTexture);
    }

    @Override
    public Color getRenderColor(SandmanEntity pAnimatable, float pPartialTick, int pPackedLight) {
        return Color.ofRGBA(1.0F, 1.0F, 1.0F, pAnimatable.getFade(pPartialTick));
    }

    // Her death animation puts her on the ground: no tipping over sideways like other mobs.
    @Override
    protected float getDeathMaxRotation(SandmanEntity pAnimatable) {
        return 0.0F;
    }

    // The glow as added light at full brightness (like Enderman eyes), fading with her.
    private static class GlowLayer extends GeoRenderLayer<SandmanEntity> {
        GlowLayer(GeoRenderer<SandmanEntity> pRenderer) {
            super(pRenderer);
        }

        @Override
        public void render(PoseStack pPoseStack, SandmanEntity pAnimatable, BakedGeoModel pModel, @Nullable RenderType pRenderType,
                           MultiBufferSource pBufferSource, @Nullable VertexConsumer pBuffer, float pPartialTick, int pPackedLight, int pPackedOverlay) {
            if (pAnimatable.isInvisible()) {
                return;
            }
            int c = Mth.clamp(Math.round(pAnimatable.getFade(pPartialTick) * 255.0F), 0, 255);
            RenderType eyes = RenderType.eyes(GLOW);
            this.getRenderer().reRender(pModel, pPoseStack, pBufferSource, pAnimatable, eyes, pBufferSource.getBuffer(eyes),
                    pPartialTick, 15728640, OverlayTexture.NO_OVERLAY, 0xFF000000 | c << 16 | c << 8 | c);
        }
    }
}
