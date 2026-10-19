package net.ridez.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.custom.ModCrossbowItem;
import net.ridez.horror.item.custom.StrawItem;
import net.ridez.horror.item.custom.ToyGunItem;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Animations for the weapons.
// - Straw (a crossbow underneath, so drawn through mixin/ItemInHandRendererMixin and posed through
//   mixin/PlayerRendererMixin, because the game handles any crossbow with its own fixed code): loading brings it up
//   in front of the chest, upright, and the free hand (if it's empty) comes across and drops a pinch of salt into
//   its top end; only once it's really loaded (vanilla loads it when the button is let go) does it go up to the
//   mouth: from the mouth (bottom middle of the screen) out to the crosshair, so its far end is the sight. A shot
//   puffs it forward, then it's lowered again. Third person the same: up to the chest with the free hand crossing
//   over, then to the mouth.
// - Toy Gun (through Forge's item hooks, TOY_GUN): at the hip when empty, raised and centred once loaded; the
//   3 second reload rolls it over to show the loading port, pushes the Dust Ball in (two shoves) and works the
//   lever; a shot kicks it up and back; hidden while looking down the scope. Third person: shouldered with both
//   hands, jolting through the reload and kicking on a shot.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WeaponAnimations {
    // Straw: where its two ends go when aimed (camera space: x right, y up, -z forward). The far end sits just
    // under the crosshair, the near end at the mouth.
    private static final Vector3f STRAW_SIGHT = new Vector3f(0.0F, -0.03F, -1.05F);
    private static final Vector3f STRAW_MOUTH = new Vector3f(0.0F, -0.21F, -0.32F);
    // Loading: held upright in front of the chest, a little to its own side, the bendy end on top for the salt.
    private static final Vector3f STRAW_LOAD_BOTTOM = new Vector3f(0.20F, -0.55F, -0.80F);
    private static final Vector3f STRAW_LOAD_TOP = new Vector3f(0.12F, -0.22F, -0.75F);
    // The straw's ends in its texture (straw.png, 32x32; item model space 0-1): far end bottom left, bendy end
    // (the one in the mouth) top right.
    private static final Vector3f STRAW_FAR_END = new Vector3f(4.5F / 32, 1 - 30.5F / 32, 0.5F);
    private static final Vector3f STRAW_NEAR_END = new Vector3f(27.5F / 32, 1 - 1.5F / 32, 0.5F);
    // Ticks to lower the straw after a shot, to raise it from the chest to the mouth once it's loaded, and for the
    // gun's recoil to settle.
    private static final float STRAW_LOWER_TICKS = 7.0F;
    private static final float STRAW_RAISE_TICKS = 4.0F;
    // The free hand loading the straw (worked out with a preview of vanilla's first-person arm): it rises from below
    // the screen and turns in about the shoulder (FREE_SHOULDER) by these angles and moves by FREE_REACH, ending just
    // over the straw's top end, coming in from the side without covering the straw.
    private static final Vector3f FREE_SHOULDER = new Vector3f(0.7F, -1.0F, -0.3F), FREE_REACH = new Vector3f(-0.134F, 0.161F, -0.075F);
    private static final float FREE_TURN_Y = 20.0F, FREE_TURN_X = 25.0F, FREE_TURN_Z = 55.0F;
    private static final float RECOIL_TICKS = 14.0F;

    private WeaponAnimations() {
    }

    public static void renderFirstPerson(ItemInHandRenderer pRenderer, AbstractClientPlayer pPlayer, float pPartialTick,
                                         InteractionHand pHand, float pSwing, ItemStack pStack, float pEquip,
                                         PoseStack pPose, MultiBufferSource pBuffer, int pLight) {
        HumanoidArm arm = pHand == InteractionHand.MAIN_HAND ? pPlayer.getMainArm() : pPlayer.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        int side = right ? 1 : -1;
        // (still holding the button after the load time runs out still counts: the use timer stops at 0, but the
        // player is still using it until they let go)
        boolean loading = pPlayer.isUsingItem() && pPlayer.getUsedItemHand() == pHand;
        float load = 0.0F;
        if (loading) {
            float used = pPlayer.getTicksUsingItem() + pPartialTick;
            load = Mth.clamp(used / CrossbowItem.getChargeDuration(pStack, pPlayer), 0.0F, 1.0F);
        }
        boolean charged = CrossbowItem.isCharged(pStack);
        float sinceShot = pPlayer.level().getGameTime() - ModCrossbowItem.lastShotTime(pPlayer.getUUID()) + pPartialTick;
        float sinceCharged = pPlayer.level().getGameTime() - chargedTime(pPlayer.getUUID()) + pPartialTick;

        pPose.pushPose();
        if (pStack.getItem() instanceof StrawItem) {
            // the free hand helps load it if it's the off hand and empty (vanilla draws an empty main hand itself)
            boolean freeHand = loading && pHand == InteractionHand.MAIN_HAND && pPlayer.getOffhandItem().isEmpty() && !pPlayer.isInvisible();
            if (freeHand) {
                freeHand(pPose, pPlayer, -side, load, pBuffer, pLight);
            }
            straw(pPose, pPlayer, pStack, side, pEquip, pSwing, loading, load, charged, sinceShot, sinceCharged);
        } else {
            held(pPose, side, pEquip, pSwing);
        }
        pRenderer.renderItem(pPlayer, pStack, right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !right, pPose, pBuffer, pLight);
        pPose.popPose();
    }

    // ---------------------------------------------------------------- the straw
    private static void straw(PoseStack pPose, AbstractClientPlayer pPlayer, ItemStack pStack, int pSide, float pEquip, float pSwing,
                              boolean pLoading, float pLoad, boolean pCharged, float pSinceShot, float pSinceCharged) {
        Placement hand = new Placement(handPosition(pSide, pEquip), new Quaternionf(), 1.0F);
        Placement now;
        if (pCharged) {
            // loaded: up from the chest to the mouth (straight away if it was loaded long ago)
            now = strawAtChest(pPlayer, pStack, pSide, pEquip).towards(strawAtMouth(pPlayer, pStack, pSide, pEquip),
                    ease(pSinceCharged / STRAW_RAISE_TICKS));
        } else if (pLoading) {
            // up to the chest, dipping a little as the salt drops in, then waiting there until it's let go of
            now = hand.towards(strawAtChest(pPlayer, pStack, pSide, pEquip), ease(span(pLoad, 0.0F, 0.3F)));
            now.translation.y -= 0.025F * bump(pLoad, 0.68F, 0.12F);
        } else if (pSinceShot < STRAW_LOWER_TICKS) {
            now = hand.towards(strawAtMouth(pPlayer, pStack, pSide, pEquip), 1.0F - ease(pSinceShot / STRAW_LOWER_TICKS));
        } else {
            held(pPose, pSide, pEquip, pSwing);
            return;
        }
        if (pSinceShot < 4.0F) {                            // the puff: a quick push forward and back
            float puff = Mth.sin(pSinceShot / 4.0F * Mth.PI);
            now.translation.z -= 0.06F * puff;
        }
        now.apply(pPose);
    }

    // Aimed: its far end lined up under the crosshair and its near end at the mouth, lying flat (seen from just
    // above, like looking down a straw).
    private static Placement strawAtMouth(AbstractClientPlayer pPlayer, ItemStack pStack, int pSide, float pEquip) {
        return strawBetween(pPlayer, pStack, pSide, pEquip, STRAW_SIGHT, STRAW_MOUTH, new Vector3f(0, 1, 0));
    }

    // Loading: upright in front of the chest, its face to the camera.
    private static Placement strawAtChest(AbstractClientPlayer pPlayer, ItemStack pStack, int pSide, float pEquip) {
        return strawBetween(pPlayer, pStack, pSide, pEquip, STRAW_LOAD_BOTTOM, STRAW_LOAD_TOP, new Vector3f(0, 0, 1));
    }

    // Where the straw goes so its far end is at pFar and its near (bendy) end at pNear (camera space, for the right
    // hand; mirrored for the left), its flat face turned toward pFace. Worked out from the item's own first-person
    // display transform, so it stays right if the model's display settings change.
    private static Placement strawBetween(AbstractClientPlayer pPlayer, ItemStack pStack, int pSide, float pEquip, Vector3f pFar,
                                          Vector3f pNear, Vector3f pFace) {
        ItemTransform display = Minecraft.getInstance().getItemRenderer().getModel(pStack, pPlayer.level(), pPlayer, 0)
                .getTransforms().getTransform(pSide > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        boolean left = pSide < 0;
        Matrix4f model = new Matrix4f()
                .translate(left ? -display.translation.x : display.translation.x, display.translation.y, display.translation.z)
                .rotateXYZ(display.rotation.x * Mth.DEG_TO_RAD, (left ? -display.rotation.y : display.rotation.y) * Mth.DEG_TO_RAD,
                        (left ? -display.rotation.z : display.rotation.z) * Mth.DEG_TO_RAD)
                .scale(display.scale)
                .translate(-0.5F, -0.5F, -0.5F);
        Vector3f far = model.transformPosition(new Vector3f(STRAW_FAR_END));
        Vector3f near = model.transformPosition(new Vector3f(STRAW_NEAR_END));
        Vector3f face = model.transformDirection(new Vector3f(0, 0, 1));
        Vector3f farTo = new Vector3f(pFar).mul(pSide, 1, 1);
        Vector3f nearTo = new Vector3f(pNear).mul(pSide, 1, 1);
        Matrix3f from = frame(new Vector3f(near).sub(far), face);
        Matrix3f to = frame(new Vector3f(nearTo).sub(farTo), pFace);
        Matrix3f turn = to.mul(from.transpose());
        float scale = nearTo.distance(farTo) / near.distance(far);
        Vector3f translation = new Vector3f(farTo).sub(turn.transform(new Vector3f(far)).mul(scale));
        translation.y -= pEquip * 0.6F;
        return new Placement(translation, new Quaternionf().setFromNormalized(turn), scale);
    }

    // The free hand dropping the salt in (pFree: 1 = the right hand, -1 = the left): up from below the screen and
    // across to the straw's top end, a little dip as it lets the salt go, then back down out of sight. Drawn as
    // vanilla draws an empty hand (its renderPlayerArm), moved by the reach first.
    private static void freeHand(PoseStack pPose, AbstractClientPlayer pPlayer, int pFree, float pLoad, MultiBufferSource pBuffer, int pLight) {
        float reach = ease(span(pLoad, 0.1F, 0.45F)) * (1.0F - ease(span(pLoad, 0.8F, 1.0F)));
        if (reach <= 0.0F) {
            return;
        }
        float f = pFree;
        float drop = bump(pLoad, 0.65F, 0.12F);
        pPose.pushPose();
        pPose.translate(0.0F, -0.6F * (1.0F - reach) - 0.04F * drop, 0.0F);
        pPose.translate(f * FREE_SHOULDER.x + f * FREE_REACH.x * reach, FREE_SHOULDER.y + FREE_REACH.y * reach, FREE_SHOULDER.z + FREE_REACH.z * reach);
        pPose.mulPose(Axis.YP.rotationDegrees(f * FREE_TURN_Y * reach));
        pPose.mulPose(Axis.XP.rotationDegrees(FREE_TURN_X * reach));
        pPose.mulPose(Axis.ZP.rotationDegrees(f * FREE_TURN_Z * reach));
        pPose.translate(-f * FREE_SHOULDER.x, -FREE_SHOULDER.y, -FREE_SHOULDER.z);
        // vanilla's empty hand
        pPose.translate(f * 0.64F, -0.6F, -0.72F);
        pPose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        pPose.translate(f * -1.0F, 3.6F, 3.5F);
        pPose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pPose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pPose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pPose.translate(f * 5.6F, 0.0F, 0.0F);
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(pPlayer);
        if (pFree > 0) {
            renderer.renderRightHand(pPose, pBuffer, pLight, pPlayer);
        } else {
            renderer.renderLeftHand(pPose, pBuffer, pLight, pPlayer);
        }
        pPose.popPose();
    }

    // An orthonormal frame (as matrix columns) with its first axis along pAlong and its second as close to pUp as
    // possible.
    private static Matrix3f frame(Vector3f pAlong, Vector3f pUp) {
        Vector3f a = new Vector3f(pAlong).normalize();
        Vector3f b = new Vector3f(pUp).sub(new Vector3f(a).mul(a.dot(pUp))).normalize();
        Vector3f c = new Vector3f(a).cross(b);
        return new Matrix3f(a, b, c);
    }

    // ---------------------------------------------------------------- the toy gun
    // Its animations come in through Forge's item hooks (it isn't a crossbow, so nothing in the way).
    public static final IClientItemExtensions TOY_GUN = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity pEntity, InteractionHand pHand, ItemStack pStack) {
            return toyGunArmPose(pEntity, pStack);
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pPose, LocalPlayer pPlayer, HumanoidArm pArm, ItemStack pStack, float pPartialTick,
                                               float pEquip, float pSwing) {
            int side = pArm == HumanoidArm.RIGHT ? 1 : -1;
            if (ToyGunItem.isScoping(pPlayer)) {
                pPose.scale(0.0F, 0.0F, 0.0F);              // looking down the scope: the overlay shows instead
                return true;
            }
            float sinceShot = pPlayer.level().getGameTime() - ModCrossbowItem.lastShotTime(pPlayer.getUUID()) + pPartialTick;
            toyGun(pPose, side, pEquip, pSwing, ToyGunItem.reloadProgress(pStack, pPlayer.level(), pPartialTick), ToyGunItem.isLoaded(pStack),
                    sinceShot);
            return true;
        }
    };

    // First person (the 3D model, which points straight ahead from the hand, see art/tools/toy_gun_model.py): at the
    // hip when empty; raised and closer to the middle once loaded, both toed in a little toward the crosshair; the
    // reload (rolled over, the ball shoved in, the lever worked) from the hip; a shot kicks it up and back, then it
    // drops to the hip.
    private static final Vector3f GUN_HIP = new Vector3f(0.40F, -0.42F, -0.55F), GUN_RAISED = new Vector3f(0.24F, -0.30F, -0.45F);
    private static final float GUN_HIP_TOE_IN = 4.0F, GUN_RAISED_TOE_IN = 2.0F;

    private static void toyGun(PoseStack pPose, int pSide, float pEquip, float pSwing, float pReload, boolean pLoaded, float pSinceShot) {
        boolean reloading = pReload >= 0.0F;
        boolean recoiling = pSinceShot < RECOIL_TICKS;
        float raised = pLoaded && !reloading ? 1.0F : recoiling ? 1.0F - ease(pSinceShot / RECOIL_TICKS) : 0.0F;
        Vector3f at = new Vector3f(GUN_HIP).lerp(GUN_RAISED, raised);
        pPose.translate(pSide * at.x, at.y - pEquip * 0.6F, at.z);
        pPose.mulPose(Axis.YP.rotationDegrees(pSide * Mth.lerp(raised, GUN_HIP_TOE_IN, GUN_RAISED_TOE_IN)));
        if (recoiling) {                                    // kicks up and back, then settles
            float kick = (1.0F - pSinceShot / RECOIL_TICKS);
            kick *= kick;
            pPose.translate(0.0F, 0.12F * kick, 0.45F * kick);
            pPose.mulPose(Axis.XP.rotationDegrees(50.0F * kick));
        }
        if (reloading) {
            reload(pPose, pSide, pReload);
        }
    }

    // The reload, over the whole load time (pLoad 0-1).
    private static void reload(PoseStack pPose, int pSide, float pLoad) {
        // roll the gun over and tip it up to show the loading port, and back at the end
        float tilt = ease(span(pLoad, 0.0F, 0.15F)) - ease(span(pLoad, 0.85F, 1.0F));
        pPose.translate(pSide * -0.18F * tilt, -0.1F * tilt, 0.04F * tilt);
        pPose.mulPose(Axis.ZP.rotationDegrees(pSide * 38.0F * tilt));
        pPose.mulPose(Axis.XP.rotationDegrees(14.0F * tilt));
        // push the Dust Ball in: two shoves
        float shove = bump(pLoad, 0.3F, 0.08F) + bump(pLoad, 0.5F, 0.08F);
        pPose.translate(0.0F, 0.035F * shove, -0.02F * shove);
        pPose.mulPose(Axis.XP.rotationDegrees(-7.0F * shove));
        // work the lever: the gun jerks down and snaps back up
        float lever = bump(pLoad, 0.72F, 0.07F);
        pPose.translate(0.0F, -0.06F * lever, 0.0F);
        pPose.mulPose(Axis.XP.rotationDegrees(-16.0F * lever));
    }

    // ---------------------------------------------------------------- third person
    private static HumanoidModel.ArmPose strawPose, gunPose;

    // The arm pose for a player holding one of these weapons in pHand.
    public static HumanoidModel.ArmPose armPose(Player pPlayer, InteractionHand pHand, ItemStack pStack) {
        createPoses();
        boolean loading = pPlayer.isUsingItem() && pPlayer.getUsedItemHand() == pHand;
        boolean recent = sinceShot(pPlayer) < STRAW_LOWER_TICKS;
        if (!loading && !CrossbowItem.isCharged(pStack) && !recent) {
            return HumanoidModel.ArmPose.ITEM;
        }
        return strawPose;
    }

    private static void createPoses() {
        if (strawPose == null) {
            strawPose = HumanoidModel.ArmPose.create("HORRORMOD_STRAW", false, WeaponAnimations::poseStraw);
            gunPose = HumanoidModel.ArmPose.create("HORRORMOD_TOY_GUN", true, WeaponAnimations::poseToyGun);
        }
    }

    // Third person, the Toy Gun: shouldered with both hands while it's loaded, scoped, reloading or just fired;
    // otherwise carried in one hand.
    private static HumanoidModel.ArmPose toyGunArmPose(LivingEntity pEntity, ItemStack pStack) {
        createPoses();
        boolean busy = ToyGunItem.isLoaded(pStack) || ToyGunItem.isReloading(pStack) || ToyGunItem.isScoping(pEntity)
                || sinceShot(pEntity) < RECOIL_TICKS;
        return busy ? gunPose : HumanoidModel.ArmPose.ITEM;
    }

    // The straw raised to the chest to load (the free hand crossing over to drop the salt in), up to the mouth once
    // it's loaded, blending from the normal held-item pose while it's raised and lowered.
    private static void poseStraw(HumanoidModel<?> pModel, LivingEntity pEntity, HumanoidArm pArm) {
        ItemStack stack = pEntity.getItemInHand(pArm == pEntity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        boolean loading = pEntity.isUsingItem() && pEntity.getUseItem() == stack;
        float load = loading ? loadProgress(pEntity, stack) : 0.0F;
        float raise, toMouth;                               // held -> chest, chest -> mouth
        if (loading) {
            raise = ease(span(load, 0.0F, 0.3F));
            toMouth = 0.0F;
        } else if (CrossbowItem.isCharged(stack)) {
            raise = 1.0F;
            toMouth = ease((pEntity.level().getGameTime() - chargedTime(pEntity.getUUID()) + partialTick()) / STRAW_RAISE_TICKS);
        } else {
            raise = 1.0F - ease(sinceShot(pEntity) / STRAW_LOWER_TICKS);
            toMouth = 1.0F;
        }
        ModelPart arm = pArm == HumanoidArm.RIGHT ? pModel.rightArm : pModel.leftArm;
        int side = pArm == HumanoidArm.RIGHT ? 1 : -1;
        float heldX = arm.xRot * 0.5F - Mth.PI / 10.0F;
        float upX = Mth.lerp(toMouth, pModel.head.xRot - 1.2F, pModel.head.xRot - 1.75F);          // forearm up to the chest, the face
        float upY = Mth.lerp(toMouth, pModel.head.yRot - side * 0.25F, pModel.head.yRot - side * 0.55F);
        arm.xRot = Mth.lerp(raise, heldX, upX);
        arm.yRot = Mth.lerp(raise, 0.0F, upY);
        arm.zRot = 0.0F;
        // (while loading only the arm holding it gets posed, so the free one can be moved here)
        boolean otherEmpty = pEntity.getItemInHand(pArm == pEntity.getMainArm() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).isEmpty();
        if (loading && otherEmpty) {
            float reach = ease(span(load, 0.1F, 0.45F)) * (1.0F - ease(span(load, 0.8F, 1.0F)));
            float drop = bump(load, 0.65F, 0.12F);
            ModelPart free = pArm == HumanoidArm.RIGHT ? pModel.leftArm : pModel.rightArm;
            free.xRot = Mth.lerp(reach, free.xRot, pModel.head.xRot - 1.45F + 0.15F * drop);
            free.yRot = Mth.lerp(reach, free.yRot, pModel.head.yRot + side * 0.6F);
            free.zRot = Mth.lerp(reach, free.zRot, 0.0F);
        }
    }

    // The gun shouldered with both hands (like a loaded crossbow), the reload's shoves and lever, and the recoil.
    // (Reload progress comes from the gun itself, which every client has, so other players see it in step.)
    private static void poseToyGun(HumanoidModel<?> pModel, LivingEntity pEntity, HumanoidArm pArm) {
        boolean right = pArm == HumanoidArm.RIGHT;
        ItemStack stack = pEntity.getItemInHand(pArm == pEntity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        ModelPart holding = right ? pModel.rightArm : pModel.leftArm;
        ModelPart support = right ? pModel.leftArm : pModel.rightArm;
        float load = ToyGunItem.reloadProgress(stack, pEntity.level(), partialTick());
        if (load >= 0.0F) {
            AnimationUtils.animateCrossbowCharge(pModel.rightArm, pModel.leftArm, pEntity, right);
            float shove = bump(load, 0.3F, 0.08F) + bump(load, 0.5F, 0.08F);
            float lever = bump(load, 0.72F, 0.07F);
            support.xRot -= 0.35F * shove;                                  // the other hand pushing the ball in
            holding.xRot += 0.3F * lever;                                   // working the lever
        } else {
            AnimationUtils.animateCrossbowHold(pModel.rightArm, pModel.leftArm, pModel.head, right);
            float t = sinceShot(pEntity) / RECOIL_TICKS;
            if (t < 1.0F) {
                float kick = (1.0F - t) * (1.0F - t);
                holding.xRot -= 1.1F * kick;
                support.xRot -= 1.1F * kick;
            }
        }
    }

    private static float loadProgress(LivingEntity pEntity, ItemStack pStack) {
        float used = pEntity.getTicksUsingItem() + partialTick();
        return Mth.clamp(used / CrossbowItem.getChargeDuration(pStack, pEntity), 0.0F, 1.0F);
    }

    private static float sinceShot(LivingEntity pEntity) {
        return pEntity.level().getGameTime() - ModCrossbowItem.lastShotTime(pEntity.getUUID()) + partialTick();
    }

    private static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    // Other players' shots: their weapon goes from loaded to empty. (The local player's are recorded as they fire.)
    // And when each player's straw got loaded (anyone's, the local player's too), for raising it to the mouth.
    private static final Map<UUID, Boolean> WAS_LOADED = new HashMap<>(), WAS_STRAW_LOADED = new HashMap<>();
    private static final Map<UUID, Long> STRAW_LOADED_AT = new HashMap<>();

    private static long chargedTime(UUID pPlayer) {
        return STRAW_LOADED_AT.getOrDefault(pPlayer, Long.MIN_VALUE / 2);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            WAS_LOADED.clear();
            WAS_STRAW_LOADED.clear();
            STRAW_LOADED_AT.clear();
            return;
        }
        for (Player player : minecraft.level.players()) {
            boolean loaded = false, strawLoaded = false;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                loaded |= stack.getItem() instanceof ModCrossbowItem && CrossbowItem.isCharged(stack)
                        || stack.getItem() instanceof ToyGunItem && ToyGunItem.isLoaded(stack);
                strawLoaded |= stack.getItem() instanceof StrawItem && CrossbowItem.isCharged(stack);
            }
            if (strawLoaded && !Boolean.TRUE.equals(WAS_STRAW_LOADED.put(player.getUUID(), true))) {
                STRAW_LOADED_AT.put(player.getUUID(), minecraft.level.getGameTime());
            } else if (!strawLoaded) {
                WAS_STRAW_LOADED.put(player.getUUID(), false);
            }
            Boolean before = WAS_LOADED.put(player.getUUID(), loaded);
            if (Boolean.TRUE.equals(before) && !loaded && player != minecraft.player) {
                ModCrossbowItem.recordShot(player.getUUID(), minecraft.level.getGameTime());
            }
        }
    }

    // ---------------------------------------------------------------- shared
    // Held normally in the hand, swinging like any item (vanilla's first-person crossbow without a shot loaded).
    private static void held(PoseStack pPose, int pSide, float pEquip, float pSwing) {
        float x = -0.4F * Mth.sin(Mth.sqrt(pSwing) * Mth.PI);
        float y = 0.2F * Mth.sin(Mth.sqrt(pSwing) * Mth.TWO_PI);
        float z = -0.2F * Mth.sin(pSwing * Mth.PI);
        pPose.translate(pSide * x, y, z);
        Vector3f hand = handPosition(pSide, pEquip);
        pPose.translate(hand.x, hand.y, hand.z);
        float f = Mth.sin(pSwing * pSwing * Mth.PI);
        pPose.mulPose(Axis.YP.rotationDegrees(pSide * (45.0F + f * -20.0F)));
        float f1 = Mth.sin(Mth.sqrt(pSwing) * Mth.PI);
        pPose.mulPose(Axis.ZP.rotationDegrees(pSide * f1 * -20.0F));
        pPose.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
        pPose.mulPose(Axis.YP.rotationDegrees(pSide * -45.0F));
    }

    // Where vanilla puts the hand holding an item (its applyItemArmTransform).
    private static Vector3f handPosition(int pSide, float pEquip) {
        return new Vector3f(pSide * 0.56F, -0.52F + pEquip * -0.6F, -0.72F);
    }

    private static float ease(float pT) {
        pT = Mth.clamp(pT, 0.0F, 1.0F);
        return pT * pT * (3.0F - 2.0F * pT);
    }

    // 0 before pFrom, 1 after pTo, rising linearly between.
    private static float span(float pT, float pFrom, float pTo) {
        return Mth.clamp((pT - pFrom) / (pTo - pFrom), 0.0F, 1.0F);
    }

    // A smooth 0-1-0 bump centred on pCentre, pHalfWidth wide on each side.
    private static float bump(float pT, float pCentre, float pHalfWidth) {
        float d = Math.abs(pT - pCentre) / pHalfWidth;
        return d >= 1.0F ? 0.0F : 0.5F + 0.5F * Mth.cos(d * Mth.PI);
    }

    // A move, turn and uniform scale, blendable.
    private record Placement(Vector3f translation, Quaternionf rotation, float scale) {
        Placement towards(Placement pOther, float pT) {
            return new Placement(new Vector3f(this.translation).lerp(pOther.translation, pT),
                    new Quaternionf(this.rotation).slerp(pOther.rotation, pT), Mth.lerp(pT, this.scale, pOther.scale));
        }

        void apply(PoseStack pPose) {
            pPose.translate(this.translation.x, this.translation.y, this.translation.z);
            pPose.mulPose(this.rotation);
            pPose.scale(this.scale, this.scale, this.scale);
        }
    }
}
