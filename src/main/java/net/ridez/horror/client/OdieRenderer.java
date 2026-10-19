package net.ridez.horror.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.OdieEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

// Draws Odie from geo/entity/odie.geo.json, animations/entity/odie.animation.json and textures/entity/odie.png
// (all made by art/tools/odie_model.py). As he crumbles he darkens into a shadow, then thins out into the air.
public class OdieRenderer extends GeoEntityRenderer<OdieEntity> {
    public OdieRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "odie")));
        this.shadowRadius = 0.4F;
    }

    private static final float SHADOW_PART = 0.4F;           // the first 40% of crumbling darkens him, the rest fades him out

    @Override
    public RenderType getRenderType(OdieEntity pAnimatable, ResourceLocation pTexture, @Nullable MultiBufferSource pBufferSource,
                                    float pPartialTick) {
        return RenderType.entityTranslucent(pTexture);
    }

    @Override
    public Color getRenderColor(OdieEntity pAnimatable, float pPartialTick, int pPackedLight) {
        float crumble = pAnimatable.getCrumble(pPartialTick);
        float dark = Mth.clamp(crumble / SHADOW_PART, 0.0F, 1.0F);
        float thin = Mth.clamp((crumble - SHADOW_PART) / (1.0F - SHADOW_PART), 0.0F, 1.0F);
        float shade = 1.0F - 0.94F * dark * dark * (3.0F - 2.0F * dark);
        return Color.ofRGBA(shade, shade, shade, 1.0F - thin * thin * (3.0F - 2.0F * thin));
    }
}
