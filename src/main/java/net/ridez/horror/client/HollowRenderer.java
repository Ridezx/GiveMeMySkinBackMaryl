package net.ridez.horror.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.HollowEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// Draws the Hollow (geo, texture and animations from art/tools/hollow_model.py). Cutout: the rips and holes in its
// clothes are see-through. Its head isn't turned to look at anything: it pays no attention, the head moves only as
// the animations move it.
public class HollowRenderer extends GeoEntityRenderer<HollowEntity> {
    public HollowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "hollow")));
        this.shadowRadius = 0.45F;
    }

    // Its death animation breaks it apart on the ground: no tipping over sideways like other mobs.
    @Override
    protected float getDeathMaxRotation(HollowEntity pAnimatable) {
        return 0.0F;
    }

    // No red tint while it falls apart (it still flashes when hurt).
    @Override
    public int getPackedOverlay(HollowEntity pAnimatable, float pU, float pPartialTick) {
        return pAnimatable.isDeadOrDying() ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(pAnimatable, pU, pPartialTick);
    }
}
