package net.ridez.horror.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.SkinwalkerEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

// The Skinwalker's model, texture and animations (geo/entity, textures/entity and animations/entity
// skinwalker.*), all written by art/tools/skinwalker_anim.py and skinwalker_texture.py.
public class SkinwalkerModel extends DefaultedEntityGeoModel<SkinwalkerEntity> {
    // How much of the way the head turns toward where the Skinwalker is looking, on top of its animation.
    private static final float HEAD_TRACKING = 0.7F;

    public SkinwalkerModel() {
        // false: GeckoLib's own head turning would replace the head's animation instead of adding to it
        super(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "skinwalker"), false);
    }

    @Override
    public void setCustomAnimations(SkinwalkerEntity animatable, long instanceId, AnimationState<SkinwalkerEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        if (animatable.isCirclingToLieDown()) {
            // the game walks it round the circle, so the animation's own turn on the spot is left out
            getBone("root").ifPresent(root -> root.setRotY(0));
        }
        if (animatable.isDeadOrDying() || !animatable.headTracks()) {
            return;
        }
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        getBone("head").ifPresent(head -> {
            head.setRotX(head.getRotX() + data.headPitch() * Mth.DEG_TO_RAD * HEAD_TRACKING);
            head.setRotY(head.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD * HEAD_TRACKING);
        });
    }
}
