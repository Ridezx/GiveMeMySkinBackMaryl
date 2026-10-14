package net.ridez.horror.client;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.TraderEntity;

// Placeholder: draws a trader with the vanilla villager model and its own texture
// (textures/entity/<name>.png, villager UV layout). Swap in a custom model once one is designed.
public class TraderRenderer extends MobRenderer<TraderEntity, VillagerModel<TraderEntity>> {
    private final ResourceLocation texture;

    public TraderRenderer(EntityRendererProvider.Context pContext, String name) {
        super(pContext, new VillagerModel<>(pContext.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.texture = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(TraderEntity pEntity) {
        return this.texture;
    }
}
