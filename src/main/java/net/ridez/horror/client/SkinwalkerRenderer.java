package net.ridez.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SkinwalkerEntity;

// Placeholder: the enderman model scaled up to the Skinwalker's 4.5-block height, drawn with the
// Skinwalker's own texture slot (missing until textures/entity/skinwalker.png exists). Swap in the
// real hunched model once it's designed.
public class SkinwalkerRenderer extends MobRenderer<SkinwalkerEntity, EndermanModel<SkinwalkerEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/entity/skinwalker.png");
    // Enderman model is 2.9 blocks tall; the Skinwalker is 4.5.
    private static final float SCALE = 4.5F / 2.9F;

    public SkinwalkerRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new EndermanModel<>(pContext.bakeLayer(ModelLayers.ENDERMAN)), 0.9F);
    }

    @Override
    protected void scale(SkinwalkerEntity pEntity, PoseStack pPoseStack, float pPartialTick) {
        pPoseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(SkinwalkerEntity pEntity) {
        return TEXTURE;
    }
}
